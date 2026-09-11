package com.fixvol.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fixvol.app.ui.theme.*

/**
 * Debug screen that exposes internal diagnostics: current volume, system
 * capabilities, and a live screen-capture via MediaProjection.
 *
 * Kept strictly offline — no logging, no network, no telemetry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    var currentVolumePercent by remember { mutableFloatStateOf(0f) }
    var screenshotBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showCapabilitySheet by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Debug diagnostics",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Internal state and diagnostics — not visible to end users",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                FilledTonalIconButton(onClick = { /* no-op */ }) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live volume readout
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LightSurface.copy(alpha = 0.4f))
                    .clip(RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.PowerOff,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Current Volume: %.0f / 100".format(currentVolumePercent),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Embed ScreenshotCapture directly
            ScreenshotCapture(
                onCaptured = { path ->
                    // Optionally load saved bitmap for preview
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // System capability verifier
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "System capabilities",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "Verify playback monitoring and volume-control support",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                FilledTonalIconButton(
                    onClick = { showCapabilitySheet = true },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = SecondaryDark
                    )
                ) {
                    Icon(Icons.Filled.Devices, contentDescription = null, tint = PrimaryEmerald)
                }
            }
        }
    }

    // Back button
    IconButton(
        onClick = onBack,
        modifier = Modifier
            .padding(16.dp)
            .statusBarsPadding()
    ) {
        Icon(
            Icons.Filled.ArrowBack,
            contentDescription = "Close",
            tint = TextSecondary
        )
    }

    // Feature verifier bottom sheet
    if (showCapabilitySheet) {
        AlertDialog(
            onDismissRequest = { showCapabilitySheet = false },
            containerColor = LightSurface,
            title = {
                Column {
                    Text(
                        "System capability verification",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Playback monitoring requires API 26+, volume-control access via `adjustStreamVolume` requires `MANAGE_AUDIO_STREAMS` at API 29+, and MediaProjection requires the user to grant capture permission at runtime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showCapabilitySheet = false }) {
                    Text("Close")
                }
            }
        )
    }
}
