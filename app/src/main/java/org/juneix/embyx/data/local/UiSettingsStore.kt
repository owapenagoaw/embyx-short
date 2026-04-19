package com.lalakiop.embyx.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.lalakiop.embyx.core.model.PlaybackQualityPresets

private val Context.uiSettingsDataStore by preferencesDataStore(name = "embyx_ui_settings")

enum class ThemeMode(val value: String) {
    LIGHT("light"),
    DARK("dark"),
    SYSTEM("system");

    companion object {
        fun fromValue(raw: String?): ThemeMode {
            return entries.firstOrNull { it.value == raw } ?: SYSTEM
        }
    }
}

data class UiSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val allowScreenOffPlayback: Boolean = false,
    val randomModeEnabled: Boolean = false,
    val preferredPlaybackPresetId: String = PlaybackQualityPresets.ORIGINAL.id
)

class UiSettingsStore(private val context: Context) {

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val allowScreenOffPlayback = booleanPreferencesKey("allow_screen_off_playback")
        val randomModeEnabled = booleanPreferencesKey("random_mode_enabled")
        val preferredPlaybackPresetId = stringPreferencesKey("preferred_playback_preset_id")
    }

    val settingsFlow: Flow<UiSettings> = context.uiSettingsDataStore.data.map { prefs: Preferences ->
        UiSettings(
            themeMode = ThemeMode.fromValue(prefs[Keys.themeMode]),
            allowScreenOffPlayback = prefs[Keys.allowScreenOffPlayback] ?: false,
            randomModeEnabled = prefs[Keys.randomModeEnabled] ?: false,
            preferredPlaybackPresetId = prefs[Keys.preferredPlaybackPresetId] ?: PlaybackQualityPresets.ORIGINAL.id
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.uiSettingsDataStore.edit { prefs ->
            prefs[Keys.themeMode] = mode.value
        }
    }

    suspend fun setAllowScreenOffPlayback(enabled: Boolean) {
        context.uiSettingsDataStore.edit { prefs ->
            prefs[Keys.allowScreenOffPlayback] = enabled
        }
    }

    suspend fun setRandomModeEnabled(enabled: Boolean) {
        context.uiSettingsDataStore.edit { prefs ->
            prefs[Keys.randomModeEnabled] = enabled
        }
    }

    suspend fun setPreferredPlaybackPresetId(presetId: String) {
        context.uiSettingsDataStore.edit { prefs ->
            prefs[Keys.preferredPlaybackPresetId] = PlaybackQualityPresets.findById(presetId).id
        }
    }
}
