package com.fixvol.app.core

import com.fixvol.app.audio.AudioCategory
import com.fixvol.app.audio.PlaybackEvent
import com.fixvol.app.audio.PlaybackState
import org.junit.Assert.assertEquals
import org.junit.Test

class EventCoordinatorTest {

    @Test
    fun getPriority_verifiesCategoryPrioritization() {
        assertEquals(6, EventCoordinator.getPriority(AudioCategory.VOICE_CALL))
        assertEquals(5, EventCoordinator.getPriority(AudioCategory.ALARM))
        assertEquals(4, EventCoordinator.getPriority(AudioCategory.RINGTONE))
        assertEquals(3, EventCoordinator.getPriority(AudioCategory.MEDIA))
        assertEquals(2, EventCoordinator.getPriority(AudioCategory.NOTIFICATION))
        assertEquals(1, EventCoordinator.getPriority(AudioCategory.SYSTEM))
    }
}
