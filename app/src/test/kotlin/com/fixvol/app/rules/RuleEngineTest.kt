package com.fixvol.app.rules

import com.fixvol.app.audio.AudioCategory
import com.fixvol.app.audio.PlaybackEvent
import com.fixvol.app.audio.PlaybackState
import org.junit.Assert.assertEquals
import org.junit.Test

class RuleEngineTest {

    private fun createEvent(packageName: String? = "com.spotify.music", category: AudioCategory = AudioCategory.MEDIA): PlaybackEvent {
        return PlaybackEvent(
            id = "test-id",
            packageName = packageName,
            uid = 10001,
            usage = 1,
            contentType = 2,
            category = category,
            state = PlaybackState.STARTED
        )
    }

    @Test
    fun evaluate_globalMediaOn_spotifyGlobal_returnsShow() {
        val globalRules = GlobalRules(enabled = true, categoryRules = mapOf(AudioCategory.MEDIA to true))
        val appRules = mapOf("com.spotify.music" to AppRule("com.spotify.music", RuleMode.FOLLOW_GLOBAL))

        val decision = RuleEngine.evaluate(createEvent(), globalRules, appRules)
        assertEquals(RuleDecision.SHOW, decision)
    }

    @Test
    fun evaluate_globalMediaOn_spotifyNeverShow_returnsIgnore() {
        val globalRules = GlobalRules(enabled = true, categoryRules = mapOf(AudioCategory.MEDIA to true))
        val appRules = mapOf("com.spotify.music" to AppRule("com.spotify.music", RuleMode.NEVER_SHOW))

        val decision = RuleEngine.evaluate(createEvent(), globalRules, appRules)
        assertEquals(RuleDecision.IGNORE, decision)
    }

    @Test
    fun evaluate_globalMediaOff_spotifyAlwaysShow_returnsShow() {
        val globalRules = GlobalRules(enabled = true, categoryRules = mapOf(AudioCategory.MEDIA to false))
        val appRules = mapOf("com.spotify.music" to AppRule("com.spotify.music", RuleMode.ALWAYS_SHOW))

        val decision = RuleEngine.evaluate(createEvent(), globalRules, appRules)
        assertEquals(RuleDecision.SHOW, decision)
    }

    @Test
    fun evaluate_globalDisabled_returnsIgnore() {
        val globalRules = GlobalRules(enabled = false, categoryRules = mapOf(AudioCategory.MEDIA to true))
        val appRules = mapOf("com.spotify.music" to AppRule("com.spotify.music", RuleMode.ALWAYS_SHOW))

        val decision = RuleEngine.evaluate(createEvent(), globalRules, appRules)
        assertEquals(RuleDecision.IGNORE, decision)
    }

    @Test
    fun evaluate_unknownPackage_usesGlobalCategoryRule() {
        val globalRules = GlobalRules(enabled = true, categoryRules = mapOf(AudioCategory.MEDIA to true))
        val appRules = emptyMap<String, AppRule>()

        val decision = RuleEngine.evaluate(createEvent(packageName = "com.unknown.app"), globalRules, appRules)
        assertEquals(RuleDecision.SHOW, decision)
    }

    @Test
    fun evaluate_explicitCategoryOverride_overridesAppGlobalMode() {
        val globalRules = GlobalRules(enabled = true, categoryRules = mapOf(AudioCategory.MEDIA to true))
        val appRule = AppRule(
            packageName = "com.spotify.music",
            mode = RuleMode.ALWAYS_SHOW,
            categoryOverrides = mapOf(AudioCategory.MEDIA to RuleMode.NEVER_SHOW)
        )
        val appRules = mapOf("com.spotify.music" to appRule)

        val decision = RuleEngine.evaluate(createEvent(), globalRules, appRules)
        assertEquals(RuleDecision.IGNORE, decision)
    }
}
