package com.lalakiop.embyx.data.remote

import com.lalakiop.embyx.data.remote.model.PlaybackProgressInfo
import com.lalakiop.embyx.data.remote.model.PlaybackStartInfo
import com.lalakiop.embyx.data.remote.model.PlaybackStopInfo
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Emby 播放状态报告 API
 * 用于向服务器报告播放开始、进度和停止状态
 */
interface EmbyPlaybackApi {
    
    /**
     * 报告播放开始
     * POST /emby/Sessions/Playing
     */
    @POST("emby/Sessions/Playing")
    suspend fun reportPlaybackStarted(
        @Header("X-Emby-Token") token: String,
        @Body request: PlaybackStartInfo
    ): Response<Unit>
    
    /**
     * 报告播放进度
     * POST /emby/Sessions/Playing/Progress
     */
    @POST("emby/Sessions/Playing/Progress")
    suspend fun reportPlaybackProgress(
        @Header("X-Emby-Token") token: String,
        @Body request: PlaybackProgressInfo
    ): Response<Unit>
    
    /**
     * 心跳保持
     * POST /emby/Sessions/Playing/Ping?PlaySessionId={id}
     */
    @POST("emby/Sessions/Playing/Ping")
    suspend fun pingPlaybackSession(
        @Query("PlaySessionId") playSessionId: String,
        @Header("X-Emby-Token") token: String
    ): Response<Unit>
    
    /**
     * 报告播放停止
     * POST /emby/Sessions/Playing/Stopped
     */
    @POST("emby/Sessions/Playing/Stopped")
    suspend fun reportPlaybackStopped(
        @Header("X-Emby-Token") token: String,
        @Body request: PlaybackStopInfo
    ): Response<Unit>
    
    /**
     * 删除活跃的转码会话（切换清晰度时清理旧ffmpeg进程）
     * POST /emby/Videos/ActiveEncodings/Delete?DeviceId={deviceId}&PlaySessionId={playSessionId}
     */
    @POST("emby/Videos/ActiveEncodings/Delete")
    suspend fun deleteActiveEncoding(
        @Query("DeviceId") deviceId: String,
        @Query("PlaySessionId") playSessionId: String,
        @Header("X-Emby-Token") token: String
    ): Response<Unit>
}
