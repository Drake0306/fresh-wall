package io.github.drake0306.freshwall.ui.donate

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import io.github.drake0306.freshwall.R
import io.github.drake0306.freshwall.ui.settings.SettingsTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Two donate destinations — pick whichever matches the donor's region.
// Razorpay handles UPI + Indian cards natively at ~2.4% fees;
// Ko-fi covers international payments via PayPal + card at ~5% fees.
// The bundled QR is the same Razorpay UPI handle, packaged so users can
// save / share / scan it from their UPI app of choice.
private const val DONATE_URL_INR = "https://razorpay.me/@abhinavroy"
private const val DONATE_URL_USD = "https://ko-fi.com/abhinavroy"

// Feature flag for the "Scan UPI QR" card on the donate screen.
// Off for v1 — we'd rather route everyone through the razorpay.me link first
// while we see whether direct-QR-pay is something users actually ask for.
// Flip to `true` to re-expose the card; the dialog + save flow are still
// wired up and the upi_qr.jpg drawable stays in res/drawable-nodpi/. No
// other code changes needed to turn it back on.
private const val SHOW_UPI_QR_OPTION = false

@Composable
fun DonateScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showQrDialog by remember { mutableStateOf(false) }

    fun openExternal(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    // Saving the QR follows the same WRITE_EXTERNAL_STORAGE flow that
    // DetailScreen uses for wallpaper downloads: on API <29 we ask once
    // and proceed if granted. On API ≥29 MediaStore writes don't need
    // the permission at all so the hasPermission shortcut is the common path.
    val savePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            scope.launch { saveQrAndToast(context) }
        } else {
            Toast.makeText(
                context,
                "Allow storage access to save the QR",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    val onSaveQrClicked: () -> Unit = {
        val needsRuntimePermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
        val hasPermission = !needsRuntimePermission ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            scope.launch { saveQrAndToast(context) }
        } else {
            savePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
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
            SettingsTopBar(title = "Support the work", onBack = onBack)

            Spacer(Modifier.height(24.dp))

            // Hero logo. Reuses the app_logo PNG that's already in
            // drawable-nodpi/ — same artwork as the launcher / splash / welcome
            // step, so this screen feels of-a-piece with the rest of the app.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.Transparent,
                    shadowElevation = 10.dp,
                    modifier = Modifier.size(112.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.app_logo),
                        contentDescription = "FreshWall logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Coffee keeps me building",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Human, no-toggle copy. No honor system, no unlock, no shame.
            // Donations are voluntary. We say thanks regardless.
            Column(modifier = Modifier.padding(horizontal = 32.dp)) {
                Text(
                    text = "I build open-source apps in my spare time, mostly on weekends. " +
                        "FreshWall is one of them, given away free because tools should be " +
                        "free when they can be.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "If it's earned a spot on your phone and you want to chip in, " +
                        "pick whichever option fits where you are. If neither feels right, " +
                        "no stress — keep using the app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DonationOption(
                    title = "Buy me a chai · India",
                    body = "UPI, cards, netbanking via Razorpay. Lower fees, lands in " +
                        "rupees, takes about ten seconds on UPI.",
                    onClick = { openExternal(DONATE_URL_INR) },
                )
                if (SHOW_UPI_QR_OPTION) {
                    DonationOption(
                        title = "Scan UPI QR · India",
                        body = "Prefer your usual UPI app? Tap to view the QR, save it to " +
                            "your gallery, and scan it from PhonePe / GPay / Paytm / any UPI app.",
                        onClick = { showQrDialog = true },
                    )
                }
                DonationOption(
                    title = "Buy me a coffee · Everywhere else",
                    body = "Cards and PayPal via Ko-fi. Best if you're outside India. " +
                        "Minimum is around three dollars.",
                    onClick = { openExternal(DONATE_URL_USD) },
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Either way, thanks for being here.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
            )

            Spacer(Modifier.height(10.dp))

            // Closing flourish — small filled heart in the primary tint,
            // centred below the thanks line. Rounded variant chosen because
            // the sharp/outlined variants read colder for a thank-you note.
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterHorizontally),
            )

            Spacer(
                Modifier.height(
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 32.dp,
                ),
            )
        }
    }

    if (showQrDialog) {
        UpiQrDialog(
            onDismiss = { showQrDialog = false },
            onSave = onSaveQrClicked,
        )
    }
}

/**
 * One donation route — title (currency · audience) on top, supporting line
 * underneath, the whole card is the tap target so users don't have to aim
 * for a specific pixel.
 */
@Composable
private fun DonationOption(
    title: String,
    body: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Modal dialog that renders the bundled UPI QR at a scannable size with a
 * "Save to Photos" action. Width is bounded so the QR section is roughly
 * 220–260dp on phones — comfortably above the "minimum 150dp for reliable
 * scan" threshold UPI apps use.
 */
@Composable
private fun UpiQrDialog(
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QrCode2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Scan to pay",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Save the image and open it in your UPI app, or scan it from " +
                        "another phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))

                // The bundled QR image. Constrained max-height so it scales
                // down on small devices but the QR portion stays scannable.
                Box(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.upi_qr),
                        contentDescription = "UPI payment QR code for ABHINAV ROY",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                    Spacer(Modifier.size(8.dp))
                    FilledTonalButton(onClick = onSave) {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.size(8.dp))
                        Text("Save to Photos")
                    }
                }
            }
        }
    }
}

/**
 * Streams the bundled `upi_qr.jpg` resource bytes directly into MediaStore.
 * No decode-then-re-encode pass — that would risk JPEG compression artifacts
 * eating QR error-correction blocks and breaking scanability. We just copy
 * the original bytes byte-for-byte into the gallery's Pictures/FreshWall folder.
 */
private suspend fun saveQrAndToast(context: Context) {
    val result = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "freshwall-upi-qr.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/FreshWall",
                    )
                }
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Failed to create MediaStore entry")
            context.resources.openRawResource(R.drawable.upi_qr).use { input ->
                resolver.openOutputStream(uri).use { output ->
                    requireNotNull(output) { "Failed to open output stream" }
                    input.copyTo(output)
                }
            }
        }
    }
    val msg = if (result.isSuccess) "QR saved to Photos" else "Couldn't save QR"
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}
