package com.lalakiop.embyx.ui.home

import com.lalakiop.embyx.core.model.MediaLibrary
import com.lalakiop.embyx.core.model.MediaLibraryType
import com.lalakiop.embyx.core.model.VideoItem

data class HomeUiState(
    val isLoading: Boolean = false,
    val videos: List<VideoItem> = emptyList(),
    val libraries: List<MediaLibrary> = emptyList(),
    val selectedLibraryId: String? = null,
    val selectedLibraryType: MediaLibraryType? = null,
    val errorMessage: String? = null,
    val currentPage: Int = 0,
    val isRandomMode: Boolean = false,
    val isMuted: Boolean = false,
    val isPlaying: Boolean = true,
    val isFavoriteUpdating: Boolean = false,
    val noticeMessage: String? = null,
    val browsingLibraryId: String? = null,
    val browsingLibraryName: String? = null,
    val browsingVideos: List<VideoItem> = emptyList(),
    val isBrowsingLoading: Boolean = false,
    val browsingErrorMessage: String? = null,
    val browsingCurrentPage: Int = 1,
    val browsingHasNextPage: Boolean = false,
    val searchSelectedLibraryId: String? = null,
    val searchSelectedLibraryType: MediaLibraryType? = null,
    val searchQuery: String = "",
    val searchResults: List<VideoItem> = emptyList(),
    val isSearchLoading: Boolean = false,
    val searchErrorMessage: String? = null
)
