package com.lalakiop.embyx.data.local

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import com.lalakiop.embyx.core.model.MediaLibrary
import com.lalakiop.embyx.core.model.VideoItem

private val Context.homeCacheDataStore by preferencesDataStore(name = "embyx_home_cache")

data class HomeCache(
    val videos: List<VideoItem> = emptyList(),
    val libraries: List<MediaLibrary> = emptyList()
)

class HomeCacheStore(private val context: Context) {

    private object Keys {
        val videosJson = stringPreferencesKey("videos_json")
        val librariesJson = stringPreferencesKey("libraries_json")
        val favoritesJson = stringPreferencesKey("favorites_json")
    }

    private val gson = Gson()
    private val videosType = object : TypeToken<List<VideoItem>>() {}.type
    private val librariesType = object : TypeToken<List<MediaLibrary>>() {}.type
    private val favoritesType = object : TypeToken<List<VideoItem>>() {}.type

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
}
