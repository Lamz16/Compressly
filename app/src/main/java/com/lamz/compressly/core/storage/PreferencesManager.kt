package com.lamz.compressly.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "compressly_settings")

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode") // "SYSTEM", "LIGHT", "DARK"
        val KEY_DEFAULT_IMAGE_PRESET = stringPreferencesKey("default_image_preset") // "MAXIMUM", "HIGH", "BALANCED", "SMALL"
        val KEY_DEFAULT_IMAGE_FORMAT = stringPreferencesKey("default_image_format") // "AUTO", "JPEG", "WEBP", "PNG"
        val KEY_STRIP_EXIF = booleanPreferencesKey("strip_exif")
        val KEY_DEFAULT_PDF_STRATEGY = stringPreferencesKey("default_pdf_strategy") // "SMART", "NATIVE", "SCANNED"
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE] ?: "SYSTEM"
    }

    val defaultImagePreset: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_IMAGE_PRESET] ?: "BALANCED"
    }

    val defaultImageFormat: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_IMAGE_FORMAT] ?: "AUTO"
    }

    val stripExif: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_STRIP_EXIF] ?: true
    }

    val defaultPdfStrategy: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_PDF_STRATEGY] ?: "SMART"
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode
        }
    }

    suspend fun setDefaultImagePreset(preset: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEFAULT_IMAGE_PRESET] = preset
        }
    }

    suspend fun setDefaultImageFormat(format: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEFAULT_IMAGE_FORMAT] = format
        }
    }

    suspend fun setStripExif(strip: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_STRIP_EXIF] = strip
        }
    }

    suspend fun setDefaultPdfStrategy(strategy: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEFAULT_PDF_STRATEGY] = strategy
        }
    }
}
