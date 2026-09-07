package com.fixvol.app.core

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.fixvol.app.audio.AudioCategory

open class NativeVolumeController(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    data class TestResult(
        val success: Boolean,
        val volumeBefore: Int,
        val volumeAfter: Int,
        val streamType: Int
    )

    open fun requestVolumeUi(category: AudioCategory): Boolean {
        val manager = audioManager ?: return false
        val streamType = mapCategoryToStreamType(category)

        return try {
            manager.adjustStreamVolume(
                streamType,
                AudioManager.ADJUST_SAME,
                AudioManager.FLAG_SHOW_UI
            )
            Log.d(TAG, "Requested SystemUI volume panel for category=$category stream=$streamType")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request SystemUI volume panel", e)
            false
        }
    }

    open fun testNativeVolumePanel(category: AudioCategory = AudioCategory.MEDIA): TestResult {
        val manager = audioManager ?: return TestResult(false, -1, -1, -1)
        val streamType = mapCategoryToStreamType(category)

        val volumeBefore = manager.getStreamVolume(streamType)
        val triggered = requestVolumeUi(category)
        val volumeAfter = manager.getStreamVolume(streamType)

        val success = triggered && (volumeBefore == volumeAfter)
        Log.d(
            TAG,
            "Test result: success=$success before=$volumeBefore after=$volumeAfter stream=$streamType"
        )
        return TestResult(success, volumeBefore, volumeAfter, streamType)
    }

    private fun mapCategoryToStreamType(category: AudioCategory): Int {
        return when (category) {
            AudioCategory.MEDIA -> AudioManager.STREAM_MUSIC
            AudioCategory.ALARM -> AudioManager.STREAM_ALARM
            AudioCategory.RINGTONE -> AudioManager.STREAM_RING
            AudioCategory.NOTIFICATION -> AudioManager.STREAM_NOTIFICATION
            AudioCategory.VOICE_CALL -> AudioManager.STREAM_VOICE_CALL
            AudioCategory.SYSTEM -> AudioManager.STREAM_SYSTEM
            AudioCategory.ACCESSIBILITY -> AudioManager.STREAM_ACCESSIBILITY
            AudioCategory.UNKNOWN -> AudioManager.STREAM_MUSIC
        }
    }

    companion object {
        private const val TAG = "NativeVolumeController"
    }
}
