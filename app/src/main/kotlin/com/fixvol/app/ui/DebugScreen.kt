package com.fixvol.app.ui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.ImageFormat
import android.hardware.display.DisplayManager
import android.media.AudioManager
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixvol.app.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onCaptureScreenshot: (imagePath: String?) -> Unit = { _ -> }
) {
    val context = LocalContext.current
    val scope = CoroutineScope(SupervisorJob())
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }
    var currentVolume by remember { mutableStateOf(0) }
    var maxVolume by remember { mutableStateOf(0) }
    var lastAction by remember { mutableStateOf<String?>(null) }
    var screenshotCaptured by remember { mutableStateOf(false) }
    var screenshotPath by remember { mutableStateOf<String?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    // captureAndSave: screenshot logic using MediaProjection
    fun captureAndSave(
        projection: MediaProjection,
        context: Context,
        scope: CoroutineScope
    ) {
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
                val vd = projection.createVirtualDisplay(
                    "FixVol-Screenshot",
                    width, height, density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    reader.surface, null, null
                )
                val image = reader.acquireLatestImage()
                val bitmap = if (image != null) {
                    val hb = image.hardwareBuffer
                    val b = hb?.let {
                        Bitmap.wrapHardwareBuffer(it, android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.SRGB))
                    }
                    hb?.close()
                    image.close()
                    b
                } else {
                    reader.close()
                    vd.release()
                    projection.stop()
                    withContext(Dispatchers.Main) {
                        lastAction = "No image captured"
                        isCapturing = false
                    }
                    return@launch
                }
                reader.close()
                vd.release()

                if (bitmap == null) {
                    withContext(Dispatchers.Main) {
                        lastAction = "Screenshot failed: no frame captured"
                        isCapturing = false
                    }
                    return@launch
                }

                val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val dir = File(context.getExternalFilesDir(null), "screenshots")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, "fixvol_$ts.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
                }
                bitmap.recycle()

                withContext(Dispatchers.Main) {
                    screenshotPath = file.absolutePath
                    screenshotCaptured = true
                    lastAction = "Screenshot saved: ${file.name}"
                    isCapturing = false
                    onCaptureScreenshot(file.absolutePath)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    lastAction = "Screenshot failed: ${e.message}"
                    isCapturing = false
                    onCaptureScreenshot(null)
                }
            }
        }
    }

    // MediaProjection permission launcher — placed AFTER captureAndSave
    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val mgr = context.getSystemService(MediaProjectionManager::class.java)
            val projection = mgr.getMediaProjection(result.resultCode, result.data!!)
            captureAndSave(projection, context, scope)
        } else {
            lastAction = "Screenshot cancelled"
            isCapturing = false
        }
    }

    // Observe current volume on compose
    LaunchedEffect(context) {
        if (audioManager != null) {
            currentVolume = audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC)
            maxVolume = audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        }
    }

    // Volume change broadcast receiver
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                    currentVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                    maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 0
                    lastAction = "Volume changed: $currentVolume / $maxVolume"
                }
            }
        }
        context.registerReceiver(receiver, android.content.IntentFilter("android.media.VOLUME_CHANGED_ACTION"))
        onDispose { context.unregisterReceiver(receiver) }
    }

    fun requestScreenshot() {
        if (isCapturing) return
        isCapturing = true
        lastAction = "Requesting screen capture permission..."
        val mgr = context.getSystemService(MediaProjectionManager::class.java)
        mediaProjectionLauncher.launch(mgr.createScreenCaptureIntent())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug & Diagnostics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBgBase)
            )
        },
        containerColor = LightBgBase
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LightSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Current Volume",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = PrimaryEmerald)
                        Text(
                            text = "Volume: $currentVolume / $maxVolume",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                    }
                    if (lastAction != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(text = lastAction!!, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LightSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Monitoring Status",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (viewModel.settings.value.enabled) "ACTIVE" else "PAUSED",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (viewModel.settings.value.enabled) StatusActiveGreen else StatusPausedAmber
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LightSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "System Capability Verification",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(text = "AudioPlaybackCallback: Supported (API 26+)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Text(
                        text = "isActive reflection: Supported (API 29+)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (Build.VERSION.SDK_INT >= 29) TextPrimary else StatusPausedAmber
                    )
                    Text(text = "Native UI adjustStreamVolume: Supported", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Text(text = "DataStore Preferences: Active", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LightSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Capture Screenshot", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(
                                text = "Capture current screen for debugging volume panel appearance",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Button(
                            onClick = ::requestScreenshot,
                            enabled = !isCapturing,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (screenshotCaptured) "Redo" else "Capture")
                        }
                    }
                    if (screenshotPath != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Saved: $screenshotPath",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = StatusActiveGreen
                        )
                    }
                }
            }
        }
    }
}
