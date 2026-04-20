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
import com.lalakiop.embyx.data.local.PlaybackHistoryMode
import com.lalakiop.embyx.data.local.SequentialPlaybackState
import com.lalakiop.embyx.data.local.SequentialPlaybackTable
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
        const val ALL_LIBRARY_KEY = "ALL::ALL"
    }

    data class PlaybackSnapshot(
        val page: Int,
        val positionMs: Long,
        val wasPlaying: Boolean
    )

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private var pendingPlaybackSnapshot: PlaybackSnapshot? = null
    private var sequentialStates: MutableMap<String, SequentialPlaybackState> = mutableMapOf()

    init {
        viewModelScope.launch {
            val settings = uiSettingsStore.settingsFlow.first()
            val cached = homeCacheStore.readCache()
            val sequentialTable = homeCacheStore.readSequentialTable()

            sequentialStates = sequentialTable.states.toMutableMap()
            val restoredLibraryId = sequentialTable.selectedLibraryId
            val restoredLibraryType = sequentialTable.selectedLibraryType
            val restoredSequentialState = if (settings.randomModeEnabled) {
                null
            } else {
                sequentialStates[buildLibraryKey(restoredLibraryId, restoredLibraryType)]
            }

            _uiState.update {
                val startupVideos = when {
                    restoredSequentialState?.videos?.isNotEmpty() == true -> restoredSequentialState.videos
                    cached.videos.isNotEmpty() -> cached.videos
                    else -> it.videos
                }
                val requestedPage = restoredSequentialState?.currentPage ?: it.currentPage
                val safePage = if (startupVideos.isEmpty()) 0 else requestedPage.coerceIn(0, startupVideos.lastIndex)
                it.copy(
                    isLoading = startupVideos.isEmpty(),
                    videos = startupVideos,
                    libraries = if (cached.libraries.isNotEmpty()) cached.libraries else it.libraries,
                    isRandomMode = settings.randomModeEnabled,
                    selectedLibraryId = restoredLibraryId,
                    selectedLibraryType = restoredLibraryType,
                    currentPage = safePage,
                    isPlaying = restoredSequentialState?.wasPlaying ?: it.isPlaying,
                    errorMessage = null
                )
            }

            if (restoredSequentialState != null && restoredSequentialState.videos.isNotEmpty()) {
                val safePage = restoredSequentialState.currentPage
                    .coerceIn(0, (restoredSequentialState.videos.size - 1).coerceAtLeast(0))
                pendingPlaybackSnapshot = PlaybackSnapshot(
                    page = safePage,
                    positionMs = restoredSequentialState.positionMs.coerceAtLeast(0L),
                    wasPlaying = restoredSequentialState.wasPlaying
                )
            }

            loadLibraries()
            if (settings.randomModeEnabled) {
                refresh(
                    random = true,
                    resetPage = false,
                    showLoading = cached.videos.isEmpty()
                )
            } else if (restoredSequentialState == null || restoredSequentialState.videos.isEmpty()) {
                refresh(
                    random = false,
                    resetPage = false,
                    showLoading = cached.videos.isEmpty()
                )
            }
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

                    var resolvedPage = 0
                    _uiState.update {
                        val safePage = when {
                            videos.isEmpty() -> 0
                            resetPage -> 0
                            else -> it.currentPage.coerceIn(0, videos.lastIndex)
                        }
                        resolvedPage = safePage
                        it.copy(
                            isLoading = false,
                            videos = videos,
                            errorMessage = null,
                            currentPage = safePage
                        )
                    }

                    if (!random) {
                        val state = _uiState.value
                        sequentialStates[currentLibraryKey(state)] = SequentialPlaybackState(
                            videos = videos,
                            currentPage = resolvedPage,
                            positionMs = 0L,
                            wasPlaying = state.isPlaying,
                            updatedAtMs = System.currentTimeMillis()
                        )
                        persistSequentialTable(state)
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

                    var shouldPersistSelection = false
                    _uiState.update {
                        val selectedId = it.selectedLibraryId
                        val selectedType = it.selectedLibraryType
                        val selectionExists = selectedId != null && selectedType != null && libraries.any { library ->
                            library.id == selectedId && library.type == selectedType
                        }
                        if (selectionExists) {
                            it.copy(libraries = libraries)
                        } else {
                            shouldPersistSelection = selectedId != null || selectedType != null
                            it.copy(
                                libraries = libraries,
                                selectedLibraryId = null,
                                selectedLibraryType = null
                            )
                        }
                    }

                    if (shouldPersistSelection) {
                        persistSequentialTable(_uiState.value)
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
        val previousState = _uiState.value
        if (!previousState.isRandomMode) {
            saveCurrentSequentialState(
                positionMs = null,
                wasPlaying = previousState.isPlaying,
                page = previousState.currentPage
            )
        }

        val selectedLibraryId = library?.takeIf { lib -> lib.type != MediaLibraryType.FAVORITES }?.id
        val selectedLibraryType = library?.type
        _uiState.update {
            it.copy(
                selectedLibraryId = selectedLibraryId,
                selectedLibraryType = selectedLibraryType
            )
        }

        val state = _uiState.value
        viewModelScope.launch {
            persistSequentialTable(state)
        }

        if (state.isRandomMode) {
            refresh(random = true, resetPage = true, showLoading = true)
            return
        }

        if (!restoreSequentialStateForCurrentLibrary()) {
            refresh(random = false, resetPage = false, showLoading = true)
        }
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
        recordHistoryAtPage(safeIndex)
    }

    fun toggleRandomMode() {
        val enabled = !_uiState.value.isRandomMode
        val current = _uiState.value

        if (enabled && !current.isRandomMode) {
            saveCurrentSequentialState(
                positionMs = null,
                wasPlaying = current.isPlaying,
                page = current.currentPage
            )
        }

        _uiState.update {
            it.copy(
                isRandomMode = enabled,
                noticeMessage = if (enabled) "随机模式已开启" else "随机模式已关闭"
            )
        }
        viewModelScope.launch {
            uiSettingsStore.setRandomModeEnabled(enabled)
        }

        if (enabled) {
            refresh(random = true, resetPage = true, showLoading = false)
            return
        }

        if (!restoreSequentialStateForCurrentLibrary()) {
            refresh(random = false, resetPage = false, showLoading = false)
        }
    }

    fun onPageChanged(page: Int) {
        var safePage = 0
        _uiState.update {
            safePage = page.coerceIn(0, (it.videos.size - 1).coerceAtLeast(0))
            it.copy(currentPage = safePage, isPlaying = true)
        }

        if (!_uiState.value.isRandomMode) {
            saveCurrentSequentialState(
                positionMs = null,
                wasPlaying = true,
                page = safePage
            )
        }

        recordHistoryAtPage(safePage)
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

        if (!_uiState.value.isRandomMode) {
            saveCurrentSequentialState(
                positionMs = positionMs,
                wasPlaying = wasPlaying,
                page = page
            )
        }
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

    private fun buildLibraryKey(
        libraryId: String?,
        libraryType: MediaLibraryType?
    ): String {
        if (libraryId == null && libraryType == null) {
            return ALL_LIBRARY_KEY
        }
        val typePart = libraryType?.name ?: "ALL"
        val idPart = libraryId ?: "ALL"
        return "$typePart::$idPart"
    }

    private fun currentLibraryKey(state: HomeUiState = _uiState.value): String {
        return buildLibraryKey(
            libraryId = state.selectedLibraryId,
            libraryType = state.selectedLibraryType
        )
    }

    private fun restoreSequentialStateForCurrentLibrary(): Boolean {
        val state = _uiState.value
        val sequentialState = sequentialStates[currentLibraryKey(state)] ?: return false
        if (sequentialState.videos.isEmpty()) {
            return false
        }

        val safePage = sequentialState.currentPage
            .coerceIn(0, (sequentialState.videos.size - 1).coerceAtLeast(0))

        _uiState.update {
            it.copy(
                isLoading = false,
                videos = sequentialState.videos,
                currentPage = safePage,
                isPlaying = sequentialState.wasPlaying,
                errorMessage = null
            )
        }

        pendingPlaybackSnapshot = PlaybackSnapshot(
            page = safePage,
            positionMs = sequentialState.positionMs.coerceAtLeast(0L),
            wasPlaying = sequentialState.wasPlaying
        )

        return true
    }

    private fun saveCurrentSequentialState(
        positionMs: Long?,
        wasPlaying: Boolean?,
        page: Int?
    ) {
        val state = _uiState.value
        val key = currentLibraryKey(state)
        val existing = sequentialStates[key]
        val videos = if (state.videos.isNotEmpty()) state.videos else existing?.videos.orEmpty()
        if (videos.isEmpty()) {
            return
        }

        val requestedPage = page ?: state.currentPage
        val safePage = requestedPage.coerceIn(0, videos.lastIndex)

        sequentialStates[key] = SequentialPlaybackState(
            videos = videos,
            currentPage = safePage,
            positionMs = (positionMs ?: existing?.positionMs ?: 0L).coerceAtLeast(0L),
            wasPlaying = wasPlaying ?: state.isPlaying,
            updatedAtMs = System.currentTimeMillis()
        )

        viewModelScope.launch {
            persistSequentialTable(state)
        }
    }

    private suspend fun persistSequentialTable(state: HomeUiState) {
        homeCacheStore.saveSequentialTable(
            SequentialPlaybackTable(
                selectedLibraryId = state.selectedLibraryId,
                selectedLibraryType = state.selectedLibraryType,
                states = sequentialStates.toMap()
            )
        )
    }

    private fun recordHistoryAtPage(page: Int) {
        val state = _uiState.value
        val item = state.videos.getOrNull(page) ?: return
        val sourceName = state.libraries.firstOrNull {
            it.id == state.selectedLibraryId && it.type == state.selectedLibraryType
        }?.name

        viewModelScope.launch {
            homeCacheStore.recordHistory(
                mode = if (state.isRandomMode) PlaybackHistoryMode.RANDOM else PlaybackHistoryMode.SEQUENTIAL,
                video = item,
                sourceName = sourceName
            )
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
