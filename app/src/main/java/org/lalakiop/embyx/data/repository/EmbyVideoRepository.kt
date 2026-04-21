package com.lalakiop.embyx.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
import kotlin.random.Random

class EmbyVideoRepository(
    private val sessionStore: SessionStore,
    private val apiClientFactory: ApiClientFactory
) : VideoRepository {

    private data class WeightedLibrary(
        val id: String,
        val weight: Int
    )

    private data class LibraryWeightsCache(
        val scopeKey: String,
        val weightsByLibraryId: Map<String, Int>,
        val updatedAtMs: Long
    )

    private var libraryWeightsCache: LibraryWeightsCache? = null

    companion object {
        private const val LIBRARY_WEIGHTS_CACHE_TTL_MS = 10 * 60 * 1000L
    }

    override suspend fun getFeed(
        parentId: String?,
        random: Boolean,
        favoritesOnly: Boolean,
        startIndex: Int,
        limit: Int,
        searchTerm: String?
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

            val effectiveParentId = if (random && parentId == null && !favoritesOnly) {
                pickWeightedRandomLibraryId(
                    api = api,
                    userId = session.userId,
                    scopeKey = sessionScopeKey(session.server, session.userId)
                )
            } else {
                parentId
            }

            val response = api.getVideos(
                userId = session.userId,
                limit = limit,
                startIndex = startIndex,
                parentId = effectiveParentId,
                filters = if (favoritesOnly) "IsFavorite" else null,
                sortBy = if (random) "Random" else "DateCreated",
                sortOrder = if (random) null else "Descending",
                searchTerm = searchTerm?.trim()?.takeIf { it.isNotBlank() },
                enableTotalRecordCount = true,
                randomSeed = if (random) Random.nextInt() else null
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

            fetchPlayableLibraries(api = api, userId = session.userId)
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

    private suspend fun fetchPlayableLibraries(
        api: com.lalakiop.embyx.data.remote.EmbyMediaApi,
        userId: String
    ): List<MediaLibrary> {
        val playlistsResponse = api.getPlaylists(userId = userId)
        val viewsResponse = api.getViews(userId = userId)

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

        return buildList {
            addAll(playlists)
            addAll(views)
        }
    }

    private suspend fun pickWeightedRandomLibraryId(
        api: com.lalakiop.embyx.data.remote.EmbyMediaApi,
        userId: String,
        scopeKey: String
    ): String? {
        val libraries = fetchPlayableLibraries(api = api, userId = userId)
        if (libraries.isEmpty()) {
            return null
        }

        val weightsByLibraryId = resolveLibraryWeights(
            api = api,
            userId = userId,
            scopeKey = scopeKey,
            libraries = libraries
        )

        val weightedLibraries = libraries.mapNotNull { library ->
            val weight = (weightsByLibraryId[library.id] ?: 0).coerceAtLeast(0)
            if (weight <= 0) {
                null
            } else {
                WeightedLibrary(id = library.id, weight = weight)
            }
        }

        if (weightedLibraries.isEmpty()) {
            return null
        }

        val totalWeight = weightedLibraries.sumOf { it.weight }
        if (totalWeight <= 0) {
            return weightedLibraries.random().id
        }

        var cursor = Random.nextInt(totalWeight)
        weightedLibraries.forEach { candidate ->
            cursor -= candidate.weight
            if (cursor < 0) {
                return candidate.id
            }
        }

        return weightedLibraries.last().id
    }

    private suspend fun resolveLibraryWeights(
        api: com.lalakiop.embyx.data.remote.EmbyMediaApi,
        userId: String,
        scopeKey: String,
        libraries: List<MediaLibrary>
    ): Map<String, Int> {
        val now = System.currentTimeMillis()
        val cached = libraryWeightsCache
        val sameScope = cached?.scopeKey == scopeKey
        val hasAllLibraries = cached?.weightsByLibraryId?.keys?.containsAll(libraries.map { it.id }) == true
        val cacheValid = cached != null && sameScope && hasAllLibraries && now - cached.updatedAtMs <= LIBRARY_WEIGHTS_CACHE_TTL_MS
        if (cacheValid) {
            return cached!!.weightsByLibraryId
        }

        val refreshed = coroutineScope {
            libraries
                .map { library ->
                    async {
                        library.id to fetchLibraryItemCount(
                            api = api,
                            userId = userId,
                            parentId = library.id
                        )
                    }
                }
                .awaitAll()
                .toMap()
        }

        libraryWeightsCache = LibraryWeightsCache(
            scopeKey = scopeKey,
            weightsByLibraryId = refreshed,
            updatedAtMs = now
        )

        return refreshed
    }

    private suspend fun fetchLibraryItemCount(
        api: com.lalakiop.embyx.data.remote.EmbyMediaApi,
        userId: String,
        parentId: String
    ): Int {
        return runCatching {
            val response = api.getVideos(
                userId = userId,
                parentId = parentId,
                limit = 1,
                startIndex = 0,
                sortBy = "DateCreated",
                sortOrder = "Descending",
                enableTotalRecordCount = true,
                randomSeed = null
            )

            if (!response.isSuccessful) {
                return 0
            }

            val body = response.body()
            (body?.totalRecordCount ?: body?.items?.size ?: 0).coerceAtLeast(0)
        }.getOrDefault(0)
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

    private fun sessionScopeKey(server: String, userId: String): String {
        return "${server.trim().trimEnd('/').lowercase()}|${userId.trim().lowercase()}"
    }
}
