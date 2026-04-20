package com.lalakiop.embyx.data.local

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.security.MessageDigest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import com.lalakiop.embyx.core.model.MediaLibrary
import com.lalakiop.embyx.core.model.MediaLibraryType
import com.lalakiop.embyx.core.model.Session
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

class HomeCacheStore(
    private val context: Context,
    private val sessionStore: SessionStore
) {

    private companion object {
        const val MAX_HISTORY_ITEMS = 200
        const val DEFAULT_SCOPE_ID = "default"
        const val SCOPE_ID_LENGTH = 24
    }

    private object KeyNames {
        const val videosJson = "videos_json"
        const val librariesJson = "libraries_json"
        const val favoritesJson = "favorites_json"
        const val sequentialTableJson = "sequential_table_json"
        const val randomHistoryJson = "random_history_json"
        const val sequentialHistoryJson = "sequential_history_json"
    }

    private object LegacyKeys {
        val videosJson = stringPreferencesKey(KeyNames.videosJson)
        val librariesJson = stringPreferencesKey(KeyNames.librariesJson)
        val favoritesJson = stringPreferencesKey(KeyNames.favoritesJson)
        val sequentialTableJson = stringPreferencesKey(KeyNames.sequentialTableJson)
        val randomHistoryJson = stringPreferencesKey(KeyNames.randomHistoryJson)
        val sequentialHistoryJson = stringPreferencesKey(KeyNames.sequentialHistoryJson)
    }

    private data class CacheScope(
        val storageId: String
    )

    private val gson = Gson()
    private val videosType = object : TypeToken<List<VideoItem>>() {}.type
    private val librariesType = object : TypeToken<List<MediaLibrary>>() {}.type
    private val favoritesType = object : TypeToken<List<VideoItem>>() {}.type
    private val sequentialTableType = object : TypeToken<SequentialPlaybackTable>() {}.type
    private val historyType = object : TypeToken<List<PlaybackHistoryEntry>>() {}.type

    val randomHistoryFlow: Flow<List<PlaybackHistoryEntry>> =
        sessionStore.sessionFlow.combine(context.homeCacheDataStore.data) { session, prefs ->
            val scope = scopeFor(session)
            parseHistoryJson(
                readScopedJson(
                    prefs = prefs,
                    keyName = KeyNames.randomHistoryJson,
                    scope = scope,
                    legacyKey = LegacyKeys.randomHistoryJson
                )
            )
        }

    val sequentialHistoryFlow: Flow<List<PlaybackHistoryEntry>> =
        sessionStore.sessionFlow.combine(context.homeCacheDataStore.data) { session, prefs ->
            val scope = scopeFor(session)
            parseHistoryJson(
                readScopedJson(
                    prefs = prefs,
                    keyName = KeyNames.sequentialHistoryJson,
                    scope = scope,
                    legacyKey = LegacyKeys.sequentialHistoryJson
                )
            )
        }

    val playlistsFlow: Flow<List<MediaLibrary>> =
        sessionStore.sessionFlow.combine(context.homeCacheDataStore.data) { session, prefs ->
            val scope = scopeFor(session)
            parseJsonList<MediaLibrary>(
                json = readScopedJson(
                    prefs = prefs,
                    keyName = KeyNames.librariesJson,
                    scope = scope,
                    legacyKey = LegacyKeys.librariesJson
                ),
                type = librariesType
            ).filter { it.type == MediaLibraryType.PLAYLIST }
        }

    suspend fun readCache(): HomeCache {
        val scope = currentScope()
        val prefs = context.homeCacheDataStore.data.first()
        val videos = parseJsonList<VideoItem>(
            json = readScopedJson(
                prefs = prefs,
                keyName = KeyNames.videosJson,
                scope = scope,
                legacyKey = LegacyKeys.videosJson
            ),
            type = videosType
        )
        val libraries = parseJsonList<MediaLibrary>(
            json = readScopedJson(
                prefs = prefs,
                keyName = KeyNames.librariesJson,
                scope = scope,
                legacyKey = LegacyKeys.librariesJson
            ),
            type = librariesType
        )
        return HomeCache(videos = videos, libraries = libraries)
    }

    suspend fun saveVideos(videos: List<VideoItem>) {
        val scope = currentScope()
        context.homeCacheDataStore.edit { prefs: MutablePreferences ->
            prefs[scopedStringKey(KeyNames.videosJson, scope)] = gson.toJson(videos)
        }
    }

    suspend fun saveLibraries(libraries: List<MediaLibrary>) {
        val scope = currentScope()
        context.homeCacheDataStore.edit { prefs: MutablePreferences ->
            prefs[scopedStringKey(KeyNames.librariesJson, scope)] = gson.toJson(libraries)
        }
    }

    suspend fun readFavorites(): List<VideoItem> {
        val scope = currentScope()
        val prefs = context.homeCacheDataStore.data.first()
        return parseJsonList<VideoItem>(
            json = readScopedJson(
                prefs = prefs,
                keyName = KeyNames.favoritesJson,
                scope = scope,
                legacyKey = LegacyKeys.favoritesJson
            ),
            type = favoritesType
        )
    }

    suspend fun saveFavorites(videos: List<VideoItem>) {
        val scope = currentScope()
        context.homeCacheDataStore.edit { prefs: MutablePreferences ->
            prefs[scopedStringKey(KeyNames.favoritesJson, scope)] = gson.toJson(videos)
        }
    }

    suspend fun readSequentialTable(): SequentialPlaybackTable {
        val scope = currentScope()
        val prefs = context.homeCacheDataStore.data.first()
        return readScopedJson(
            prefs = prefs,
            keyName = KeyNames.sequentialTableJson,
            scope = scope,
            legacyKey = LegacyKeys.sequentialTableJson
        )
            ?.takeIf { it.isNotBlank() }
            ?.let { gson.fromJson<SequentialPlaybackTable>(it, sequentialTableType) }
            ?: SequentialPlaybackTable()
    }

    suspend fun saveSequentialTable(table: SequentialPlaybackTable) {
        val scope = currentScope()
        context.homeCacheDataStore.edit { prefs: MutablePreferences ->
            prefs[scopedStringKey(KeyNames.sequentialTableJson, scope)] = gson.toJson(table)
        }
    }

    suspend fun recordHistory(
        mode: PlaybackHistoryMode,
        video: VideoItem,
        sourceName: String?
    ) {
        val scope = currentScope()
        context.homeCacheDataStore.edit { prefs: MutablePreferences ->
            val keyName = when (mode) {
                PlaybackHistoryMode.RANDOM -> KeyNames.randomHistoryJson
                PlaybackHistoryMode.SEQUENTIAL -> KeyNames.sequentialHistoryJson
            }
            val legacyKey = when (mode) {
                PlaybackHistoryMode.RANDOM -> LegacyKeys.randomHistoryJson
                PlaybackHistoryMode.SEQUENTIAL -> LegacyKeys.sequentialHistoryJson
            }
            val key = scopedStringKey(keyName, scope)
            val oldList = parseHistoryJson(
                prefs[key] ?: prefs[legacyKey]
            )
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

    private suspend fun currentScope(): CacheScope {
        val session = sessionStore.sessionFlow.first()
        return scopeFor(session)
    }

    private fun scopeFor(session: Session): CacheScope {
        if (!session.isLoggedIn) {
            return CacheScope(DEFAULT_SCOPE_ID)
        }
        val normalizedServer = session.server.trim().trimEnd('/').lowercase()
        val normalizedUserId = session.userId.trim().lowercase()
        if (normalizedServer.isBlank() || normalizedUserId.isBlank()) {
            return CacheScope(DEFAULT_SCOPE_ID)
        }
        return CacheScope(hashToStorageId("$normalizedServer|$normalizedUserId"))
    }

    private fun hashToStorageId(raw: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return digest
            .joinToString(separator = "") { byte ->
                (byte.toInt() and 0xFF).toString(16).padStart(2, '0')
            }
            .take(SCOPE_ID_LENGTH)
    }

    private fun scopedStringKey(keyName: String, scope: CacheScope): Preferences.Key<String> {
        return stringPreferencesKey("${keyName}_${scope.storageId}")
    }

    private fun readScopedJson(
        prefs: Preferences,
        keyName: String,
        scope: CacheScope,
        legacyKey: Preferences.Key<String>
    ): String? {
        val scoped = prefs[scopedStringKey(keyName, scope)]
        return if (!scoped.isNullOrBlank()) scoped else prefs[legacyKey]
    }

    private fun <T> parseJsonList(
        json: String?,
        type: Type
    ): List<T> {
        return json
            ?.takeIf { it.isNotBlank() }
            ?.let { gson.fromJson<List<T>>(it, type) }
            .orEmpty()
    }

    private fun parseHistoryJson(json: String?): List<PlaybackHistoryEntry> {
        return json
            ?.takeIf { it.isNotBlank() }
            ?.let { gson.fromJson<List<PlaybackHistoryEntry>>(it, historyType) }
            .orEmpty()
    }

}
