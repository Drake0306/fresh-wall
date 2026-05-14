package com.example.freshwall.data.feedback

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.freshwall.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

private const val TAG = "FreshWallFeedback"

/**
 * Toggle for the Firebase Storage screenshot upload step.
 *
 * Set to `false` while Storage init / bucket creation is failing on the
 * Firebase side — text-only feedback still flows to Firestore so we can
 * confirm auth + Firestore are wired up. Flip back to `true` once
 * `Storage → Get started` completes cleanly in the Firebase console.
 */
private const val UPLOAD_SCREENSHOTS = false

enum class FeedbackKind(val value: String) {
    BUG("bug"),
    FEEDBACK("feedback"),
}

/**
 * Thrown when one of the three submission steps fails. The [step] field
 * identifies which stage broke (sign-in, screenshot, or firestore) so the
 * UI can surface that to the user without a stack trace.
 */
class FeedbackSubmitException(
    val step: String,
    cause: Throwable,
) : Exception("$step — ${cause.message ?: cause.javaClass.simpleName}", cause)

/**
 * Writes user-submitted feedback to Firebase. Each submission becomes a doc
 * in the `feedback` Firestore collection with optional screenshot uploaded
 * to `feedback-screenshots/{anonUid}/` in Firebase Storage.
 *
 * The repository signs the device in anonymously on the first submission so
 * Firestore security rules can require `request.auth != null` — without
 * actually exposing any sign-in UI. The anonymous UID is also stored on
 * the doc so duplicate submissions from the same device can be grouped
 * during manual triage.
 *
 * [isAvailable] is `false` when the app was built without a
 * `google-services.json` (e.g. CI or a forked checkout without a Firebase
 * project). Callers should check this before calling [submit] and offer
 * the email fallback instead.
 */
class FeedbackRepository(@Suppress("unused") private val context: Context) {

    val isAvailable: Boolean
        get() = runCatching { FirebaseApp.getInstance() }.isSuccess

    /** Whether the repository will actually upload attached screenshots. */
    val supportsScreenshots: Boolean = UPLOAD_SCREENSHOTS

    suspend fun submit(
        kind: FeedbackKind,
        body: String,
        screenshot: Uri?,
    ): Result<Unit> = runCatching {
        check(isAvailable) { "Firebase is not configured." }

        // Step 1 — anonymous sign-in. Most likely failure mode here is
        // "Anonymous provider not enabled" in Firebase Auth settings.
        val userId = try {
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
            }
            requireNotNull(auth.currentUser).uid
        } catch (e: Exception) {
            Log.e(TAG, "Anonymous sign-in failed", e)
            throw FeedbackSubmitException("Sign-in", e)
        }

        // Step 2 — upload screenshot (optional). Gated behind UPLOAD_SCREENSHOTS
        // so we can ship text-only feedback while Storage init is still
        // failing on the Firebase side. When the flag is off and the user
        // attached a screenshot, we drop it silently here — the UI surfaces
        // the limitation upstream so they don't think it's been uploaded.
        val screenshotUrl: String? = if (UPLOAD_SCREENSHOTS && screenshot != null) {
            try {
                val storageRef = FirebaseStorage.getInstance().reference
                    .child("feedback-screenshots/$userId/${UUID.randomUUID()}.jpg")
                storageRef.putFile(screenshot).await()
                storageRef.downloadUrl.await().toString()
            } catch (e: Exception) {
                Log.e(TAG, "Screenshot upload failed", e)
                throw FeedbackSubmitException("Screenshot upload", e)
            }
        } else null

        // Step 3 — write the feedback document. Likely failure: firestore
        // rules deny the write (project in production mode with default
        // rules, or our rules weren't deployed yet).
        try {
            val payload = mutableMapOf<String, Any?>(
                "kind" to kind.value,
                "body" to body,
                "screenshotUrl" to screenshotUrl,
                "appVersionName" to BuildConfig.VERSION_NAME,
                "appVersionCode" to BuildConfig.VERSION_CODE,
                "androidSdk" to Build.VERSION.SDK_INT,
                "androidRelease" to Build.VERSION.RELEASE,
                "device" to "${Build.MANUFACTURER} ${Build.MODEL}",
                "anonId" to userId,
                "createdAt" to Timestamp.now(),
            )
            FirebaseFirestore.getInstance()
                .collection("feedback")
                .add(payload)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Firestore write failed", e)
            throw FeedbackSubmitException("Firestore write", e)
        }

        Log.i(TAG, "Feedback submitted: kind=${kind.value} uid=$userId")
        Unit
    }
}
