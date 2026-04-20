package com.lalakiop.embyx.data.local

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.lalakiop.embyx.core.model.MediaLibrary
import com.lalakiop.embyx.core.model.MediaLibraryType
import com.lalakiop.embyx.core.model.VideoItem

private val Context.homeCacheDataStore by preferencesDataStore(name = "embyx_home_cache")

data class HomeCache(
    val videos: List<VideoItem> = emptyList(),
    val libraries: List<MediaLibrary> = emptyList()
)

data class SequentialPlaybackState(
    val videos: List<VideoItem> = emptyList(),
    val currentPage: Int = 0,
    val positionMs: Long = 0L,
    val wasPlaying: Boolean = true,
    val updatedAtMs: Long = 0L
)

data class SequentialPlaybackTable(
    val selectedLibraryId: String? = null,
    val selectedLibraryType: MediaLibraryType? = null,
    val states: Map<String, SequentialPlaybackState> = emptyMap()
)

data class PlaybackHistoryEntry(
    val video: VideoItem,
    val playedAtMs: Long,
    val sourceName: String? = null
)

enum class PlaybackHistoryMode {
    RANDOM,
    SEQUENTIAL
}

class HomeCacheStore(private val context: Context) {

    private companion object {
        const val MAX_HISTORY_ITEMS = 80
    }

    private object Keys {
        val videosJson = stringPreferencesKey("videos_json")
        val librariesJson = stringPreferencesKey("libraries_json")
        val favoritesJson = stringPreferencesKey("favorites_json")
        val sequentialTableJson = stringPreferencesKey("sequential_table_json")
        val randomHistoryJson = stringPreferencesKey("random_history_json")
        val sequentialHistoryJson = stringPreferencesKey("sequential_history_json")
    }

    private val gson = Gson()
    private val videosType = object : TypeToken<List<VideoItem>>() {}.type
    private val librariesType = object : TypeToken<List<MediaLibrary>>() {}.type
    private val favoritesType = object : TypeToken<List<VideoItem>>() {}.type
    private val sequentialTableType = object : TypeToken<SequentialPlaybackTable>() {}.type
    private val historyType = object : TypeToken<List<PlaybackHistoryEntry>>() {}.type

    val randomHistoryFlow: Flow<List<PlaybackHistoryEntry>> = context.homeCacheDataStore.data.map { prefs ->
        parseHistoryJson(prefs[Keys.randomHistoryJson])
    }

    val sequentialHistoryFlow: Flow<List<PlaybackHistoryEntry>> = context.homeCacheDataStore.data.map { prefs ->
        parseHistoryJson(prefs[Keys.sequentialHistoryJson])
    }

    val playlistsFlow: Flow<List<MediaLibrary>> = context.homeCacheDataStore.data.map { prefs ->
        prefs[Keys.librariesJson]
            ?.takeIf { it.isNotBlank() }
            ?.let { gson.fromJson<List<MediaLibrary>>(it, librariesType) }
            .orEmpty()
            .filter { it.type == MediaLibraryType.PLAYLIST }
    }

    suspend fun readCache(): HomeCache {
        val prefs = context.homeCacheDataStore.data.first()
        val videos = prefs[Keys.videosJson]
            ?.takeIf { it.isNotBlank() }
            ?.let { gson.fromJson<List<VideoItem>>(it, videosType) }
            .orEmpty()
        val libraries = prefs[Keys.librariesJson]
            ?.takeIf { it.isNotBlank() }
            ?.let { gson.fromJson<List<MediaLibrary>>(it, librariesType) }
            .orEmpty()
        return HomeCache(videos = videos, libraries = libraries)
    }

    suspend fun saveVideos(videos: List<VideoItem>) {
        context.homeCacheDataStore.edit { prefs: MutablePreferences ->
            prefs[Keys.videosJson] = gson.toJson(videos)
        }
    }

    suspend fun saveLibraries(libraries: List<MediaLibrary>) {
        context.homeCacheDataStore.edit { prefs: MutablePreferences ->
            prefs[Keys.librariesJson] = gson.toJson(libraries)
        }
    }

    suspend fun readFavorites(): List<VideoItem> {
        val prefs = context.homeCacheDataStore.data.first()
        return prefs[Keys.favoritesJson]
            ?.takeIf { it.isNotBlank() }
            ?.let { gson.fromJson<List<VideoItem>>(it, favoritesType) }
            .orEmpty()
    }

    suspend fun saveFavorites(videos: List<VideoItem>) {
        context.homeCacheDataStore.edit { prefs: MutablePreferences ->
            prefs[Keys.favoritesJson] = gson.toJson(videos)
        }
    }

    suspend fun readSequentialTable(): SequentialPlaybackTable {
        val prefs = context.homeCacheDataStore.data.first()
        return prefs[Keys.sequentialTableJson]
            ?.takeIf { it.isNotBlank() }
            ?.let { gson.fromJson<SequentialPlaybackTable>(it, sequentialTableType) }
            ?: SequentialPlaybackTable()
    }

    suspend fun saveSequentialTable(table: SequentialPlaybackTable) {
        context.homeCacheDataStore.edit { prefs: MutablePreferences ->
            prefs[Keys.sequentialTableJson] = gson.toJson(table)
        }
    }

    suspend fun recordHistory(
        mode: PlaybackHistoryMode,
        video: VideoItem,
        sourceName: String?
    ) {
        context.homeCacheDataStore.edit { prefs: MutablePreferences ->
            val key = when (mode) {
                PlaybackHistoryMode.RANDOM -> Keys.randomHistoryJson
                PlaybackHistoryMode.SEQUENTIAL -> Keys.sequentialHistoryJson
            }
            val oldList = parseHistoryJson(prefs[key])
            val now = System.currentTimeMillis()
            val entry = PlaybackHistoryEntry(
                video = video,
                playedAtMs = now,
                sourceName = sourceName
            )
            val merged = buildList {
                add(entry)
                addAll(oldList.filterNot { it.video.id == video.id })
            }.take(MAX_HISTORY_ITEMS)

            prefs[key] = gson.toJson(merged)
        }
    }

    private fun parseHistoryJson(json: String?): List<PlaybackHistoryEntry> {
        return json
            ?.takeIf { it.isNotBlank() }
            ?.let { gson.fromJson<List<PlaybackHistoryEntry>>(it, historyType) }
            .orEmpty()
    }
}
