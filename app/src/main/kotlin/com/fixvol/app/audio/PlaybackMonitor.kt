package com.fixvol.app.audio

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID

class PlaybackMonitor(
    private val context: Context,
    private val routeDetector: AudioRouteDetector = AudioRouteDetector(context)
) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val packageManager: PackageManager = context.packageManager
    private val ownPackageName: String = context.packageName

    private val _events = MutableSharedFlow<PlaybackEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<PlaybackEvent> = _events.asSharedFlow()

    private var callback: AudioManager.AudioPlaybackCallback? = null
    private val uidPackageCache = mutableMapOf<Int, String>()

    fun startMonitoring() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Log.w(TAG, "AudioPlaybackCallback requires API level 26+")
            return
        }

        if (callback != null) return

        val monitorCallback = object : AudioManager.AudioPlaybackCallback() {
            override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) {
                handlePlaybackConfigs(configs)
            }
        }

        try {
            audioManager?.registerAudioPlaybackCallback(monitorCallback, null)
            callback = monitorCallback
            Log.d(TAG, "AudioPlaybackCallback registered successfully")

            // Inspect currently active configs on start
            audioManager?.activePlaybackConfigurations?.let { handlePlaybackConfigs(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register AudioPlaybackCallback", e)
        }
    }

    fun stopMonitoring() {
        callback?.let {
            try {
                audioManager?.unregisterAudioPlaybackCallback(it)
                Log.d(TAG, "AudioPlaybackCallback unregistered")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister AudioPlaybackCallback", e)
            }
        }
        callback = null
    }

    private fun handlePlaybackConfigs(configs: List<AudioPlaybackConfiguration>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val route = routeDetector.getCurrentRoute()

        for (config in configs) {
            val isActive = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val method = config.javaClass.getMethod("isActive")
                    method.invoke(config) as? Boolean ?: true
                } catch (e: Exception) {
                    true
                }
            } else {
                true
            }

            if (!isActive) continue

            val attributes = config.audioAttributes
            val usage = attributes?.usage
            val contentType = attributes?.contentType

            val category = PlaybackClassifier.classify(usage, contentType)

            val uid = try {
                val method = config.javaClass.getMethod("getClientUid")
                method.invoke(config) as? Int
            } catch (e: Exception) {
                null
            }

            val packageName = uid?.let { resolvePackageName(it) }

            // Do not act on FixVol's own playback events
            if (packageName == ownPackageName) continue

            val event = PlaybackEvent(
                id = UUID.randomUUID().toString(),
                packageName = packageName,
                uid = uid,
                usage = usage,
                contentType = contentType,
                category = category,
                state = PlaybackState.STARTED,
                audioRoute = route,
                timestamp = System.currentTimeMillis()
            )

            Log.d(
                TAG,
                "PLAYBACK_STARTED package=$packageName uid=$uid usage=$usage category=$category route=$route"
            )

            _events.tryEmit(event)
        }
    }

    private fun resolvePackageName(uid: Int): String? {
        if (uidPackageCache.containsKey(uid)) {
            return uidPackageCache[uid]
        }

        val packages = try {
            packageManager.getPackagesForUid(uid)
        } catch (e: Exception) {
            null
        }

        val pkgName = packages?.firstOrNull()
        if (pkgName != null) {
            uidPackageCache[uid] = pkgName
        }
        return pkgName
    }

    companion object {
        private const val TAG = "PlaybackMonitor"
    }
}
