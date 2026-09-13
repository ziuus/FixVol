package com.fixvol.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.fixvol.app.R
import com.fixvol.app.audio.AudioCategory
import com.fixvol.app.audio.PlaybackEvent
import com.fixvol.app.audio.PlaybackState
import com.fixvol.app.audio.PlaybackMonitor
import com.fixvol.app.audio.AudioRouteDetector
import com.fixvol.app.core.EventCoordinator
import com.fixvol.app.core.NativeVolumeController
import com.fixvol.app.data.FixVolSettings
import com.fixvol.app.data.SettingsRepository
import com.fixvol.app.ui.ScreenshotActivity
import com.fixvol.app.ui.MainActivity
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.onEach

class FloatingControlsService : LifecycleService() {

    private lateinit var playbackMonitor: PlaybackMonitor
    private lateinit var nativeVolumeController: NativeVolumeController
    private lateinit var eventCoordinator: EventCoordinator
    private lateinit var settingsRepository: SettingsRepository

    private var currentSettings: FixVolSettings = FixVolSettings()
    private var currentVolumeLevel: Int = 0
    private var currentMaxVolume: Int = 0
    private var overlayView: View? = null
    private var windowManager: WindowManager? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FloatingControlsService onCreate")

        nativeVolumeController = NativeVolumeController(this)
        eventCoordinator = EventCoordinator(nativeVolumeController)
        playbackMonitor = PlaybackMonitor(this, AudioRouteDetector(this))
        settingsRepository = SettingsRepository(this)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        createNotificationChannel()
        refreshCurrentVolume()

        // Show persistent notification so OS doesn't kill us
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Observe settings
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

        // Inflate and show the floating overlay once service is foregrounded
        showFloatingOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "FloatingControlsService onDestroy")
        hideFloatingOverlay()
        playbackMonitor.stopMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            TRIGGER_VOLUME_ACTION -> {
                Log.d(TAG, "Floating control: volume panel triggered")
                try {
                    eventCoordinator.processEvent(
                        event = PlaybackEvent(
                            id = "floating_volume_tap",
                            packageName = "floating_volume_tap",
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
                    Log.e(TAG, "Failed to trigger volume from floating control", e)
                }
            }
            SCREENSHOT_ACTION -> {
                Log.d(TAG, "Floating control: screenshot triggered")
                try {
                    val intent = Intent(this, ScreenshotActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to launch screenshot from floating control", e)
                }
            }
            LOCK_SCREEN_ACTION -> {
                Log.d(TAG, "Floating control: lock screen triggered")
                lockScreen()
            }
            POWER_MENU_ACTION -> {
                Log.d(TAG, "Floating control: power menu triggered")
                openPowerMenu()
            }
            OPEN_APP_ACTION -> {
                Log.d(TAG, "Floating control: open app triggered")
                openApp()
            }
            TOGGLE_OVERLAY_VISIBILITY -> {
                Log.d(TAG, "Floating control: toggle overlay visibility")
                if (overlayView != null) {
                    hideFloatingOverlay()
                } else {
                    showFloatingOverlay()
                }
            }
        }
        return START_STICKY
    }

    // ─── Floating overlay ───────────────────────────────────────────────────

    private fun showFloatingOverlay() {
        if (overlayView != null) return

        val inflater = LayoutInflater.from(this)
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 0
            y = 100 // offset from top
            screenOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LOCKED
        }

        overlayView = inflater.inflate(R.layout.floating_controls, null).apply {
            findViewById<ImageButton>(R.id.btn_volume).setOnClickListener {
                val intent = Intent(this@FloatingControlsService, FloatingControlsService::class.java)
                intent.action = TRIGGER_VOLUME_ACTION
                startService(intent)
            }
            findViewById<ImageButton>(R.id.btn_screenshot).setOnClickListener {
                val intent = Intent(this@FloatingControlsService, FloatingControlsService::class.java)
                intent.action = SCREENSHOT_ACTION
                startService(intent)
            }
            findViewById<ImageButton>(R.id.btn_lock).setOnClickListener {
                val intent = Intent(this@FloatingControlsService, FloatingControlsService::class.java)
                intent.action = LOCK_SCREEN_ACTION
                startService(intent)
            }
            findViewById<ImageButton>(R.id.btn_power_menu).setOnClickListener {
                val intent = Intent(this@FloatingControlsService, FloatingControlsService::class.java)
                intent.action = POWER_MENU_ACTION
                startService(intent)
            }
            findViewById<ImageButton>(R.id.btn_open_app).setOnClickListener {
                val intent = Intent(this@FloatingControlsService, FloatingControlsService::class.java)
                intent.action = OPEN_APP_ACTION
                startService(intent)
            }
            findViewById<ImageButton>(R.id.btn_toggle_visibility).setOnClickListener {
                val intent = Intent(this@FloatingControlsService, FloatingControlsService::class.java)
                intent.action = TOGGLE_OVERLAY_VISIBILITY
                startService(intent)
            }
        }

        try {
            windowManager?.addView(overlayView, layoutParams)
            Log.d(TAG, "Floating overlay shown")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show floating overlay — SYSTEM_ALERT_WINDOW required", e)
            Toast.makeText(this, "Floating controls: permission required (Settings > Apps > FixVol > Display over other apps)", Toast.LENGTH_LONG).show()
            overlayView = null
        }
    }

    private fun hideFloatingOverlay() {
        try {
            overlayView?.let { windowManager?.removeView(it) }
            overlayView = null
            Log.d(TAG, "Floating overlay hidden")
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding floating overlay", e)
        }
    }

    // ─── Lock / Power / App ─────────────────────────────────────────────────

    private fun lockScreen() {
        // PowerManager.goToSleep() added in API 29; called via reflection since minSdk=26
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
                if (powerManager?.isInteractive == true) {
                    val m = PowerManager::class.java.getMethod("goToSleep", Long::class.java)
                    m.invoke(powerManager, SystemClock.uptimeMillis())
                    Log.d(TAG, "Screen locked via PowerManager.goToSleep")
                    return
                }
            } catch (e: Exception) {
                Log.w(TAG, "PowerManager.goToSleep failed: ${e.message}", e)
            }
        }
        Log.w(TAG, "Screen lock: no viable method available on this device/OS")
    }

    private fun openPowerMenu() {
        // No reliable public API exists for third-party apps to trigger the system power menu.
        // Android restricts this for security reasons. Device owners/admins have more options
        // but for a regular app this is not feasible. Logging for diagnostics.
        Log.w(TAG, "Power menu: no reliable API available for third-party apps")
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

    // ─── Notification ───────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "FixVol Floating Controls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps floating controls alive"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, FloatingControlsService::class.java).apply {
            action = TOGGLE_OVERLAY_VISIBILITY
        }
        val pendingIntent = PendingIntent.getService(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FixVol Controls")
            .setContentText("Volume: $currentVolumeLevel / $currentMaxVolume")
            .setSmallIcon(R.drawable.ic_menu_music)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
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
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createTriggerPendingIntent(): PendingIntent {
        val intent = Intent(this, FloatingControlsService::class.java).apply {
            action = TRIGGER_VOLUME_ACTION
        }
        return PendingIntent.getService(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createScreenshotPendingIntent(): PendingIntent {
        val intent = Intent(this, FloatingControlsService::class.java).apply {
            action = SCREENSHOT_ACTION
        }
        return PendingIntent.getService(
            this, SCREENSHOT_REQUEST_CODE, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createLockScreenPendingIntent(): PendingIntent {
        val intent = Intent(this, FloatingControlsService::class.java).apply {
            action = LOCK_SCREEN_ACTION
        }
        return PendingIntent.getService(
            this, LOCK_SCREEN_REQUEST_CODE, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createPowerMenuPendingIntent(): PendingIntent {
        val intent = Intent(this, FloatingControlsService::class.java).apply {
            action = POWER_MENU_ACTION
        }
        return PendingIntent.getService(
            this, POWER_MENU_REQUEST_CODE, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createOpenAppPendingIntent(): PendingIntent {
        val intent = Intent(this, FloatingControlsService::class.java).apply {
            action = OPEN_APP_ACTION
        }
        return PendingIntent.getService(
            this, OPEN_APP_REQUEST_CODE, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun refreshCurrentVolume() {
        val manager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager ?: return
        currentVolumeLevel = manager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        currentMaxVolume = manager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
    }

    companion object {
        private const val TAG = "FloatingControlsService"
        private const val CHANNEL_ID = "fixvol_floating_channel"
        private const val NOTIFICATION_ID = 1002

        const val TRIGGER_VOLUME_ACTION = "com.fixvol.app.FLOAT_ACTION_TRIGGER_VOLUME"
        const val SCREENSHOT_ACTION = "com.fixvol.app.FLOAT_ACTION_SCREENSHOT"
        const val LOCK_SCREEN_ACTION = "com.fixvol.app.FLOAT_ACTION_LOCK_SCREEN"
        const val POWER_MENU_ACTION = "com.fixvol.app.FLOAT_ACTION_POWER_MENU"
        const val OPEN_APP_ACTION = "com.fixvol.app.FLOAT_ACTION_OPEN_APP"
        const val TOGGLE_OVERLAY_VISIBILITY = "com.fixvol.app.FLOAT_ACTION_TOGGLE_VISIBILITY"

        private const val SCREENSHOT_REQUEST_CODE = 201
        private const val LOCK_SCREEN_REQUEST_CODE = 202
        private const val POWER_MENU_REQUEST_CODE = 203
        private const val OPEN_APP_REQUEST_CODE = 204

        fun startService(context: Context) {
            val intent = Intent(context, FloatingControlsService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FloatingControlsService::class.java)
            context.stopService(intent)
        }
    }
}
