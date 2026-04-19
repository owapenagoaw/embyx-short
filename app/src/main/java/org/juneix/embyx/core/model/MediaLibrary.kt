package com.lalakiop.embyx.core.model

enum class MediaLibraryType {
    FAVORITES,
    PLAYLIST,
    LIBRARY
}

data class MediaLibrary(
    val id: String,
    val name: String,
    val type: MediaLibraryType
)
