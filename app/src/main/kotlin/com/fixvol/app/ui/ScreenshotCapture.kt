package com.fixvol.app.ui

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.ColorSpace
import android.graphics.ImageFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.fixvol.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reusable screenshot capture component using MediaProjection + HardwareBuffer.
 * Works on API 29+ (Android 10+). Shows capture status inline.
 *
 * Usage: place one instance per screen that needs screenshot capability.
 * Each instance manages its own permission launcher and internal state.
 */
@Composable
fun ScreenshotCapture(
    onCaptured: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isCapturing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var savedPath by remember { mutableStateOf<String?>(null) }

    var projection by remember { mutableStateOf<MediaProjection?>(null) }

    // Local function to perform the actual capture — defined before caller
    fun performCapture() {
        val proj = projection ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
                val display = wm.defaultDisplay
                val metrics = DisplayMetrics()
                display.getRealMetrics(metrics)
                val width = metrics.widthPixels
                val height = metrics.heightPixels
                val density = metrics.densityDpi

                val reader = ImageReader.newInstance(width, height, ImageFormat.PRIVATE, 2)
                val vd = proj.createVirtualDisplay(
                    "FixVol-Screenshot",
                    width, height, density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    reader.surface, null, null
                )
                val image = reader.acquireLatestImage()
                val hardwareBuffer = image.hardwareBuffer
                val bitmap = hardwareBuffer?.let {
                    Bitmap.wrapHardwareBuffer(it, ColorSpace.get(ColorSpace.Named.SRGB))
                }
                hardwareBuffer?.close()
                image.close()
                reader.close()
                vd.release()

                if (bitmap == null) {
                    withContext(Dispatchers.Main) {
                        status = "Screenshot failed: no frame captured"
                        isCapturing = false
                    }
                    return@launch
                }

                val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val dir = File(context.getExternalFilesDir(null), "screenshots")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, "fixvol_$ts.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(CompressFormat.PNG, 95, out)
                }
                bitmap.recycle()

                withContext(Dispatchers.Main) {
                    savedPath = file.absolutePath
                    status = "Screenshot saved: ${file.name}"
                    isCapturing = false
                    onCaptured(file.absolutePath)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    status = "Screenshot failed: ${e.message}"
                    isCapturing = false
                }
            }
        }
    }

    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val mgr = context.getSystemService(MediaProjectionManager::class.java)
                as MediaProjectionManager
            projection = mgr.getMediaProjection(result.resultCode, result.data!!)
            performCapture()
        } else {
            status = "Screenshot cancelled"
            isCapturing = false
            projection = null
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Capture Screenshot",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
                        "Requires Android 10 or higher"
                    else
                        "Capture current screen for debugging",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Button(
                onClick = {
                    if (isCapturing || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@Button
                    isCapturing = true
                    status = "Requesting screen capture permission..."
                    val mgr = context.getSystemService(MediaProjectionManager::class.java)
                        as MediaProjectionManager
                    mediaProjectionLauncher.launch(mgr.createScreenCaptureIntent())
                },
                enabled = !isCapturing && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(if (savedPath != null) "Redo" else "Capture")
            }
        }
        if (savedPath != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Saved: $savedPath",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = StatusActiveGreen
            )
        }
        if (status != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = status!!,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
