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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.freshwall.ui.settings.SettingsTopBar

// Placeholder — swap to your real support email.
private const val SUPPORT_EMAIL = "support@freshwall.app"

private enum class FeedbackKind { BUG, FEEDBACK }

@Composable
fun FeedbackScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    var kind by remember { mutableStateOf(FeedbackKind.BUG) }
    var body by remember { mutableStateOf("") }
    var attachedImage by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        attachedImage = uri
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
                    text = "Tell us what's on your mind",
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
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = kind == FeedbackKind.FEEDBACK,
                    onClick = { kind = FeedbackKind.FEEDBACK },
                    label = { Text("Feedback") },
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
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { attachedImage = null },
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
                    val subject = if (kind == FeedbackKind.BUG) {
                        "FreshWall · Bug report"
                    } else {
                        "FreshWall · Feedback"
                    }
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
                        Toast.makeText(
                            context,
                            "No email app available",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
            ) {
                Text("Send")
            }

            Spacer(
                Modifier.height(
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
                )
            )
        }
    }
}
