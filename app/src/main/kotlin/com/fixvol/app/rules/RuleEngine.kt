package com.fixvol.app.rules

import com.fixvol.app.audio.PlaybackEvent

object RuleEngine {

    fun evaluate(
        event: PlaybackEvent,
        globalRules: GlobalRules,
        appRules: Map<String, AppRule>
    ): RuleDecision {
        if (!globalRules.enabled) {
            return RuleDecision.IGNORE
        }

        val packageName = event.packageName
        val appRule = if (packageName != null) appRules[packageName] else null

        // 1. Explicit app + category rule override
        if (appRule != null && event.category in appRule.categoryOverrides) {
            when (appRule.categoryOverrides[event.category]) {
                RuleMode.ALWAYS_SHOW -> return RuleDecision.SHOW
                RuleMode.NEVER_SHOW -> return RuleDecision.IGNORE
                RuleMode.FOLLOW_GLOBAL -> { /* Fall through to next check */ }
                null -> { /* Fall through */ }
            }
        }

        // 2. Explicit app global rule mode
        if (appRule != null) {
            when (appRule.mode) {
                RuleMode.ALWAYS_SHOW -> return RuleDecision.SHOW
                RuleMode.NEVER_SHOW -> return RuleDecision.IGNORE
                RuleMode.FOLLOW_GLOBAL -> { /* Fall through to next check */ }
            }
        }

        // 3. Global category rule
        val globalCategoryAllowed = globalRules.categoryRules[event.category] ?: false
        if (globalCategoryAllowed) {
            return RuleDecision.SHOW
        }

        // 4. Safe fallback
        return RuleDecision.IGNORE
    }
}
