package com.lalakiop.embyx.domain.usecase

import com.lalakiop.embyx.core.model.VideoItem
import com.lalakiop.embyx.data.repository.VideoRepository

class GetFeedUseCase(
    private val repository: VideoRepository
) {
    suspend operator fun invoke(
        parentId: String? = null,
        random: Boolean = false,
        favoritesOnly: Boolean = false,
        startIndex: Int = 0,
        limit: Int = 99
    ): Result<List<VideoItem>> {
        return repository.getFeed(
            parentId = parentId,
            random = random,
            favoritesOnly = favoritesOnly,
            startIndex = startIndex,
            limit = limit
        )
    }
}
