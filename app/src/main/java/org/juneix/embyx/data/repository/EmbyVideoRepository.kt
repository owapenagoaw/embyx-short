package com.lalakiop.embyx.data.repository

import kotlinx.coroutines.flow.first
import com.lalakiop.embyx.core.model.MediaLibrary
import com.lalakiop.embyx.core.model.MediaLibraryType
import com.lalakiop.embyx.core.model.PlaybackQualityPreset
import com.lalakiop.embyx.core.model.ResolvedPlaybackStream
import com.lalakiop.embyx.core.model.VideoItem
import com.lalakiop.embyx.data.local.SessionStore
import com.lalakiop.embyx.data.remote.ApiClientFactory
import com.lalakiop.embyx.data.remote.model.CodecConditionRequest
import com.lalakiop.embyx.data.remote.model.CodecProfileRequest
import com.lalakiop.embyx.data.remote.model.DeviceProfileRequest
import com.lalakiop.embyx.data.remote.model.DirectPlayProfileRequest
import com.lalakiop.embyx.data.remote.model.PlaybackInfoRequest
import com.lalakiop.embyx.data.remote.model.ResponseProfileRequest
import com.lalakiop.embyx.data.remote.model.SubtitleProfileRequest
import com.lalakiop.embyx.data.remote.model.TranscodingProfileRequest
import java.io.IOException

class EmbyVideoRepository(
    private val sessionStore: SessionStore,
    private val apiClientFactory: ApiClientFactory
) : VideoRepository {

    override suspend fun getFeed(
        parentId: String?,
        random: Boolean,
        favoritesOnly: Boolean,
        startIndex: Int,
        limit: Int
    ): Result<List<VideoItem>> {
        return runCatching {
            val session = sessionStore.sessionFlow.first()
            if (!session.isLoggedIn) {
                throw IllegalStateException("请先登录")
            }

            val api = apiClientFactory.createMediaApi(
                server = session.server,
                token = session.token
            )

            val response = api.getVideos(
                userId = session.userId,
                limit = limit,
                startIndex = startIndex,
                parentId = parentId,
                filters = if (favoritesOnly) "IsFavorite" else null,
                sortBy = if (random) "Random" else "DateCreated",
                sortOrder = if (random) null else "Descending"
            )

            if (!response.isSuccessful) {
                throw IllegalStateException("获取视频列表失败: HTTP ${response.code()}")
            }

            val body = response.body()
            val items = body?.items.orEmpty()
            items.map { dto ->
                val durationSec = dto.runTimeTicks?.div(10_000_000L)
                val streamUrl = "${session.server}/emby/Videos/${dto.id}/stream?Static=true&api_key=${session.token}"
                val imageUrl = "${session.server}/emby/Items/${dto.id}/Images/Primary?maxWidth=480&quality=90&api_key=${session.token}"

                VideoItem(
                    id = dto.id,
                    title = dto.name ?: "未命名视频",
                    streamUrl = streamUrl,
                    overview = dto.overview,
                    durationSec = durationSec,
                    imageUrl = imageUrl,
                    isFavorite = dto.userData?.isFavorite == true
                )
            }
        }.recoverCatching { throwable ->
            when (throwable) {
                is IOException -> throw IllegalStateException("网络连接失败，请检查网络或服务器地址")
                else -> throw throwable
            }
        }
    }

    override suspend fun getLibraries(): Result<List<MediaLibrary>> {
        return runCatching {
            val session = sessionStore.sessionFlow.first()
            if (!session.isLoggedIn) {
                throw IllegalStateException("请先登录")
            }

            val api = apiClientFactory.createMediaApi(
                server = session.server,
                token = session.token
            )

            val playlistsResponse = api.getPlaylists(userId = session.userId)
            val viewsResponse = api.getViews(userId = session.userId)

            val playlists = if (playlistsResponse.isSuccessful) {
                playlistsResponse.body()?.items.orEmpty().map {
                    MediaLibrary(
                        id = it.id,
                        name = it.name ?: "未命名播放列表",
                        type = MediaLibraryType.PLAYLIST
                    )
                }
            } else {
                emptyList()
            }

            val views = if (viewsResponse.isSuccessful) {
                viewsResponse.body()?.items.orEmpty()
                    .filterNot { dto ->
                        dto.collectionType == "playlists" || dto.collectionType == "boxsets"
                    }
                    .map {
                        MediaLibrary(
                            id = it.id,
                            name = it.name ?: "未命名媒体库",
                            type = MediaLibraryType.LIBRARY
                        )
                    }
            } else {
                emptyList()
            }

            buildList {
                addAll(playlists)
                addAll(views)
            }
        }.recoverCatching { throwable ->
            when (throwable) {
                is IOException -> throw IllegalStateException("网络连接失败，无法加载媒体库")
                else -> throw throwable
            }
        }
    }

    override suspend fun resolvePlaybackStream(
        itemId: String,
        preset: PlaybackQualityPreset,
        mediaSourceId: String?,
        startTimeTicks: Long
    ): Result<ResolvedPlaybackStream> {
        return runCatching {
            val session = sessionStore.sessionFlow.first()
            if (!session.isLoggedIn) {
                throw IllegalStateException("请先登录")
            }

            val api = apiClientFactory.createMediaApi(
                server = session.server,
                token = session.token
            )

            val response = api.postPlaybackInfo(
                itemId = itemId,
                userId = session.userId,
                startTimeTicks = startTimeTicks.coerceAtLeast(0L),
                mediaSourceId = mediaSourceId,
                maxStreamingBitrate = preset.maxStreamingBitrate,
                body = buildPlaybackInfoRequest(maxWidth = preset.maxWidth)
            )

            if (!response.isSuccessful) {
                throw IllegalStateException("切换清晰度失败: HTTP ${response.code()}")
            }

            val source = response.body()?.mediaSources.orEmpty().firstOrNull { src ->
                !src.transcodingUrl.isNullOrBlank() || !src.directStreamUrl.isNullOrBlank() || !src.path.isNullOrBlank()
            }
                ?: throw IllegalStateException("切换清晰度失败: 未返回可播放媒体源")

            val resolvedUrl = resolvePlaybackUrl(
                server = session.server,
                token = session.token,
                sourcePath = source.transcodingUrl ?: source.directStreamUrl ?: source.path
            ) ?: "${session.server}/emby/Videos/$itemId/stream?Static=true&api_key=${session.token}&MaxStreamingBitrate=${preset.maxStreamingBitrate}"

            if (resolvedUrl.isBlank()) {
                throw IllegalStateException("切换清晰度失败: 返回播放地址为空")
            }

            ResolvedPlaybackStream(
                streamUrl = resolvedUrl,
                mediaSourceId = source.id
            )
        }.recoverCatching { throwable ->
            when (throwable) {
                is IOException -> throw IllegalStateException("网络连接失败，无法切换清晰度")
                else -> throw throwable
            }
        }
    }

    override suspend fun setFavorite(itemId: String, favorite: Boolean): Result<Unit> {
        return runCatching {
            val session = sessionStore.sessionFlow.first()
            if (!session.isLoggedIn) {
                throw IllegalStateException("请先登录")
            }

            val api = apiClientFactory.createMediaApi(
                server = session.server,
                token = session.token
            )

            val response = if (favorite) {
                api.favoriteItem(userId = session.userId, itemId = itemId)
            } else {
                api.unfavoriteItem(userId = session.userId, itemId = itemId)
            }

            if (!response.isSuccessful) {
                throw IllegalStateException("收藏操作失败: HTTP ${response.code()}")
            }
        }.recoverCatching { throwable ->
            when (throwable) {
                is IOException -> throw IllegalStateException("网络连接失败，收藏状态未同步")
                else -> throw throwable
            }
        }
    }

    private fun buildPlaybackInfoRequest(maxWidth: Int): PlaybackInfoRequest {
        return PlaybackInfoRequest(
            deviceProfile = DeviceProfileRequest(
                maxStaticBitrate = 140_000_000,
                maxStreamingBitrate = 140_000_000,
                musicStreamingTranscodingBitrate = 192_000,
                directPlayProfiles = listOf(
                    DirectPlayProfileRequest(
                        container = "mp4,m4v",
                        type = "Video",
                        videoCodec = "h264,hevc,av1,vp8,vp9",
                        audioCodec = "mp3,aac,opus,flac,vorbis"
                    ),
                    DirectPlayProfileRequest(
                        container = "mkv",
                        type = "Video",
                        videoCodec = "h264,hevc,av1,vp8,vp9",
                        audioCodec = "mp3,aac,opus,flac,vorbis"
                    )
                ),
                transcodingProfiles = listOf(
                    TranscodingProfileRequest(
                        container = "ts",
                        type = "Video",
                        audioCodec = "mp3,aac",
                        videoCodec = "hevc,h264,av1",
                        context = "Streaming",
                        protocol = "hls"
                    )
                ),
                codecProfiles = listOf(
                    CodecProfileRequest(
                        type = "Video",
                        codec = "h264",
                        conditions = listOf(
                            CodecConditionRequest(
                                condition = "LessThanEqual",
                                property = "VideoLevel",
                                value = "62"
                            ),
                            CodecConditionRequest(
                                condition = "LessThanEqual",
                                property = "Width",
                                value = maxWidth.toString()
                            )
                        )
                    ),
                    CodecProfileRequest(
                        type = "Video",
                        codec = "hevc",
                        conditions = listOf(
                            CodecConditionRequest(
                                condition = "EqualsAny",
                                property = "VideoCodecTag",
                                value = "hvc1|hev1|hevc|hdmv"
                            ),
                            CodecConditionRequest(
                                condition = "LessThanEqual",
                                property = "Width",
                                value = maxWidth.toString()
                            )
                        )
                    )
                ),
                subtitleProfiles = listOf(
                    SubtitleProfileRequest(
                        format = "vtt",
                        method = "Hls"
                    )
                ),
                responseProfiles = listOf(
                    ResponseProfileRequest(
                        type = "Video",
                        container = "m4v",
                        mimeType = "video/mp4"
                    )
                )
            )
        )
    }

    private fun resolvePlaybackUrl(
        server: String,
        token: String,
        sourcePath: String?
    ): String? {
        val raw = sourcePath?.takeIf { it.isNotBlank() } ?: return null
        val absolute = if (raw.startsWith("http://") || raw.startsWith("https://")) {
            raw
        } else {
            val normalizedServer = server.trimEnd('/')
            val normalizedPath = if (raw.startsWith('/')) raw else "/$raw"
            "$normalizedServer$normalizedPath"
        }
        return if (absolute.contains("api_key=")) {
            absolute
        } else if (absolute.contains('?')) {
            "$absolute&api_key=$token"
        } else {
            "$absolute?api_key=$token"
        }
    }
}
