package com.fixvol.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            runBlocking {
                com.fixvol.app.data.SettingsRepository(context).settingsFlow.first().let { settings ->
                    if (settings.enabled && settings.floatingControlsEnabled) {
                        FloatingControlsService.startService(context)
                    }
                }
            }
        }
    }
}
