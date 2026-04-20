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

class PlayerRuntimeConfig(context: Context) {
    private val appContext = context.applicationContext

    private val cacheDir = File(appContext.cacheDir, MEDIA_CACHE_DIR).apply {
        if (!exists()) {
            mkdirs()
        }
    }
    private val databaseProvider = StandaloneDatabaseProvider(appContext)
    private val cacheEvictor = LeastRecentlyUsedCacheEvictor(MAX_DISK_CACHE_BYTES)
    private val simpleCache = SimpleCache(cacheDir, cacheEvictor, databaseProvider)

    private val upstreamDataSourceFactory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)

    private val cacheDataSourceFactory = CacheDataSource.Factory()
        .setCache(simpleCache)
        .setUpstreamDataSourceFactory(upstreamDataSourceFactory)
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

    fun newMediaSourceFactory(): DefaultMediaSourceFactory {
        return DefaultMediaSourceFactory(cacheDataSourceFactory)
    }

    fun cacheDirectory(): File = cacheDir

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

        private const val MEDIA_CACHE_DIR = "player_media_cache"
        private const val MIN_BUFFER_MS = 10_000
        private const val MAX_BUFFER_MS = 30_000
        private const val BUFFER_FOR_PLAYBACK_MS = 500
        private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 1_000
        private const val BACK_BUFFER_MS = 0
        private const val TARGET_BUFFER_BYTES = 4 * 1024 * 1024
    }
}
