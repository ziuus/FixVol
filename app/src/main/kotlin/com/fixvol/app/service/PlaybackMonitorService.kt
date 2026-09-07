package com.fixvol.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.fixvol.app.audio.AudioRouteDetector
import com.fixvol.app.audio.PlaybackMonitor
import com.fixvol.app.core.EventCoordinator
import com.fixvol.app.core.NativeVolumeController
import com.fixvol.app.data.FixVolSettings
import com.fixvol.app.data.SettingsRepository
import com.fixvol.app.ui.MainActivity
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class PlaybackMonitorService : LifecycleService() {

    private lateinit var playbackMonitor: PlaybackMonitor
    private lateinit var nativeVolumeController: NativeVolumeController
    private lateinit var eventCoordinator: EventCoordinator
    private lateinit var settingsRepository: SettingsRepository

    private var currentSettings: FixVolSettings = FixVolSettings()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "PlaybackMonitorService onCreate")

        nativeVolumeController = NativeVolumeController(this)
        eventCoordinator = EventCoordinator(nativeVolumeController)
        playbackMonitor = PlaybackMonitor(this, AudioRouteDetector(this))
        settingsRepository = SettingsRepository(this)

        createNotificationChannel()

        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Observe settings changes
        settingsRepository.settingsFlow.onEach { settings ->
            currentSettings = settings
            eventCoordinator.cooldownMs = settings.cooldownMs
            if (!settings.enabled) {
                stopSelf()
            }
        }.launchIn(lifecycleScope)

        // Listen to playback events
        playbackMonitor.events.onEach { event ->
            eventCoordinator.processEvent(
                event = event,
                globalRules = currentSettings.globalRules,
                appRules = currentSettings.appRules
            )
        }.launchIn(lifecycleScope)

        playbackMonitor.startMonitoring()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "PlaybackMonitorService onDestroy")
        playbackMonitor.stopMonitoring()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "FixVol Service Status",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Shows that native volume monitoring is active"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FixVol")
            .setContentText("Native volume control is active")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val TAG = "PlaybackMonitorService"
        private const val CHANNEL_ID = "fixvol_monitoring_channel"
        private const val NOTIFICATION_ID = 1001

        fun startService(context: Context) {
            val intent = Intent(context, PlaybackMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, PlaybackMonitorService::class.java)
            context.stopService(intent)
        }
    }
}
