package com.lalakiop.embyx.domain.usecase

import com.lalakiop.embyx.data.repository.VideoRepository

class SetFavoriteUseCase(
    private val repository: VideoRepository
) {
    suspend operator fun invoke(itemId: String, favorite: Boolean): Result<Unit> {
        return repository.setFavorite(itemId = itemId, favorite = favorite)
    }
}
