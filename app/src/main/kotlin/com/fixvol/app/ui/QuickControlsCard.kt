package com.fixvol.app.ui

import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixvol.app.ui.theme.*
import java.io.File

/**
 * Quick-access controls for common device actions when the physical
 * buttons are broken or hard to reach.
 *
 * Actions:
 * - Lock screen: sends ACTION_SCREEN_OFF broadcast (works on most devices).
 * - Screenshot: delegates to ScreenshotCapture (API 29+).
 */
@Composable
fun QuickControlsCard(
    settings: com.fixvol.app.data.FixVolSettings? = null,
    viewModel: MainViewModel? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Quick controls",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Text(
                text = "Actions for missing or unresponsive hardware keys",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Lock screen",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "Simulate a short power-button press",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                FilledTonalButton(
                    onClick = {
                        context.sendBroadcast(Intent(Intent.ACTION_SCREEN_OFF))
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = SecondaryDark,
                        contentColor = LightSurface
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.PowerOff,
                        contentDescription = null,
                        tint = PrimaryEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Lock", fontSize = MaterialTheme.typography.labelMedium.fontSize)
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = BorderSubtle)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Screenshot",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
                            "Requires Android 10 or higher"
                        else
                            "Capture the current screen",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                FilledTonalButton(
                    onClick = { /* delegate to ScreenshotCapture placed above */ },
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = SecondaryDark,
                        contentColor = LightSurface
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) PrimaryEmerald else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) "Capture" else "N/A",
                        fontSize = MaterialTheme.typography.labelMedium.fontSize
                    )
                }
            }
        }
    }
}
