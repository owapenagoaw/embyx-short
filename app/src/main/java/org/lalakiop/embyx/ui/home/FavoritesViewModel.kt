package com.lalakiop.embyx.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.lalakiop.embyx.core.model.VideoItem
import com.lalakiop.embyx.data.local.HomeCacheStore
import com.lalakiop.embyx.domain.usecase.GetFeedUseCase

data class FavoritesUiState(
    val isLoading: Boolean = false,
    val videos: List<VideoItem> = emptyList(),
    val errorMessage: String? = null
)

class FavoritesViewModel(
    private val getFeedUseCase: GetFeedUseCase,
    private val homeCacheStore: HomeCacheStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState(isLoading = true))
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val cachedVideos = homeCacheStore.readFavorites()
            if (cachedVideos.isNotEmpty()) {
                _uiState.update {
                    it.copy(isLoading = false, videos = cachedVideos, errorMessage = null)
                }
            }
            refresh(showLoading = cachedVideos.isEmpty())
        }
    }

    fun refresh(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading || _uiState.value.videos.isEmpty()) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            } else {
                _uiState.update { it.copy(errorMessage = null) }
            }

            getFeedUseCase(parentId = null, random = false, favoritesOnly = true)
                .onSuccess { videos ->
                    homeCacheStore.saveFavorites(videos)
                    _uiState.update {
                        it.copy(isLoading = false, videos = videos, errorMessage = null)
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "加载收藏失败"
                        )
                    }
                }
        }
    }

    class Factory(
        private val getFeedUseCase: GetFeedUseCase,
        private val homeCacheStore: HomeCacheStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FavoritesViewModel::class.java)) {
                return FavoritesViewModel(
                    getFeedUseCase = getFeedUseCase,
                    homeCacheStore = homeCacheStore
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
