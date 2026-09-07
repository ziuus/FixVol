package com.fixvol.app.service

import android.app.ActivityManager
import android.content.Context

class MonitoringController(private val context: Context) {

    fun setMonitoringEnabled(enabled: Boolean) {
        if (enabled) {
            PlaybackMonitorService.startService(context)
        } else {
            PlaybackMonitorService.stopService(context)
        }
    }

    fun isServiceRunning(): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return false
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (PlaybackMonitorService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
