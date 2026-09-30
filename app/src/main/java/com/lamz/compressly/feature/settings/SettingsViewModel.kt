package com.lamz.compressly.feature.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lamz.compressly.core.storage.PreferencesManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)

    val themeMode = preferencesManager.themeMode.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "SYSTEM"
    )

    val defaultPreset = preferencesManager.defaultImagePreset.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "BALANCED"
    )

    val defaultFormat = preferencesManager.defaultImageFormat.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "AUTO"
    )

    val stripExif = preferencesManager.stripExif.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        true
    )

    val defaultPdfStrategy = preferencesManager.defaultPdfStrategy.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "SMART"
    )

    fun setThemeMode(mode: String) {
        viewModelScope.launch { preferencesManager.setThemeMode(mode) }
    }

    fun setDefaultPreset(preset: String) {
        viewModelScope.launch { preferencesManager.setDefaultImagePreset(preset) }
    }

    fun setDefaultFormat(format: String) {
        viewModelScope.launch { preferencesManager.setDefaultImageFormat(format) }
    }

    fun setStripExif(strip: Boolean) {
        viewModelScope.launch { preferencesManager.setStripExif(strip) }
    }

    fun setDefaultPdfStrategy(strategy: String) {
        viewModelScope.launch { preferencesManager.setDefaultPdfStrategy(strategy) }
    }
}
