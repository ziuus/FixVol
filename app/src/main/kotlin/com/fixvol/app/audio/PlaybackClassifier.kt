package com.fixvol.app.audio

import android.media.AudioAttributes

object PlaybackClassifier {

    fun classify(usage: Int?, contentType: Int?): AudioCategory {
        if (usage == null) {
            return classifyByContentType(contentType)
        }

        return when (usage) {
            AudioAttributes.USAGE_MEDIA,
            AudioAttributes.USAGE_GAME,
            AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE -> AudioCategory.MEDIA

            AudioAttributes.USAGE_ALARM -> AudioCategory.ALARM

            AudioAttributes.USAGE_NOTIFICATION_RINGTONE -> AudioCategory.RINGTONE

            AudioAttributes.USAGE_NOTIFICATION,
            AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_REQUEST,
            AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT,
            AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_DELAYED,
            AudioAttributes.USAGE_NOTIFICATION_EVENT -> AudioCategory.NOTIFICATION

            AudioAttributes.USAGE_VOICE_COMMUNICATION,
            AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING -> AudioCategory.VOICE_CALL

            AudioAttributes.USAGE_ASSISTANCE_SONIFICATION -> AudioCategory.SYSTEM

            AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY -> AudioCategory.ACCESSIBILITY

            AudioAttributes.USAGE_UNKNOWN -> classifyByContentType(contentType)

            else -> AudioCategory.UNKNOWN
        }
    }

    private fun classifyByContentType(contentType: Int?): AudioCategory {
        return when (contentType) {
            AudioAttributes.CONTENT_TYPE_MUSIC,
            AudioAttributes.CONTENT_TYPE_MOVIE -> AudioCategory.MEDIA

            AudioAttributes.CONTENT_TYPE_SONIFICATION -> AudioCategory.SYSTEM
            AudioAttributes.CONTENT_TYPE_SPEECH -> AudioCategory.VOICE_CALL
            else -> AudioCategory.UNKNOWN
        }
    }
}
