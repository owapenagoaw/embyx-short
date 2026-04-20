package com.lalakiop.embyx.player

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import java.io.File
import java.security.MessageDigest

class PlayerRuntimeConfig(context: Context) {
    private val appContext = context.applicationContext
    private val cacheLock = Any()
    private val databaseProvider = StandaloneDatabaseProvider(appContext)

    @Volatile
    private var activeScopeId: String = DEFAULT_SCOPE_ID

    @Volatile
    private var activeCacheDir: File = createCacheDirForScope(DEFAULT_SCOPE_ID)

    @Volatile
    private var simpleCache: SimpleCache = newSimpleCache(activeCacheDir)

    fun setCacheScope(server: String, userId: String) {
        val nextScopeId = scopeIdFor(server = server, userId = userId)
        synchronized(cacheLock) {
            if (nextScopeId == activeScopeId) {
                return
            }

            runCatching { simpleCache.release() }
            activeCacheDir = createCacheDirForScope(nextScopeId)
            simpleCache = newSimpleCache(activeCacheDir)
            activeScopeId = nextScopeId
        }
    }

    fun newMediaSourceFactory(): DefaultMediaSourceFactory {
        val scopedCache = synchronized(cacheLock) { simpleCache }
        val upstreamDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(scopedCache)
            .setUpstreamDataSourceFactory(upstreamDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        return DefaultMediaSourceFactory(cacheDataSourceFactory)
    }

    fun cacheDirectory(): File = synchronized(cacheLock) { activeCacheDir }

    fun newLoadControl(): DefaultLoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                MIN_BUFFER_MS,
                MAX_BUFFER_MS,
                BUFFER_FOR_PLAYBACK_MS,
                BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .setBackBuffer(BACK_BUFFER_MS, false)
            .setTargetBufferBytes(TARGET_BUFFER_BYTES)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    companion object {
        const val MAX_DISK_CACHE_BYTES: Long = 2L * 1024L * 1024L * 1024L

        private const val MEDIA_CACHE_DIR_PREFIX = "player_media_cache_"
        private const val DEFAULT_SCOPE_ID = "default"
        private const val SCOPE_ID_LENGTH = 24
        private const val MIN_BUFFER_MS = 10_000
        private const val MAX_BUFFER_MS = 30_000
        private const val BUFFER_FOR_PLAYBACK_MS = 500
        private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 1_000
        private const val BACK_BUFFER_MS = 0
        private const val TARGET_BUFFER_BYTES = 4 * 1024 * 1024
    }

    private fun newSimpleCache(cacheDir: File): SimpleCache {
        val cacheEvictor = LeastRecentlyUsedCacheEvictor(MAX_DISK_CACHE_BYTES)
        return SimpleCache(cacheDir, cacheEvictor, databaseProvider)
    }

    private fun createCacheDirForScope(scopeId: String): File {
        return File(appContext.cacheDir, "$MEDIA_CACHE_DIR_PREFIX$scopeId").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    private fun scopeIdFor(server: String, userId: String): String {
        val normalizedServer = server.trim().trimEnd('/').lowercase()
        val normalizedUserId = userId.trim().lowercase()
        if (normalizedServer.isBlank() || normalizedUserId.isBlank()) {
            return DEFAULT_SCOPE_ID
        }

        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$normalizedServer|$normalizedUserId".toByteArray(Charsets.UTF_8))
        return digest
            .joinToString(separator = "") { byte ->
                (byte.toInt() and 0xFF).toString(16).padStart(2, '0')
            }
            .take(SCOPE_ID_LENGTH)
    }
}
