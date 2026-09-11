package com.fixvol.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.fixvol.app.audio.AudioCategory
import com.fixvol.app.audio.AudioRouteDetector
import com.fixvol.app.audio.PlaybackEvent
import com.fixvol.app.audio.PlaybackState
import com.fixvol.app.audio.PlaybackMonitor
import com.fixvol.app.core.EventCoordinator
import com.fixvol.app.core.NativeVolumeController
import com.fixvol.app.data.FixVolSettings
import com.fixvol.app.data.SettingsRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class PlaybackMonitorService : LifecycleService() {

    private lateinit var playbackMonitor: PlaybackMonitor
    private lateinit var nativeVolumeController: NativeVolumeController
    private lateinit var eventCoordinator: EventCoordinator
    private lateinit var settingsRepository: SettingsRepository

    private var currentSettings: FixVolSettings = FixVolSettings()
    private var currentVolumeLevel: Int = 0
    private var currentMaxVolume: Int = 0

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "PlaybackMonitorService onCreate")

        nativeVolumeController = NativeVolumeController(this)
        eventCoordinator = EventCoordinator(nativeVolumeController)
        playbackMonitor = PlaybackMonitor(this, AudioRouteDetector(this))
        settingsRepository = SettingsRepository(this)

        createNotificationChannel()
        refreshCurrentVolume()

        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Observe settings changes
        settingsRepository.settingsFlow.onEach { settings ->
            currentSettings = settings
            eventCoordinator.cooldownMs = settings.cooldownMs
            eventCoordinator.triggerOnlyOnFirstPlay = settings.triggerOnlyOnFirstPlay
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            TRIGGER_VOLUME_ACTION -> {
                Log.d(TAG, "Notification tap triggered volume panel request")
                try {
                    eventCoordinator.processEvent(
                        event = PlaybackEvent(
                            id = "notification_tap",
                            packageName = "notification_tap",
                            uid = 0,
                            usage = null,
                            contentType = null,
                            category = AudioCategory.MEDIA,
                            state = PlaybackState.ACTIVE
                        ),
                        globalRules = currentSettings.globalRules,
                        appRules = currentSettings.appRules
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to trigger volume from notification", e)
                }
            }
        }
        return START_STICKY
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
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FixVol")
            .setContentText("Volume: $currentVolumeLevel / $currentMaxVolume — Native volume control active")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(createTriggerPendingIntent())
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createTriggerPendingIntent(): PendingIntent {
        val intent = Intent(this, PlaybackMonitorService::class.java).apply {
            action = TRIGGER_VOLUME_ACTION
        }
        return PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun refreshCurrentVolume() {
        val manager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        currentVolumeLevel = manager.getStreamVolume(AudioManager.STREAM_MUSIC)
        currentMaxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        updateNotification()
    }

    private fun updateNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FixVol")
            .setContentText("Volume: $currentVolumeLevel / $currentMaxVolume — Native volume control active")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(createTriggerPendingIntent())
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "PlaybackMonitorService"
        private const val CHANNEL_ID = "fixvol_monitoring_channel"
        private const val NOTIFICATION_ID = 1001
        const val TRIGGER_VOLUME_ACTION = "com.fixvol.app.ACTION_TRIGGER_VOLUME"

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
