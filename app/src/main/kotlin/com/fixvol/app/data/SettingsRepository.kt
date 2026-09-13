package com.fixvol.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fixvol.app.audio.AudioCategory
import com.fixvol.app.rules.AppRule
import com.fixvol.app.rules.GlobalRules
import com.fixvol.app.rules.RuleMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fixvol_settings")

data class FixVolSettings(
    val enabled: Boolean = true,
    val floatingControlsEnabled: Boolean = true,
    val globalRules: GlobalRules = GlobalRules(),
    val appRules: Map<String, AppRule> = emptyMap(),
    val cooldownMs: Long = 2500L,
    val triggerOnlyOnFirstPlay: Boolean = true,
    val debugLogging: Boolean = false
)

class SettingsRepository(private val context: Context) {

    private val KEY_ENABLED = booleanPreferencesKey("enabled")
    private val KEY_FLOATING_CONTROLS_ENABLED = booleanPreferencesKey("floating_controls_enabled")
    private val KEY_COOLDOWN_MS = longPreferencesKey("cooldown_ms")
    private val KEY_FIRST_PLAY_ONLY = booleanPreferencesKey("first_play_only")
    private val KEY_DEBUG_LOGGING = booleanPreferencesKey("debug_logging")

    // Category keys
    private val KEY_CAT_MEDIA = booleanPreferencesKey("cat_media")
    private val KEY_CAT_ALARM = booleanPreferencesKey("cat_alarm")
    private val KEY_CAT_RINGTONE = booleanPreferencesKey("cat_ringtone")
    private val KEY_CAT_NOTIFICATION = booleanPreferencesKey("cat_notification")
    private val KEY_CAT_VOICE_CALL = booleanPreferencesKey("cat_voice_call")
    private val KEY_CAT_SYSTEM = booleanPreferencesKey("cat_system")
    private val KEY_CAT_ACCESSIBILITY = booleanPreferencesKey("cat_accessibility")
    private val KEY_CAT_UNKNOWN = booleanPreferencesKey("cat_unknown")

    private val KEY_APP_RULES_SERIALIZED = stringPreferencesKey("app_rules_json")

    val settingsFlow: Flow<FixVolSettings> = context.dataStore.data.map { prefs ->
        val enabled = prefs[KEY_ENABLED] ?: true
        val floatingControlsEnabled = prefs[KEY_FLOATING_CONTROLS_ENABLED] ?: true
        val cooldownMs = prefs[KEY_COOLDOWN_MS] ?: 2500L
        val triggerOnlyOnFirstPlay = prefs[KEY_FIRST_PLAY_ONLY] ?: true
        val debugLogging = prefs[KEY_DEBUG_LOGGING] ?: false

        val defaultCats = GlobalRules.defaultCategoryRules()
        val categoryRules = mapOf(
            AudioCategory.MEDIA to (prefs[KEY_CAT_MEDIA] ?: defaultCats[AudioCategory.MEDIA]!!),
            AudioCategory.ALARM to (prefs[KEY_CAT_ALARM] ?: defaultCats[AudioCategory.ALARM]!!),
            AudioCategory.RINGTONE to (prefs[KEY_CAT_RINGTONE] ?: defaultCats[AudioCategory.RINGTONE]!!),
            AudioCategory.NOTIFICATION to (prefs[KEY_CAT_NOTIFICATION] ?: defaultCats[AudioCategory.NOTIFICATION]!!),
            AudioCategory.VOICE_CALL to (prefs[KEY_CAT_VOICE_CALL] ?: defaultCats[AudioCategory.VOICE_CALL]!!),
            AudioCategory.SYSTEM to (prefs[KEY_CAT_SYSTEM] ?: defaultCats[AudioCategory.SYSTEM]!!),
            AudioCategory.ACCESSIBILITY to (prefs[KEY_CAT_ACCESSIBILITY] ?: defaultCats[AudioCategory.ACCESSIBILITY]!!),
            AudioCategory.UNKNOWN to (prefs[KEY_CAT_UNKNOWN] ?: defaultCats[AudioCategory.UNKNOWN]!!)
        )

        val appRulesJson = prefs[KEY_APP_RULES_SERIALIZED] ?: ""
        val appRules = deserializeAppRules(appRulesJson)

        FixVolSettings(
            enabled = enabled,
            floatingControlsEnabled = floatingControlsEnabled,
            globalRules = GlobalRules(enabled = enabled, categoryRules = categoryRules),
            appRules = appRules,
            cooldownMs = cooldownMs,
            triggerOnlyOnFirstPlay = triggerOnlyOnFirstPlay,
            debugLogging = debugLogging
        )
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ENABLED] = enabled
        }
    }

    suspend fun setFloatingControlsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FLOATING_CONTROLS_ENABLED] = enabled
        }
    }

    suspend fun setCategoryEnabled(category: AudioCategory, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            when (category) {
                AudioCategory.MEDIA -> prefs[KEY_CAT_MEDIA] = enabled
                AudioCategory.ALARM -> prefs[KEY_CAT_ALARM] = enabled
                AudioCategory.RINGTONE -> prefs[KEY_CAT_RINGTONE] = enabled
                AudioCategory.NOTIFICATION -> prefs[KEY_CAT_NOTIFICATION] = enabled
                AudioCategory.VOICE_CALL -> prefs[KEY_CAT_VOICE_CALL] = enabled
                AudioCategory.SYSTEM -> prefs[KEY_CAT_SYSTEM] = enabled
                AudioCategory.ACCESSIBILITY -> prefs[KEY_CAT_ACCESSIBILITY] = enabled
                AudioCategory.UNKNOWN -> prefs[KEY_CAT_UNKNOWN] = enabled
            }
        }
    }

    suspend fun setAppRule(appRule: AppRule) {
        context.dataStore.edit { prefs ->
            val currentRules = deserializeAppRules(prefs[KEY_APP_RULES_SERIALIZED] ?: "").toMutableMap()
            currentRules[appRule.packageName] = appRule
            prefs[KEY_APP_RULES_SERIALIZED] = serializeAppRules(currentRules)
        }
    }

    suspend fun setCooldownMs(cooldownMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_COOLDOWN_MS] = cooldownMs
        }
    }

    suspend fun setTriggerOnlyOnFirstPlay(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FIRST_PLAY_ONLY] = enabled
        }
    }

    suspend fun setDebugLogging(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEBUG_LOGGING] = enabled
        }
    }

    private fun serializeAppRules(rules: Map<String, AppRule>): String {
        val sb = StringBuilder()
        rules.values.forEach { rule ->
            sb.append("${rule.packageName}:${rule.mode.name};")
        }
        return sb.toString()
    }

    private fun deserializeAppRules(data: String): Map<String, AppRule> {
        if (data.isBlank()) return emptyMap()
        val result = mutableMapOf<String, AppRule>()
        val entries = data.split(";")
        for (entry in entries) {
            if (entry.isBlank()) continue
            val parts = entry.split(":")
            if (parts.size == 2) {
                val pkg = parts[0]
                val mode = try { RuleMode.valueOf(parts[1]) } catch (e: Exception) { RuleMode.FOLLOW_GLOBAL }
                result[pkg] = AppRule(packageName = pkg, mode = mode)
            }
        }
        return result
    }
}
