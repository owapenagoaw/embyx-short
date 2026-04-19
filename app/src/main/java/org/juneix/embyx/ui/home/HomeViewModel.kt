package com.lalakiop.embyx.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.lalakiop.embyx.core.model.MediaLibrary
import com.lalakiop.embyx.core.model.MediaLibraryType
import com.lalakiop.embyx.data.local.HomeCacheStore
import com.lalakiop.embyx.data.local.UiSettingsStore
import com.lalakiop.embyx.domain.usecase.GetFeedUseCase
import com.lalakiop.embyx.domain.usecase.GetLibrariesUseCase
import com.lalakiop.embyx.domain.usecase.SetFavoriteUseCase

class HomeViewModel(
    private val getFeedUseCase: GetFeedUseCase,
    private val getLibrariesUseCase: GetLibrariesUseCase,
    private val setFavoriteUseCase: SetFavoriteUseCase,
    private val uiSettingsStore: UiSettingsStore,
    private val homeCacheStore: HomeCacheStore
) : ViewModel() {

    private companion object {
        const val LIBRARY_PAGE_SIZE = 30
    }

    data class PlaybackSnapshot(
        val page: Int,
        val positionMs: Long,
        val wasPlaying: Boolean
    )

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private var pendingPlaybackSnapshot: PlaybackSnapshot? = null

    init {
        viewModelScope.launch {
            val settings = uiSettingsStore.settingsFlow.first()
            val cached = homeCacheStore.readCache()

            _uiState.update {
                val safePage = if (cached.videos.isEmpty()) 0 else it.currentPage.coerceIn(0, cached.videos.lastIndex)
                it.copy(
                    isLoading = cached.videos.isEmpty(),
                    videos = if (cached.videos.isNotEmpty()) cached.videos else it.videos,
                    libraries = if (cached.libraries.isNotEmpty()) cached.libraries else it.libraries,
                    isRandomMode = settings.randomModeEnabled,
                    currentPage = safePage,
                    errorMessage = null
                )
            }

            loadLibraries()
            refresh(
                random = settings.randomModeEnabled,
                resetPage = false,
                showLoading = cached.videos.isEmpty()
            )
        }
    }

    fun refresh(
        random: Boolean = _uiState.value.isRandomMode,
        resetPage: Boolean = false,
        showLoading: Boolean = true
    ) {
        viewModelScope.launch {
            val selected = _uiState.value
            if (showLoading) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null, noticeMessage = null) }
            } else {
                _uiState.update { it.copy(errorMessage = null, noticeMessage = null) }
            }

            val result = getFeedUseCase(
                parentId = selected.selectedLibraryId,
                random = random,
                favoritesOnly = false
            )
            result
                .onSuccess { videos ->
                    homeCacheStore.saveVideos(videos)
                    _uiState.update {
                        val safePage = when {
                            videos.isEmpty() -> 0
                            resetPage -> 0
                            else -> it.currentPage.coerceIn(0, videos.lastIndex)
                        }
                        it.copy(
                            isLoading = false,
                            videos = videos,
                            errorMessage = null,
                            currentPage = safePage
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "加载失败"
                        )
                    }
                }
        }
    }

    fun loadLibraries() {
        viewModelScope.launch {
            getLibrariesUseCase()
                .onSuccess { libraries ->
                    homeCacheStore.saveLibraries(libraries)
                    _uiState.update {
                        val selectedId = it.selectedLibraryId
                        val selectedType = it.selectedLibraryType
                        if (selectedId != null && selectedType != null) {
                            it.copy(libraries = libraries)
                        } else {
                            it.copy(
                                libraries = libraries,
                                selectedLibraryId = null,
                                selectedLibraryType = null
                            )
                        }
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(noticeMessage = throwable.message ?: "媒体库加载失败")
                    }
                }
        }
    }

    fun selectLibrary(library: MediaLibrary?) {
        _uiState.update {
            it.copy(
                selectedLibraryId = library?.takeIf { lib -> lib.type != MediaLibraryType.FAVORITES }?.id,
                selectedLibraryType = library?.type
            )
        }
        refresh(showLoading = true)
    }

    fun openLibraryBrowser(library: MediaLibrary) {
        _uiState.update {
            it.copy(
                browsingLibraryId = library.id,
                browsingLibraryName = library.name,
                browsingVideos = emptyList(),
                isBrowsingLoading = true,
                browsingErrorMessage = null,
                browsingHasMore = true,
                browsingNextStartIndex = 0
            )
        }
        requestLibraryPage(libraryId = library.id, startIndex = 0, append = false)
    }

    fun refreshLibraryBrowser() {
        val current = _uiState.value
        val libraryId = current.browsingLibraryId ?: return
        _uiState.update {
            it.copy(
                browsingVideos = emptyList(),
                isBrowsingLoading = true,
                browsingErrorMessage = null,
                browsingHasMore = true,
                browsingNextStartIndex = 0
            )
        }
        requestLibraryPage(libraryId = libraryId, startIndex = 0, append = false)
    }

    fun loadMoreLibraryBrowser() {
        val state = _uiState.value
        val libraryId = state.browsingLibraryId ?: return
        if (state.isBrowsingLoading || !state.browsingHasMore) return
        requestLibraryPage(
            libraryId = libraryId,
            startIndex = state.browsingNextStartIndex,
            append = true
        )
    }

    fun closeLibraryBrowser() {
        _uiState.update {
            it.copy(
                browsingLibraryId = null,
                browsingLibraryName = null,
                browsingVideos = emptyList(),
                isBrowsingLoading = false,
                browsingErrorMessage = null,
                browsingHasMore = false,
                browsingNextStartIndex = 0
            )
        }
    }

    private fun requestLibraryPage(
        libraryId: String,
        startIndex: Int,
        append: Boolean
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBrowsingLoading = true,
                    browsingErrorMessage = null
                )
            }

            getFeedUseCase(
                parentId = libraryId,
                random = false,
                favoritesOnly = false,
                startIndex = startIndex,
                limit = LIBRARY_PAGE_SIZE
            )
                .onSuccess { videos ->
                    _uiState.update {
                        val merged = if (append) {
                            (it.browsingVideos + videos).distinctBy { item -> item.id }
                        } else {
                            videos
                        }
                        it.copy(
                            browsingVideos = merged,
                            isBrowsingLoading = false,
                            browsingErrorMessage = null,
                            browsingHasMore = videos.size >= LIBRARY_PAGE_SIZE,
                            browsingNextStartIndex = startIndex + videos.size
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isBrowsingLoading = false,
                            browsingErrorMessage = throwable.message ?: "加载媒体失败"
                        )
                    }
                }
        }
    }

    fun playVideoAt(index: Int) {
        val safeIndex = index.coerceIn(0, (_uiState.value.videos.size - 1).coerceAtLeast(0))
        _uiState.update {
            it.copy(currentPage = safeIndex, isPlaying = true)
        }
    }

    fun toggleRandomMode() {
        val enabled = !_uiState.value.isRandomMode
        _uiState.update {
            it.copy(
                isRandomMode = enabled,
                noticeMessage = if (enabled) "随机模式已开启" else "随机模式已关闭"
            )
        }
        viewModelScope.launch {
            uiSettingsStore.setRandomModeEnabled(enabled)
        }
        refresh(random = enabled, resetPage = true, showLoading = false)
    }

    fun onPageChanged(page: Int) {
        _uiState.update {
            val safePage = page.coerceIn(0, (it.videos.size - 1).coerceAtLeast(0))
            it.copy(currentPage = safePage, isPlaying = true)
        }
    }

    fun toggleMuted() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
    }

    fun togglePlayPause() {
        _uiState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    fun pausePlayback() {
        _uiState.update { it.copy(isPlaying = false) }
    }

    fun consumeNotice() {
        _uiState.update { it.copy(noticeMessage = null) }
    }

    fun cachePlaybackSnapshot(page: Int, positionMs: Long, wasPlaying: Boolean) {
        pendingPlaybackSnapshot = PlaybackSnapshot(
            page = page.coerceAtLeast(0),
            positionMs = positionMs.coerceAtLeast(0L),
            wasPlaying = wasPlaying
        )
    }

    fun consumePlaybackSnapshot(expectedPage: Int): PlaybackSnapshot? {
        val snapshot = pendingPlaybackSnapshot ?: return null
        if (snapshot.page != expectedPage) return null
        pendingPlaybackSnapshot = null
        return snapshot
    }

    fun toggleFavoriteCurrent() {
        val state = _uiState.value
        val current = state.videos.getOrNull(state.currentPage) ?: return
        if (state.isFavoriteUpdating) return

        val targetFavorite = !current.isFavorite
        val updatedVideos = state.videos.toMutableList().apply {
            this[state.currentPage] = current.copy(isFavorite = targetFavorite)
        }
        _uiState.update {
            it.copy(
                videos = updatedVideos,
                isFavoriteUpdating = true,
                noticeMessage = null
            )
        }

        viewModelScope.launch {
            setFavoriteUseCase(itemId = current.id, favorite = targetFavorite)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isFavoriteUpdating = false,
                            noticeMessage = if (targetFavorite) "已加入收藏" else "已取消收藏"
                        )
                    }
                }
                .onFailure { throwable ->
                    val reverted = _uiState.value.videos.toMutableList()
                    val latest = reverted.getOrNull(_uiState.value.currentPage)
                    if (latest != null) {
                        reverted[_uiState.value.currentPage] = latest.copy(isFavorite = !targetFavorite)
                    }
                    _uiState.update {
                        it.copy(
                            videos = reverted,
                            isFavoriteUpdating = false,
                            noticeMessage = throwable.message ?: "收藏同步失败"
                        )
                    }
                }
        }
    }

    class Factory(
        private val getFeedUseCase: GetFeedUseCase,
        private val getLibrariesUseCase: GetLibrariesUseCase,
        private val setFavoriteUseCase: SetFavoriteUseCase,
        private val uiSettingsStore: UiSettingsStore,
        private val homeCacheStore: HomeCacheStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                return HomeViewModel(
                    getFeedUseCase = getFeedUseCase,
                    getLibrariesUseCase = getLibrariesUseCase,
                    setFavoriteUseCase = setFavoriteUseCase,
                    uiSettingsStore = uiSettingsStore,
                    homeCacheStore = homeCacheStore
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
