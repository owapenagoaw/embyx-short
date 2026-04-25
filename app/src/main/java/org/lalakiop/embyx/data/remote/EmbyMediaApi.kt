package com.lalakiop.embyx.data.remote

import com.lalakiop.embyx.data.remote.model.ItemsResponse
import com.lalakiop.embyx.data.remote.model.PlaybackInfoRequest
import com.lalakiop.embyx.data.remote.model.PlaybackInfoResponse
import com.lalakiop.embyx.data.remote.model.ViewsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface EmbyMediaApi {
    @GET("emby/Users/{userId}/Items")
    suspend fun getVideos(
        @Path("userId") userId: String,
        @Query("Recursive") recursive: Boolean = true,
        @Query("IncludeItemTypes") includeItemTypes: String = "Movie,Episode,Video",
        @Query("Limit") limit: Int = 99,
        @Query("StartIndex") startIndex: Int = 0,
        @Query("Fields") fields: String = "Overview,RunTimeTicks,ImageTags,UserData",
        @Query("ParentId") parentId: String? = null,
        @Query("Filters") filters: String? = null,
        @Query("SortBy") sortBy: String? = "DateCreated",
        @Query("SortOrder") sortOrder: String? = "Descending",
        @Query("SearchTerm") searchTerm: String? = null,
        @Query("EnableTotalRecordCount") enableTotalRecordCount: Boolean = true,
        @Query("RandomSeed") randomSeed: Int? = null
    ): Response<ItemsResponse>

    @GET("emby/Users/{userId}/Views")
    suspend fun getViews(
        @Path("userId") userId: String
    ): Response<ViewsResponse>

    @POST("emby/Items/{itemId}/PlaybackInfo")
    suspend fun postPlaybackInfo(
        @Path("itemId") itemId: String,
        @Query("UserId") userId: String,
        @Query("StartTimeTicks") startTimeTicks: Long = 0L,
        @Query("IsPlayback") isPlayback: Boolean = true,
        @Query("AutoOpenLiveStream") autoOpenLiveStream: Boolean = true,
        @Query("AudioStreamIndex") audioStreamIndex: Int = 1,
        @Query("SubtitleStreamIndex") subtitleStreamIndex: Int = -1,
        @Query("MediaSourceId") mediaSourceId: String? = null,
        @Query("reqformat") reqFormat: String = "json",
        // ⚠️ 关键修复：移除MaxStreamingBitrate查询参数，只使用Body中的DeviceProfile
        @Query("CurrentPlaySessionId") currentPlaySessionId: String? = null,  // ⚠️ 切换清晰度时传入旧会话ID
        @Body body: PlaybackInfoRequest
    ): Response<PlaybackInfoResponse>

    @GET("emby/Users/{userId}/Items")
    suspend fun getPlaylists(
        @Path("userId") userId: String,
        @Query("Recursive") recursive: Boolean = true,
        @Query("IncludeItemTypes") includeItemTypes: String = "Playlist"
    ): Response<ItemsResponse>

    @POST("emby/Users/{userId}/FavoriteItems/{itemId}")
    suspend fun favoriteItem(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String
    ): Response<Unit>

    @DELETE("emby/Users/{userId}/FavoriteItems/{itemId}")
    suspend fun unfavoriteItem(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String
    ): Response<Unit>
}
