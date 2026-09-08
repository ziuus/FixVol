package com.fixvol.shared.rules

import com.fixvol.shared.audio.AudioCategory

data class GlobalRules(
    val enabled: Boolean = true,
    val categoryRules: Map<AudioCategory, Boolean> = defaultCategoryRules()
) {
    companion object {
        fun defaultCategoryRules(): Map<AudioCategory, Boolean> {
            return mapOf(
                AudioCategory.MEDIA to true,
                AudioCategory.ALARM to true,
                AudioCategory.RINGTONE to true,
                AudioCategory.NOTIFICATION to false,
                AudioCategory.VOICE_CALL to true,
                AudioCategory.SYSTEM to false,
                AudioCategory.ACCESSIBILITY to false,
                AudioCategory.UNKNOWN to true
            )
        }
    }
}

data class AppRule(
    val packageName: String,
    val mode: RuleMode = RuleMode.FOLLOW_GLOBAL,
    val categoryOverrides: Map<AudioCategory, RuleMode> = emptyMap()
)
