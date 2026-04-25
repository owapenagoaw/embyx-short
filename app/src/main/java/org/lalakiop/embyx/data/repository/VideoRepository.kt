package com.lalakiop.embyx.data.repository

import com.lalakiop.embyx.core.model.MediaLibrary
import com.lalakiop.embyx.core.model.PlaybackQualityPreset
import com.lalakiop.embyx.core.model.ResolvedPlaybackStream
import com.lalakiop.embyx.core.model.VideoItem

interface VideoRepository {
    suspend fun getFeed(
        parentId: String? = null,
        random: Boolean = false,
        favoritesOnly: Boolean = false,
        startIndex: Int = 0,
        limit: Int = 99,
        searchTerm: String? = null
    ): Result<List<VideoItem>>

    suspend fun getLibraries(): Result<List<MediaLibrary>>

    suspend fun resolvePlaybackStream(
        itemId: String,
        preset: PlaybackQualityPreset,
        mediaSourceId: String? = null,
        startTimeTicks: Long = 0L,
        currentPlaySessionId: String? = null  // ⚠️ 新增：切换清晰度时传入旧会话ID
    ): Result<ResolvedPlaybackStream>

    suspend fun setFavorite(itemId: String, favorite: Boolean): Result<Unit>
}
