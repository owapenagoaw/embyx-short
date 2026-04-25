package com.lalakiop.embyx.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * 播放开始信息
 */
data class PlaybackStartInfo(
    @SerializedName("ItemId")
    val itemId: String,
    
    @SerializedName("PlaySessionId")
    val playSessionId: String,
    
    @SerializedName("MediaSourceId")
    val mediaSourceId: String? = null,
    
    @SerializedName("LiveStreamId")
    val liveStreamId: String? = null,
    
    @SerializedName("PositionTicks")
    val positionTicks: Long = 0L,
    
    @SerializedName("IsPaused")
    val isPaused: Boolean = false,
    
    @SerializedName("IsMuted")
    val isMuted: Boolean = false,
    
    @SerializedName("CanSeek")
    val canSeek: Boolean = true,
    
    @SerializedName("AudioStreamIndex")
    val audioStreamIndex: Int = 1,
    
    @SerializedName("SubtitleStreamIndex")
    val subtitleStreamIndex: Int = -1,
    
    @SerializedName("VolumeLevel")
    val volumeLevel: Int = 100,
    
    @SerializedName("PlayMethod")
    val playMethod: String = "DirectPlay",
    
    @SerializedName("PlaylistIndex")
    val playlistIndex: Int = 0,
    
    @SerializedName("PlaylistLength")
    val playlistLength: Int = 1,
    
    @SerializedName("RepeatMode")
    val repeatMode: String = "RepeatNone",
    
    @SerializedName("Shuffle")
    val shuffle: Boolean = false,
    
    @SerializedName("SubtitleOffset")
    val subtitleOffset: Int = 0,
    
    @SerializedName("PlaybackRate")
    val playbackRate: Double = 1.0,
    
    @SerializedName("RunTimeTicks")
    val runTimeTicks: Long? = null,
    
    @SerializedName("PlaybackStartTimeTicks")
    val playbackStartTimeTicks: Long = 0L,
    
    @SerializedName("AspectRatio")
    val aspectRatio: String? = null,
    
    @SerializedName("Brightness")
    val brightness: Int? = null,
    
    @SerializedName("EventName")
    val eventName: String? = null,
    
    @SerializedName("NowPlayingQueue")
    val nowPlayingQueue: List<QueueItem>? = null,
    
    @SerializedName("PlaylistItemIds")
    val playlistItemIds: List<String>? = null,
    
    @SerializedName("PlaylistItemId")
    val playlistItemId: String? = null,
    
    @SerializedName("SessionId")
    val sessionId: String? = null,
    
    // ⚠️ 新增：补充网页端有的缓冲和可seek范围字段
    @SerializedName("BufferedRanges")
    val bufferedRanges: List<List<Long>>? = null,  // [[start, end], ...]
    
    @SerializedName("SeekableRanges")
    val seekableRanges: List<List<Long>>? = null  // [[start, end], ...]
)

/**
 * 播放进度信息
 */
data class PlaybackProgressInfo(
    @SerializedName("ItemId")
    val itemId: String,
    
    @SerializedName("PlaySessionId")
    val playSessionId: String,
    
    @SerializedName("MediaSourceId")
    val mediaSourceId: String? = null,
    
    @SerializedName("LiveStreamId")
    val liveStreamId: String? = null,
    
    @SerializedName("PositionTicks")
    val positionTicks: Long,
    
    @SerializedName("IsPaused")
    val isPaused: Boolean = false,
    
    @SerializedName("IsMuted")
    val isMuted: Boolean = false,
    
    @SerializedName("CanSeek")
    val canSeek: Boolean = true,
    
    @SerializedName("AudioStreamIndex")
    val audioStreamIndex: Int = 1,
    
    @SerializedName("SubtitleStreamIndex")
    val subtitleStreamIndex: Int = -1,
    
    @SerializedName("VolumeLevel")
    val volumeLevel: Int = 100,
    
    @SerializedName("PlayMethod")
    val playMethod: String = "DirectPlay",
    
    @SerializedName("PlaylistIndex")
    val playlistIndex: Int = 0,
    
    @SerializedName("PlaylistLength")
    val playlistLength: Int = 1,
    
    @SerializedName("RepeatMode")
    val repeatMode: String = "RepeatNone",
    
    @SerializedName("Shuffle")
    val shuffle: Boolean = false,
    
    @SerializedName("SubtitleOffset")
    val subtitleOffset: Int = 0,
    
    @SerializedName("PlaybackRate")
    val playbackRate: Double = 1.0,
    
    @SerializedName("RunTimeTicks")
    val runTimeTicks: Long? = null,
    
    @SerializedName("PlaybackStartTimeTicks")
    val playbackStartTimeTicks: Long = 0L,
    
    @SerializedName("AspectRatio")
    val aspectRatio: String? = null,
    
    @SerializedName("Brightness")
    val brightness: Int? = null,
    
    @SerializedName("EventName")
    val eventName: String,  // 必需字段
    
    @SerializedName("NowPlayingQueue")
    val nowPlayingQueue: List<QueueItem>? = null,
    
    @SerializedName("PlaylistItemIds")
    val playlistItemIds: List<String>? = null,
    
    @SerializedName("PlaylistItemId")
    val playlistItemId: String? = null,
    
    @SerializedName("SessionId")
    val sessionId: String? = null,
    
    // ⚠️ 新增：补充网页端有的缓冲和可seek范围字段
    @SerializedName("BufferedRanges")
    val bufferedRanges: List<List<Long>>? = null,  // [[start, end], ...]
    
    @SerializedName("SeekableRanges")
    val seekableRanges: List<List<Long>>? = null  // [[start, end], ...]
)

/**
 * 播放停止信息
 */
data class PlaybackStopInfo(
    @SerializedName("ItemId")
    val itemId: String,
    
    @SerializedName("PlaySessionId")
    val playSessionId: String,
    
    @SerializedName("MediaSourceId")
    val mediaSourceId: String? = null,
    
    @SerializedName("LiveStreamId")
    val liveStreamId: String? = null,
    
    @SerializedName("PositionTicks")
    val positionTicks: Long,
    
    @SerializedName("NextMediaType")
    val nextMediaType: String? = null,
    
    @SerializedName("IsAutomated")
    val isAutomated: Boolean = false,
    
    @SerializedName("Failed")
    val failed: Boolean = false,
    
    @SerializedName("NowPlayingQueue")
    val nowPlayingQueue: List<QueueItem>? = null,
    
    @SerializedName("PlaylistIndex")
    val playlistIndex: Int = 0,
    
    @SerializedName("PlaylistLength")
    val playlistLength: Int = 1,
    
    @SerializedName("PlaylistItemId")
    val playlistItemId: String? = null,
    
    @SerializedName("SessionId")
    val sessionId: String? = null,
    
    // ⚠️ 新增：补充网页端有的关键字段，与官方客户端保持一致
    @SerializedName("IsPaused")
    val isPaused: Boolean = true,
    
    @SerializedName("PlayMethod")
    val playMethod: String? = null,  // "DirectPlay", "DirectStream", "Transcode"
    
    @SerializedName("CanSeek")
    val canSeek: Boolean = true,
    
    @SerializedName("AudioStreamIndex")
    val audioStreamIndex: Int? = null,
    
    @SerializedName("SubtitleStreamIndex")
    val subtitleStreamIndex: Int? = null,
    
    @SerializedName("VolumeLevel")
    val volumeLevel: Int? = null,
    
    @SerializedName("IsMuted")
    val isMuted: Boolean? = null,
    
    @SerializedName("PlaybackRate")
    val playbackRate: Float? = null,
    
    @SerializedName("MaxStreamingBitrate")
    val maxStreamingBitrate: Int? = null,
    
    @SerializedName("PlaybackStartTimeTicks")
    val playbackStartTimeTicks: Long? = null,
    
    @SerializedName("RepeatMode")
    val repeatMode: String? = null,  // "RepeatNone", "RepeatAll", "RepeatOne"
    
    @SerializedName("Shuffle")
    val shuffle: Boolean? = null,
    
    @SerializedName("BufferedRanges")
    val bufferedRanges: List<List<Long>>? = null,  // [[start, end], ...]
    
    @SerializedName("SeekableRanges")
    val seekableRanges: List<List<Long>>? = null  // [[start, end], ...]
)

/**
 * 播放队列项
 */
data class QueueItem(
    @SerializedName("Id")
    val id: Long,
    
    @SerializedName("PlaylistItemId")
    val playlistItemId: String? = null
)

/**
 * 播放方法枚举
 */
object PlayMethod {
    const val DIRECT_PLAY = "DirectPlay"
    const val DIRECT_STREAM = "DirectStream"
    const val TRANSCODE = "Transcode"
}

/**
 * 进度事件类型枚举
 */
object ProgressEvent {
    const val TIME_UPDATE = "TimeUpdate"
    const val PAUSE = "Pause"
    const val UNPAUSE = "Unpause"
    const val VOLUME_CHANGE = "VolumeChange"
    const val REPEAT_MODE_CHANGE = "RepeatModeChange"
    const val AUDIO_TRACK_CHANGE = "AudioTrackChange"
    const val SUBTITLE_TRACK_CHANGE = "SubtitleTrackChange"
    const val PLAYLIST_ITEM_MOVE = "PlaylistItemMove"
    const val PLAYLIST_ITEM_REMOVE = "PlaylistItemRemove"
    const val PLAYLIST_ITEM_ADD = "PlaylistItemAdd"
    const val QUALITY_CHANGE = "QualityChange"
    const val SUBTITLE_OFFSET_CHANGE = "SubtitleOffsetChange"
    const val PLAYBACK_RATE_CHANGE = "PlaybackRateChange"
}
