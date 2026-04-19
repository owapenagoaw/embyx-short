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
    val P1440_40M = PlaybackQualityPreset(
        id = "1440p_40m",
        label = "1440P 40M",
        maxStreamingBitrate = 40_000_000,
        maxWidth = 2560
    )
    val P1440_20M = PlaybackQualityPreset(
        id = "1440p_20m",
        label = "1440P 20M",
        maxStreamingBitrate = 20_000_000,
        maxWidth = 2560
    )
    val P1080_20M = PlaybackQualityPreset(
        id = "1080p_20m",
        label = "1080P 20M",
        maxStreamingBitrate = 20_000_000,
        maxWidth = 1920
    )
    val P1080_10M = PlaybackQualityPreset(
        id = "1080p_10m",
        label = "1080P 10M",
        maxStreamingBitrate = 10_000_000,
        maxWidth = 1920
    )
    val P720_10M = PlaybackQualityPreset(
        id = "720p_10m",
        label = "720P 10M",
        maxStreamingBitrate = 10_000_000,
        maxWidth = 1280
    )
    val P720_5M = PlaybackQualityPreset(
        id = "720p_5m",
        label = "720P 5M",
        maxStreamingBitrate = 5_000_000,
        maxWidth = 1280
    )

    val all: List<PlaybackQualityPreset> = listOf(
        ORIGINAL,
        P1440_40M,
        P1440_20M,
        P1080_20M,
        P1080_10M,
        P720_10M,
        P720_5M
    )

    fun findById(id: String?): PlaybackQualityPreset {
        return all.firstOrNull { it.id == id } ?: ORIGINAL
    }
}
