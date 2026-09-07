package com.fixvol.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fixvol.app.audio.AudioCategory
import com.fixvol.app.core.NativeVolumeController
import com.fixvol.app.data.AppMetadata
import com.fixvol.app.data.AppResolver
import com.fixvol.app.data.FixVolSettings
import com.fixvol.app.data.SettingsRepository
import com.fixvol.app.rules.AppRule
import com.fixvol.app.rules.RuleMode
import com.fixvol.app.service.MonitoringController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val monitoringController = MonitoringController(application)
    private val appResolver = AppResolver(application)
    private val nativeVolumeController = NativeVolumeController(application)

    val settings: StateFlow<FixVolSettings> = settingsRepository.settingsFlow
        .toStateFlow(FixVolSettings())

    private val _installedApps = MutableStateFlow<List<AppMetadata>>(emptyList())
    val installedApps: StateFlow<List<AppMetadata>> = _installedApps.asStateFlow()

    private val _testResult = MutableStateFlow<String?>(null)
    val testResult: StateFlow<String?> = _testResult.asStateFlow()

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            _installedApps.value = appResolver.getInstalledMediaApps()
        }
    }

    fun toggleMasterEnable(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setEnabled(enabled)
            monitoringController.setMonitoringEnabled(enabled)
        }
    }

    fun toggleCategory(category: AudioCategory, enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setCategoryEnabled(category, enabled)
        }
    }

    fun setAppRuleMode(packageName: String, mode: RuleMode) {
        viewModelScope.launch {
            settingsRepository.setAppRule(AppRule(packageName = packageName, mode = mode))
        }
    }

    fun setCooldownMs(cooldownMs: Long) {
        viewModelScope.launch {
            settingsRepository.setCooldownMs(cooldownMs)
        }
    }

    fun setDebugLogging(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDebugLogging(enabled)
        }
    }

    fun testVolumePanel() {
        val result = nativeVolumeController.testNativeVolumePanel(AudioCategory.MEDIA)
        val message = if (result.success) {
            "✓ Volume panel displayed! Volume verified unchanged (${result.volumeBefore} -> ${result.volumeAfter})"
        } else {
            "✗ Test failed or volume changed (${result.volumeBefore} vs ${result.volumeAfter})"
        }
        _testResult.value = message
    }

    fun clearTestResult() {
        _testResult.value = null
    }

    private fun <T> kotlinx.coroutines.flow.Flow<T>.toStateFlow(initial: T): StateFlow<T> {
        val state = MutableStateFlow(initial)
        viewModelScope.launch {
            collect { state.value = it }
        }
        return state.asStateFlow()
    }
}
