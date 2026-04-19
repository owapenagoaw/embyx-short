package com.lalakiop.embyx.core.model

data class VideoItem(
    val id: String,
    val title: String,
    val streamUrl: String,
    val overview: String? = null,
    val durationSec: Long? = null,
    val imageUrl: String? = null,
    val isFavorite: Boolean = false
)
