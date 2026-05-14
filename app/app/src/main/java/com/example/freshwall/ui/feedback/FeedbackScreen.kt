package com.example.freshwall.ui.feedback

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.freshwall.FreshWallApplication
import com.example.freshwall.data.feedback.FeedbackKind
import com.example.freshwall.ui.settings.SettingsTopBar
import kotlinx.coroutines.launch

// Email fallback — used when Firebase isn't configured (e.g. a forked
// checkout without google-services.json). Swap to your real address.
private const val SUPPORT_EMAIL = "support@freshwall.app"

private enum class SubmitState { IDLE, SENDING, SUCCESS }

@Composable
fun FeedbackScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as FreshWallApplication
    val feedbackRepo = app.feedbackRepository
    val scope = rememberCoroutineScope()

    var kind by remember { mutableStateOf(FeedbackKind.BUG) }
    var body by remember { mutableStateOf("") }
    var attachedImage by remember { mutableStateOf<Uri?>(null) }
    var submitState by remember { mutableStateOf(SubmitState.IDLE) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        attachedImage = uri
    }

    val sendViaEmail: () -> Unit = {
        val subject = if (kind == FeedbackKind.BUG) "FreshWall · Bug report"
                      else "FreshWall · Feedback"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (attachedImage != null) "image/*" else "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            attachedImage?.let { putExtra(Intent.EXTRA_STREAM, it) }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, "Send feedback"))
        }.onFailure {
            Toast.makeText(context, "No email app available", Toast.LENGTH_SHORT).show()
        }
        Unit
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsTopBar(title = "Send feedback", onBack = onBack)

            Spacer(Modifier.height(16.dp))

            // Hero icon
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Forum,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(48.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = if (submitState == SubmitState.SUCCESS) {
                        "Thanks — we'll take a look."
                    } else {
                        "Tell us what's on your mind"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }

            Spacer(Modifier.height(24.dp))

            // Kind selector
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = kind == FeedbackKind.BUG,
                    onClick = { kind = FeedbackKind.BUG },
                    label = { Text("Bug") },
                    enabled = submitState != SubmitState.SENDING,
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = kind == FeedbackKind.FEEDBACK,
                    onClick = { kind = FeedbackKind.FEEDBACK },
                    label = { Text("Feedback") },
                    enabled = submitState != SubmitState.SENDING,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = {
                    Text(
                        text = if (kind == FeedbackKind.BUG) {
                            "What happened? Steps to reproduce, expected vs actual…"
                        } else {
                            "What would you like to see? Ideas, suggestions…"
                        },
                    )
                },
                minLines = 6,
                enabled = submitState != SubmitState.SENDING,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            // Image attachment (Bug only)
            if (kind == FeedbackKind.BUG) {
                Spacer(Modifier.height(16.dp))
                if (attachedImage == null) {
                    OutlinedButton(
                        onClick = {
                            imagePicker.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                                )
                            )
                        },
                        enabled = submitState != SubmitState.SENDING,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Attach screenshot")
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                    ) {
                        AsyncImage(
                            model = attachedImage,
                            contentDescription = "Attached screenshot",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp)),
                        )
                        if (!feedbackRepo.supportsScreenshots) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Screenshot uploads are temporarily disabled. " +
                                    "Use the email button below to include this screenshot.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { attachedImage = null },
                            enabled = submitState != SubmitState.SENDING,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Remove attachment")
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (body.isBlank()) {
                        Toast.makeText(
                            context,
                            "Please add some details first",
                            Toast.LENGTH_SHORT,
                        ).show()
                        return@Button
                    }
                    if (!feedbackRepo.isAvailable) {
                        // Firebase isn't configured in this build — fall back
                        // to the email intent so the user still has a way to
                        // reach us.
                        sendViaEmail()
                        return@Button
                    }
                    submitState = SubmitState.SENDING
                    scope.launch {
                        val outcome = feedbackRepo.submit(kind, body, attachedImage)
                        outcome
                            .onSuccess {
                                submitState = SubmitState.SUCCESS
                                body = ""
                                attachedImage = null
                            }
                            .onFailure { e ->
                                submitState = SubmitState.IDLE
                                // Show the step + underlying Firebase message
                                // so the user (or a developer reading logcat)
                                // can tell which stage failed.
                                val msg = e.message ?: e.javaClass.simpleName
                                Toast.makeText(
                                    context,
                                    "Couldn't send. $msg",
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                    }
                },
                enabled = submitState != SubmitState.SENDING,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
            ) {
                when (submitState) {
                    SubmitState.SENDING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.size(12.dp))
                        Text("Sending…")
                    }
                    SubmitState.SUCCESS -> Text("Sent")
                    SubmitState.IDLE -> Text("Send")
                }
            }

            // Email fallback link — visible even when Firebase IS configured,
            // so anyone who'd rather email us still can.
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = sendViaEmail,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
            ) {
                Text("Or email us instead")
            }

            Spacer(
                Modifier.height(
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
                )
            )
        }
    }
}
