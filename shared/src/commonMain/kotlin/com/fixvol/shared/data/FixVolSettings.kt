package com.fixvol.shared.data

import com.fixvol.shared.rules.GlobalRules
import com.fixvol.shared.rules.AppRule

data class FixVolSettings(
    val enabled: Boolean = true,
    val globalRules: GlobalRules = GlobalRules(),
    val appRules: Map<String, AppRule> = emptyMap(),
    val cooldownMs: Long = 2500L,
    val triggerOnlyOnFirstPlay: Boolean = true,
    val debugLogging: Boolean = false
)
