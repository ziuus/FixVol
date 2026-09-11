package com.fixvol.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fixvol.app.R
import com.fixvol.app.ui.theme.*

/**
 * Splash screen shown while the main content is hydrated.
 * Displays the FixVol launcher icon on the app background
 * color, then fades out after a short delay so the launch
 * feels intentional on both fast and slow devices.
 */
@Composable
fun FixVolSplash(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    if (visible) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(LightBgBase),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher),
                    contentDescription = "FixVol",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(minOf(120.dp, 200.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(LightSurface)
                        .padding(4.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "FixVol",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimary
                )
                Text(
                    text = "Volume control for broken buttons",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}
