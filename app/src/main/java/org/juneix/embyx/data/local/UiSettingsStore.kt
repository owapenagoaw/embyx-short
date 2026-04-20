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

data class PlaybackTouchBand(
    val topFraction: Float = 0.2f,
    val heightFraction: Float = 0.24f
) {
    fun normalized(): PlaybackTouchBand {
        val safeHeight = heightFraction.coerceIn(0.08f, 0.9f)
        val safeTop = topFraction.coerceIn(0f, 1f)
        return copy(
            topFraction = safeTop,
            heightFraction = safeHeight
        )
    }
}

data class UiSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val allowScreenOffPlayback: Boolean = false,
    val randomModeEnabled: Boolean = false,
    val preferredPlaybackPresetId: String = PlaybackQualityPresets.ORIGINAL.id,
    val debugOverlayEnabled: Boolean = false,
    val autoPlayHomeEnabled: Boolean = false,
    val autoPlayFavoritesEnabled: Boolean = false,
    val playerAutoHideTopArea: Boolean = true,
    val playerAutoHideRightArea: Boolean = true,
    val playerAutoHideBottomArea: Boolean = true,
    val playerAutoHideDelayMs: Int = 1800,
    val playerSummonBand: PlaybackTouchBand = PlaybackTouchBand(
        topFraction = 0.18f,
        heightFraction = 0.26f
    ),
    val playerPauseBand: PlaybackTouchBand = PlaybackTouchBand(
        topFraction = 0.5f,
        heightFraction = 0.2f
    )
)

class UiSettingsStore(private val context: Context) {

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val allowScreenOffPlayback = booleanPreferencesKey("allow_screen_off_playback")
        val randomModeEnabled = booleanPreferencesKey("random_mode_enabled")
        val preferredPlaybackPresetId = stringPreferencesKey("preferred_playback_preset_id")
        val debugOverlayEnabled = booleanPreferencesKey("debug_overlay_enabled")
        val autoPlayHomeEnabled = booleanPreferencesKey("auto_play_home_enabled")
        val autoPlayFavoritesEnabled = booleanPreferencesKey("auto_play_favorites_enabled")
        val autoPlayNextEnabledLegacy = booleanPreferencesKey("auto_play_next_enabled")
        val playerAutoHideTopArea = booleanPreferencesKey("player_auto_hide_top_area")
        val playerAutoHideRightArea = booleanPreferencesKey("player_auto_hide_right_area")
        val playerAutoHideBottomArea = booleanPreferencesKey("player_auto_hide_bottom_area")
        val playerAutoHideDelayMs = androidx.datastore.preferences.core.intPreferencesKey("player_auto_hide_delay_ms")

        val playerSummonBandTop = androidx.datastore.preferences.core.floatPreferencesKey("player_summon_band_top")
        val playerSummonBandHeight = androidx.datastore.preferences.core.floatPreferencesKey("player_summon_band_height")
        val playerPauseBandTop = androidx.datastore.preferences.core.floatPreferencesKey("player_pause_band_top")
        val playerPauseBandHeight = androidx.datastore.preferences.core.floatPreferencesKey("player_pause_band_height")

        // Legacy square-zone keys kept for migration compatibility.
        val playerSummonZoneLeft = androidx.datastore.preferences.core.floatPreferencesKey("player_summon_zone_left")
        val playerSummonZoneTop = androidx.datastore.preferences.core.floatPreferencesKey("player_summon_zone_top")
        val playerSummonZoneSize = androidx.datastore.preferences.core.floatPreferencesKey("player_summon_zone_size")
        val playerPauseZoneLeft = androidx.datastore.preferences.core.floatPreferencesKey("player_pause_zone_left")
        val playerPauseZoneTop = androidx.datastore.preferences.core.floatPreferencesKey("player_pause_zone_top")
        val playerPauseZoneSize = androidx.datastore.preferences.core.floatPreferencesKey("player_pause_zone_size")
    }

    val settingsFlow: Flow<UiSettings> = context.uiSettingsDataStore.data.map { prefs: Preferences ->
        val legacyAutoPlay = prefs[Keys.autoPlayNextEnabledLegacy] ?: false

        val legacySummonTop = prefs[Keys.playerSummonZoneTop] ?: 0.18f
        val legacySummonHeight = prefs[Keys.playerSummonZoneSize] ?: 0.26f
        val legacyPauseTop = prefs[Keys.playerPauseZoneTop] ?: 0.5f
        val legacyPauseHeight = prefs[Keys.playerPauseZoneSize] ?: 0.2f

        val summonBand = PlaybackTouchBand(
            topFraction = prefs[Keys.playerSummonBandTop] ?: legacySummonTop,
            heightFraction = prefs[Keys.playerSummonBandHeight] ?: legacySummonHeight
        ).normalized()
        val pauseBand = PlaybackTouchBand(
            topFraction = prefs[Keys.playerPauseBandTop] ?: legacyPauseTop,
            heightFraction = prefs[Keys.playerPauseBandHeight] ?: legacyPauseHeight
        ).normalized()

        UiSettings(
            themeMode = ThemeMode.fromValue(prefs[Keys.themeMode]),
            allowScreenOffPlayback = prefs[Keys.allowScreenOffPlayback] ?: false,
            randomModeEnabled = prefs[Keys.randomModeEnabled] ?: false,
            preferredPlaybackPresetId = prefs[Keys.preferredPlaybackPresetId] ?: PlaybackQualityPresets.ORIGINAL.id,
            debugOverlayEnabled = prefs[Keys.debugOverlayEnabled] ?: false,
            autoPlayHomeEnabled = prefs[Keys.autoPlayHomeEnabled] ?: legacyAutoPlay,
            autoPlayFavoritesEnabled = prefs[Keys.autoPlayFavoritesEnabled] ?: legacyAutoPlay,
            playerAutoHideTopArea = prefs[Keys.playerAutoHideTopArea] ?: true,
            playerAutoHideRightArea = prefs[Keys.playerAutoHideRightArea] ?: true,
            playerAutoHideBottomArea = prefs[Keys.playerAutoHideBottomArea] ?: true,
            playerAutoHideDelayMs = (prefs[Keys.playerAutoHideDelayMs] ?: 1800).coerceIn(500, 8000),
            playerSummonBand = summonBand,
            playerPauseBand = pauseBand
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

    suspend fun setDebugOverlayEnabled(enabled: Boolean) {
        context.uiSettingsDataStore.edit { prefs ->
            prefs[Keys.debugOverlayEnabled] = enabled
        }
    }

    suspend fun setAutoPlayHomeEnabled(enabled: Boolean) {
        context.uiSettingsDataStore.edit { prefs ->
            prefs[Keys.autoPlayHomeEnabled] = enabled
        }
    }

    suspend fun setAutoPlayFavoritesEnabled(enabled: Boolean) {
        context.uiSettingsDataStore.edit { prefs ->
            prefs[Keys.autoPlayFavoritesEnabled] = enabled
        }
    }

    suspend fun setPlayerControlAreaAutoHide(
        topArea: Boolean,
        rightArea: Boolean,
        bottomArea: Boolean
    ) {
        context.uiSettingsDataStore.edit { prefs ->
            prefs[Keys.playerAutoHideTopArea] = topArea
            prefs[Keys.playerAutoHideRightArea] = rightArea
            prefs[Keys.playerAutoHideBottomArea] = bottomArea
        }
    }

    suspend fun setPlayerAutoHideDelayMs(delayMs: Int) {
        context.uiSettingsDataStore.edit { prefs ->
            prefs[Keys.playerAutoHideDelayMs] = delayMs.coerceIn(500, 8000)
        }
    }

    suspend fun setPlayerSummonBand(band: PlaybackTouchBand) {
        val normalized = band.normalized()
        context.uiSettingsDataStore.edit { prefs ->
            prefs[Keys.playerSummonBandTop] = normalized.topFraction
            prefs[Keys.playerSummonBandHeight] = normalized.heightFraction
        }
    }

    suspend fun setPlayerPauseBand(band: PlaybackTouchBand) {
        val normalized = band.normalized()
        context.uiSettingsDataStore.edit { prefs ->
            prefs[Keys.playerPauseBandTop] = normalized.topFraction
            prefs[Keys.playerPauseBandHeight] = normalized.heightFraction
        }
    }
}
