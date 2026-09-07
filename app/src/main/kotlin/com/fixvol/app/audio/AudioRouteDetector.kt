package com.fixvol.app.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

class AudioRouteDetector(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    fun getCurrentRoute(): AudioRoute {
        val manager = audioManager ?: return AudioRoute.UNKNOWN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = try {
                manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            } catch (e: Exception) {
                emptyArray<AudioDeviceInfo>()
            }

            for (device in devices) {
                when (device.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                    AudioDeviceInfo.TYPE_HEARING_AID -> return AudioRoute.BLUETOOTH

                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> return AudioRoute.WIRED_HEADSET

                    AudioDeviceInfo.TYPE_USB_DEVICE,
                    AudioDeviceInfo.TYPE_USB_HEADSET,
                    AudioDeviceInfo.TYPE_USB_ACCESSORY -> return AudioRoute.USB

                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE -> return AudioRoute.SPEAKER
                }
            }
        }

        return AudioRoute.UNKNOWN
    }
}
