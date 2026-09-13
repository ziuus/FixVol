package com.fixvol.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.fixvol.app.R
import com.fixvol.app.audio.AudioCategory
import com.fixvol.app.audio.AudioRouteDetector
import com.fixvol.app.audio.PlaybackEvent
import com.fixvol.app.audio.PlaybackState
import com.fixvol.app.audio.PlaybackMonitor
import com.fixvol.app.core.EventCoordinator
import com.fixvol.app.core.NativeVolumeController
import com.fixvol.app.data.FixVolSettings
import com.fixvol.app.data.SettingsRepository
import com.fixvol.app.ui.MainActivity
import com.fixvol.app.ui.ScreenshotActivity
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
            SCREENSHOT_ACTION -> {
                Log.d(TAG, "Notification screenshot action triggered")
                try {
                    val intent = Intent(this, ScreenshotActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to launch screenshot activity", e)
                }
            }
            LOCK_SCREEN_ACTION -> {
                Log.d(TAG, "Notification lock screen action triggered")
                lockScreen()
            }
            POWER_MENU_ACTION -> {
                Log.d(TAG, "Notification power menu action triggered")
                openPowerMenu()
            }
            OPEN_APP_ACTION -> {
                Log.d(TAG, "Notification open app action triggered")
                openApp()
            }
        }
        return START_STICKY
    }

    private fun lockScreen() {
        try {
            val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            val adminComponent = ComponentName(this, DeviceAdminReceiver::class.java)
            if (devicePolicyManager?.isAdminActive(adminComponent) == true) {
                devicePolicyManager.lockNow()
                Log.d(TAG, "Screen locked via DevicePolicyManager")
            } else {
                // Fallback: send broadcast to trigger screen off
                sendBroadcast(Intent(Intent.ACTION_SCREEN_OFF))
                Log.d(TAG, "Screen lock via broadcast fallback")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to lock screen", e)
            sendBroadcast(Intent(Intent.ACTION_SCREEN_OFF))
        }
    }

    private fun openPowerMenu() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && powerManager?.isInteractive == true) {
                val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                val adminComponent = ComponentName(this, DeviceAdminReceiver::class.java)
                if (devicePolicyManager?.isAdminActive(adminComponent) == true) {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    startActivity(intent)
                    Log.d(TAG, "Opened power usage settings")
                } else {
                    Log.d(TAG, "Power menu not available without DeviceAdmin")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open power menu", e)
        }
    }

    private fun openApp() {
        try {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            Log.d(TAG, "Opened main app")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app", e)
        }
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
            .setContentText("Volume: $currentVolumeLevel / $currentMaxVolume")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(
                R.drawable.ic_menu_music,
                "Volume",
                createTriggerPendingIntent()
            )
            .addAction(
                R.drawable.ic_menu_camera,
                "Screenshot",
                createScreenshotPendingIntent()
            )
            .addAction(
                R.drawable.ic_menu_power,
                "Lock",
                createLockScreenPendingIntent()
            )
            .addAction(
                R.drawable.ic_menu_power,
                "Power Menu",
                createPowerMenuPendingIntent()
            )
            .addAction(
                R.drawable.ic_menu_info_details,
                "Open App",
                createOpenAppPendingIntent()
            )
            .setContentIntent(createTriggerPendingIntent())
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

    private fun createScreenshotPendingIntent(): PendingIntent {
        val intent = Intent(this, PlaybackMonitorService::class.java).apply {
            action = SCREENSHOT_ACTION
        }
        return PendingIntent.getService(
            this,
            SCREENSHOT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createLockScreenPendingIntent(): PendingIntent {
        val intent = Intent(this, PlaybackMonitorService::class.java).apply {
            action = LOCK_SCREEN_ACTION
        }
        return PendingIntent.getService(
            this,
            LOCK_SCREEN_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createPowerMenuPendingIntent(): PendingIntent {
        val intent = Intent(this, PlaybackMonitorService::class.java).apply {
            action = POWER_MENU_ACTION
        }
        return PendingIntent.getService(
            this,
            POWER_MENU_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createOpenAppPendingIntent(): PendingIntent {
        val intent = Intent(this, PlaybackMonitorService::class.java).apply {
            action = OPEN_APP_ACTION
        }
        return PendingIntent.getService(
            this,
            OPEN_APP_REQUEST_CODE,
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
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(
                R.drawable.ic_menu_music,
                "Volume",
                createTriggerPendingIntent()
            )
            .addAction(
                R.drawable.ic_menu_camera,
                "Screenshot",
                createScreenshotPendingIntent()
            )
            .addAction(
                R.drawable.ic_menu_power,
                "Lock",
                createLockScreenPendingIntent()
            )
            .addAction(
                R.drawable.ic_menu_power,
                "Power Menu",
                createPowerMenuPendingIntent()
            )
            .addAction(
                R.drawable.ic_menu_info_details,
                "Open App",
                createOpenAppPendingIntent()
            )
            .setContentIntent(createTriggerPendingIntent())
            .build()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "PlaybackMonitorService"
        private const val CHANNEL_ID = "fixvol_monitoring_channel"
        private const val NOTIFICATION_ID = 1001

        const val TRIGGER_VOLUME_ACTION = "com.fixvol.app.ACTION_TRIGGER_VOLUME"
        const val SCREENSHOT_ACTION = "com.fixvol.app.ACTION_SCREENSHOT"
        const val LOCK_SCREEN_ACTION = "com.fixvol.app.ACTION_LOCK_SCREEN"
        const val POWER_MENU_ACTION = "com.fixvol.app.ACTION_POWER_MENU"
        const val OPEN_APP_ACTION = "com.fixvol.app.ACTION_OPEN_APP"

        private const val SCREENSHOT_REQUEST_CODE = 2
        private const val LOCK_SCREEN_REQUEST_CODE = 3
        private const val POWER_MENU_REQUEST_CODE = 4
        private const val OPEN_APP_REQUEST_CODE = 5

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
