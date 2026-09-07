package com.fixvol.app.audio

import android.media.AudioAttributes
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackClassifierTest {

    @Test
    fun classify_mediaUsage_returnsMediaCategory() {
        val category = PlaybackClassifier.classify(AudioAttributes.USAGE_MEDIA, AudioAttributes.CONTENT_TYPE_MUSIC)
        assertEquals(AudioCategory.MEDIA, category)
    }

    @Test
    fun classify_alarmUsage_returnsAlarmCategory() {
        val category = PlaybackClassifier.classify(AudioAttributes.USAGE_ALARM, AudioAttributes.CONTENT_TYPE_UNKNOWN)
        assertEquals(AudioCategory.ALARM, category)
    }

    @Test
    fun classify_ringtoneUsage_returnsRingtoneCategory() {
        val category = PlaybackClassifier.classify(AudioAttributes.USAGE_NOTIFICATION_RINGTONE, AudioAttributes.CONTENT_TYPE_SONIFICATION)
        assertEquals(AudioCategory.RINGTONE, category)
    }

    @Test
    fun classify_notificationUsage_returnsNotificationCategory() {
        val category = PlaybackClassifier.classify(AudioAttributes.USAGE_NOTIFICATION, AudioAttributes.CONTENT_TYPE_SONIFICATION)
        assertEquals(AudioCategory.NOTIFICATION, category)
    }

    @Test
    fun classify_voiceCallUsage_returnsVoiceCallCategory() {
        val category = PlaybackClassifier.classify(AudioAttributes.USAGE_VOICE_COMMUNICATION, AudioAttributes.CONTENT_TYPE_SPEECH)
        assertEquals(AudioCategory.VOICE_CALL, category)
    }

    @Test
    fun classify_sonificationUsage_returnsSystemCategory() {
        val category = PlaybackClassifier.classify(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION, AudioAttributes.CONTENT_TYPE_SONIFICATION)
        assertEquals(AudioCategory.SYSTEM, category)
    }

    @Test
    fun classify_nullUsage_fallbacksToContentType() {
        val category = PlaybackClassifier.classify(null, AudioAttributes.CONTENT_TYPE_MUSIC)
        assertEquals(AudioCategory.MEDIA, category)
    }

    @Test
    fun classify_unknownUsageAndContentType_returnsUnknownCategory() {
        val category = PlaybackClassifier.classify(AudioAttributes.USAGE_UNKNOWN, AudioAttributes.CONTENT_TYPE_UNKNOWN)
        assertEquals(AudioCategory.UNKNOWN, category)
    }
}
