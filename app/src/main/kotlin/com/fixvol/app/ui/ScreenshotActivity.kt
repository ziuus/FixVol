package com.fixvol.app.ui

import android.app.Activity
import android.content.Intent
import android.graphics.ImageFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.WindowManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ScreenshotActivity : ComponentActivity() {

    private val TAG = "ScreenshotActivity"
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

                // Use ImageInputStream for reliable capture on all supported API levels
                val reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 2)
                val vd = projection!!.createVirtualDisplay(
                    "FixVol-Screenshot",
                    width, height, density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    reader.surface, null, null
                )

                // Give the virtual display a moment to render
                kotlinx.coroutines.delay(300)

                val image = reader.acquireLatestImage()
                if (image != null) {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)

                    val dir = File(getExternalFilesDir(null), "screenshots")
                    if (!dir.exists()) dir.mkdirs()
                    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val file = File(dir, "fixvol_$ts.jpg")
                    FileOutputStream(file).use { out ->
                        out.write(bytes)
                    }

                    Log.d(TAG, "Screenshot saved: ${file.absolutePath} ($width×$height)")
                    image.close()
                } else {
                    Log.w(TAG, "No image acquired from ImageReader")
                }

                reader.close()
                vd.release()
                projection = null
                isCapturing = false
                finish()
            } catch (e: Exception) {
                Log.e(TAG, "Screenshot capture failed", e)
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
