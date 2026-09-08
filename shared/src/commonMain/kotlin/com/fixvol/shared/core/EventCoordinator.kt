package com.fixvol.shared.core

import com.fixvol.shared.audio.AudioCategory
import com.fixvol.shared.audio.PlaybackEvent
import com.fixvol.shared.rules.GlobalRules
import com.fixvol.shared.rules.AppRule
import com.fixvol.shared.rules.RuleDecision
import com.fixvol.shared.rules.RuleEngine
import com.fixvol.shared.utils.currentTimeMillis

class EventCoordinator(
    private val nativeVolumeController: NativeVolumeController,
    var cooldownMs: Long = DEFAULT_COOLDOWN_MS,
    var triggerOnlyOnFirstPlay: Boolean = true
) {

    private var lastTriggeredTimestamp: Long = 0L
    private var lastTriggeredKey: String? = null
    private val activeSessions = mutableMapOf<String, Long>()

    // Debug statistics tracking
    var totalEventsCount: Long = 0
        private set
    var triggeredCount: Long = 0
        private set
    var ignoredCount: Long = 0
        private set
    var lastEvent: PlaybackEvent? = null
        private set
    var lastActionDescription: String = "None"
        private set

    fun processEvent(
        event: PlaybackEvent,
        globalRules: GlobalRules,
        appRules: Map<String, AppRule>
    ): RuleDecision {
        totalEventsCount++
        lastEvent = event

        // Evaluate rule engine first
        val decision = RuleEngine.evaluate(event, globalRules, appRules)

        if (decision == RuleDecision.IGNORE) {
            ignoredCount++
            lastActionDescription = "Ignored by rule engine (${event.category})"
            return RuleDecision.IGNORE
        }

        val eventKey = "${event.packageName ?: event.uid ?: "unknown"}:${event.category}"
        val now = currentTimeMillis()

        // Active Session Check ("First Play Only" feature)
        val lastSessionTime = activeSessions[eventKey]
        val isOngoingSession = lastSessionTime != null && (now - lastSessionTime < SESSION_TIMEOUT_MS)
        activeSessions[eventKey] = now

        if (triggerOnlyOnFirstPlay && isOngoingSession) {
            ignoredCount++
            lastActionDescription = "Suppressed continuous playback ($eventKey)"
            return RuleDecision.IGNORE
        }

        // Debouncing / Cooldown check
        if (now - lastTriggeredTimestamp < cooldownMs && eventKey == lastTriggeredKey) {
            ignoredCount++
            lastActionDescription = "Ignored duplicate within cooldown (${now - lastTriggeredTimestamp}ms)"
            return RuleDecision.IGNORE
        }

        // Global cooldown check regardless of key to prevent UI flickering
        if (now - lastTriggeredTimestamp < MIN_GLOBAL_COOLDOWN_MS) {
            ignoredCount++
            lastActionDescription = "Ignored global rapid trigger (${now - lastTriggeredTimestamp}ms)"
            return RuleDecision.IGNORE
        }

        // Trigger native volume panel display
        val success = nativeVolumeController.requestVolumeUi(event.category)
        if (success) {
            triggeredCount++
            lastTriggeredTimestamp = now
            lastTriggeredKey = eventKey
            lastActionDescription = "Triggered native volume UI for ${event.packageName ?: "unknown"} (${event.category})"
            return RuleDecision.SHOW
        } else {
            ignoredCount++
            lastActionDescription = "Native volume UI call failed"
            return RuleDecision.IGNORE
        }
    }

    fun resetStats() {
        totalEventsCount = 0
        triggeredCount = 0
        ignoredCount = 0
        lastEvent = null
        lastActionDescription = "Stats reset"
    }

    companion object {
        const val DEFAULT_COOLDOWN_MS = 2500L
        private const val MIN_GLOBAL_COOLDOWN_MS = 1000L
        private const val SESSION_TIMEOUT_MS = 4000L

        fun getPriority(category: AudioCategory): Int {
            return when (category) {
                AudioCategory.VOICE_CALL -> 6
                AudioCategory.ALARM -> 5
                AudioCategory.RINGTONE -> 4
                AudioCategory.MEDIA -> 3
                AudioCategory.NOTIFICATION -> 2
                AudioCategory.SYSTEM -> 1
                AudioCategory.ACCESSIBILITY -> 0
                AudioCategory.UNKNOWN -> 0
            }
        }
    }
}
