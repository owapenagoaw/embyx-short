package com.lalakiop.embyx.domain.usecase

import com.lalakiop.embyx.core.model.MediaLibrary
import com.lalakiop.embyx.data.repository.VideoRepository

class GetLibrariesUseCase(
    private val repository: VideoRepository
) {
    suspend operator fun invoke(): Result<List<MediaLibrary>> {
        return repository.getLibraries()
    }
}
