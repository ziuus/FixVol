package com.fixvol.app.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.ColorSpace
import android.graphics.ImageFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ScreenshotActivity : ComponentActivity() {

    private var projection: MediaProjection? = null
    private var isCapturing = false

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val mgr = getSystemService(MediaProjectionManager::class.java) as MediaProjectionManager
            projection = mgr.getMediaProjection(result.resultCode, result.data!!)
            performCapture()
        } else {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestScreenCapture()
        } else {
            finish()
        }
    }

    private fun requestScreenCapture() {
        if (isCapturing) return
        isCapturing = true
        try {
            val mgr = getSystemService(MediaProjectionManager::class.java) as MediaProjectionManager
            mediaProjectionLauncher.launch(mgr.createScreenCaptureIntent())
        } catch (e: Exception) {
            finish()
        }
    }

    private fun performCapture() {
        if (projection == null) {
            finish()
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                val wm = getSystemService(WINDOW_SERVICE) as WindowManager
                val display = wm.defaultDisplay
                val metrics = DisplayMetrics()
                display.getRealMetrics(metrics)
                val width = metrics.widthPixels
                val height = metrics.heightPixels
                val density = metrics.densityDpi

                val reader = ImageReader.newInstance(width, height, ImageFormat.PRIVATE, 2)
                val vd = projection!!.createVirtualDisplay(
                    "FixVol-Screenshot",
                    width, height, density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    reader.surface, null, null
                )

                val image = reader.acquireLatestImage()
                val bitmap = image.use { img ->
                    val buffer = img.hardwareBuffer
                    buffer?.let {
                        Bitmap.wrapHardwareBuffer(it, ColorSpace.get(ColorSpace.Named.SRGB))
                    }
                }
                reader.close()
                vd.release()

                if (bitmap != null) {
                    val dir = File(getExternalFilesDir(null), "screenshots")
                    if (!dir.exists()) dir.mkdirs()
                    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val file = File(dir, "fixvol_$ts.png")
                    FileOutputStream(file).use { out ->
                        bitmap.compress(CompressFormat.PNG, 95, out)
                    }
                    bitmap.recycle()
                }

                projection = null
                isCapturing = false
                finish()
            } catch (e: Exception) {
                projection = null
                isCapturing = false
                finish()
            }
        }
    }

    override fun finish() {
        projection = null
        isCapturing = false
        super.finish()
    }
}
