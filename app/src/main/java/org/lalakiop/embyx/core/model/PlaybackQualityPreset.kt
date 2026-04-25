package com.lalakiop.embyx.core.model

data class PlaybackQualityPreset(
    val id: String,
    val label: String,
    val maxStreamingBitrate: Int,
    val maxWidth: Int
)

object PlaybackQualityPresets {
    val ORIGINAL = PlaybackQualityPreset(
        id = "original",
        label = "原画",
        maxStreamingBitrate = 140_000_000,
        maxWidth = 8192
    )
    val P1080_60M = PlaybackQualityPreset(
        id = "1080p_60m",
        label = "1080P 60M",
        maxStreamingBitrate = 60_000_000,
        maxWidth = 1920
    )
    val P1080_20M = PlaybackQualityPreset(
        id = "1080p_20m",
        label = "1080P 20M",
        maxStreamingBitrate = 20_000_000,
        maxWidth = 1920
    )
    val P1080_8M = PlaybackQualityPreset(
        id = "1080p_8m",
        label = "1080P 8M",
        maxStreamingBitrate = 8_000_000,
        maxWidth = 1920
    )
    val P720_4M = PlaybackQualityPreset(
        id = "720p_4m",
        label = "720P 4M",
        maxStreamingBitrate = 4_000_000,
        maxWidth = 1280
    )
    val P720_2M = PlaybackQualityPreset(
        id = "720p_2m",
        label = "720P 2M",
        maxStreamingBitrate = 2_000_000,
        maxWidth = 1280
    )

    val all: List<PlaybackQualityPreset> = listOf(
        ORIGINAL,
        P1080_60M,
        P1080_20M,
        P1080_8M,
        P720_4M,
        P720_2M
    )

    fun findById(id: String?): PlaybackQualityPreset {
        return all.firstOrNull { it.id == id } ?: ORIGINAL
    }
}
