package com.lalakiop.embyx.core.playback

import android.util.Log
import com.lalakiop.embyx.data.remote.EmbyPlaybackApi
import com.lalakiop.embyx.data.remote.model.PlaybackProgressInfo
import com.lalakiop.embyx.data.remote.model.PlaybackStartInfo
import com.lalakiop.embyx.data.remote.model.PlaybackStopInfo
import com.lalakiop.embyx.data.remote.model.PlayMethod
import com.lalakiop.embyx.data.remote.model.ProgressEvent
import kotlinx.coroutines.*
import java.util.UUID

/**
 * 播放状态报告器
 * 负责向Emby服务器报告播放开始、进度和停止状态
 */
class PlaybackReporter(
    private val playbackApi: EmbyPlaybackApi,
    private val token: String,
    private val userId: String,
    private val deviceId: String = "Android-EmbyX"  // ⚠️ 新增：设备ID，用于删除转码会话
) {
    private var playSessionId: String = ""
    private var currentItemId: String = ""
    private var currentMediaSourceId: String? = null
    private var reportingJob: Job? = null
    private var pingJob: Job? = null
    
    // 当前位置（由调用方定期更新）
    @Volatile
    private var currentPositionMs: Long = 0L
    
    // 检查是否已登录
    private val isLoggedIn: Boolean
        get() = token.isNotBlank() && userId.isNotBlank()
    
    companion object {
        private const val TAG = "PlaybackReporter"
        private const val PROGRESS_INTERVAL_MS = 10_000L  // 10秒
        private const val PING_INTERVAL_MS = 30_000L      // 30秒
    }
    
    /**
     * 开始播放报告
     * @param itemId 媒体项ID
     * @param mediaSourceId 媒体源ID
     * @param positionMs 起始位置（毫秒）
     * @param playMethod 播放方法
     * @param playSessionId 可选的PlaySessionId（如果由服务器生成则传入，否则自动生成）
     */
    fun onStart(
        itemId: String,
        mediaSourceId: String?,
        positionMs: Long = 0L,
        playMethod: String = PlayMethod.DIRECT_PLAY,
        playSessionId: String? = null  // ⚠️ 新增：可选的服务器生成的会话ID
    ) {
        // 如果未登录，不执行任何操作
        if (!isLoggedIn) {
            Log.d(TAG, "Skipping playback report: not logged in")
            return
        }
        
        // ⚠️ 关键修复：如果已有活跃会话，先停止它
        if (this.playSessionId.isNotEmpty() && currentItemId.isNotEmpty()) {
            Log.w(TAG, "New session starting while old session active! Stopping old session: itemId=$currentItemId, sessionId=${this.playSessionId}")
            // 立即停止旧会话（同步调用，确保ffmpeg进程被清理）
            stopCurrentSessionSync()
        }
        
        currentItemId = itemId
        currentMediaSourceId = mediaSourceId
        currentPositionMs = positionMs  // 初始化位置
        // ⚠️ 关键：优先使用服务器生成的PlaySessionId，否则自动生成
        this.playSessionId = playSessionId ?: UUID.randomUUID().toString()
        
        Log.d(TAG, "Playback started: itemId=$itemId, sessionId=${this.playSessionId}, fromServer=${playSessionId != null}")
        
        // 报告播放开始
        reportStart(itemId, mediaSourceId, positionMs, playMethod)
        
        // 启动定时进度报告
        startPeriodicReporting()
        
        // 启动心跳保持
        startPing()
    }
    
    /**
     * 更新当前播放位置（由调用方在主线程定期调用）
     * @param positionMs 当前位置（毫秒）
     */
    fun updatePosition(positionMs: Long) {
        currentPositionMs = positionMs
    }
    
    /**
     * 用户交互时报告进度
     * @param eventName 事件类型
     * @param positionMs 当前位置（毫秒）
     * @param isPaused 是否暂停
     */
    fun onUserAction(
        eventName: String,
        positionMs: Long,
        isPaused: Boolean = false
    ) {
        if (!isLoggedIn) return
        
        if (playSessionId.isEmpty()) {
            Log.w(TAG, "Cannot report progress: no active session")
            return
        }
        
        reportProgress(eventName, positionMs, isPaused)
    }
    
    /**
     * 停止播放报告
     * @param positionMs 停止时的位置（毫秒）
     * @param nextMediaType 下一个媒体类型（连续播放时）
     */
    fun onStop(positionMs: Long, nextMediaType: String? = null) {
        if (!isLoggedIn) return
        
        Log.d(TAG, "Playback stopped: itemId=$currentItemId, position=${positionMs}ms")
        
        // 先清理状态，让正在运行的协程提前返回
        val oldSessionId = playSessionId
        val oldItemId = currentItemId
        playSessionId = ""
        currentItemId = ""
        currentMediaSourceId = null
        
        // 取消定时任务
        reportingJob?.cancel()
        pingJob?.cancel()
        
        // 报告播放停止（使用保存的旧值）
        reportStoppedWithSession(positionMs, nextMediaType, oldSessionId, oldItemId)
    }
    
    /**
     * 恢复定时报告和心跳（用于页面重新可见时）
     * 不会创建新的PlaySessionId，只是重启后台任务
     */
    fun resumeReporting() {
        if (!isLoggedIn || playSessionId.isEmpty()) {
            Log.w(TAG, "Cannot resume: not logged in or no active session")
            return
        }
        
        Log.d(TAG, "Resuming periodic reporting and ping for session: $playSessionId")
        
        // 重新启动定时进度报告
        if (reportingJob?.isActive != true) {
            startPeriodicReporting()
        }
        
        // 重新启动心跳
        if (pingJob?.isActive != true) {
            startPing()
        }
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        reportingJob?.cancel()
        pingJob?.cancel()
    }
    
    /**
     * 切换清晰度时删除旧的转码会话
     * @param oldPlaySessionId 旧的会话ID
     */
    fun deleteOldTranscodingSession(oldPlaySessionId: String) {
        if (oldPlaySessionId.isEmpty()) {
            return
        }
        
        Log.d(TAG, "Deleting old transcoding session: $oldPlaySessionId")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = playbackApi.deleteActiveEncoding(
                    deviceId = deviceId,
                    playSessionId = oldPlaySessionId,
                    token = token
                )
                
                if (response.isSuccessful) {
                    Log.d(TAG, "Old transcoding session deleted: $oldPlaySessionId")
                } else {
                    Log.w(TAG, "Failed to delete old transcoding session: HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting old transcoding session", e)
            }
        }
    }
    
    /**
     * 获取当前PlaySessionId（用于切换清晰度时传入CurrentPlaySessionId）
     */
    fun getCurrentPlaySessionId(): String {
        return playSessionId
    }
    
    // ==================== 私有方法 ====================
    
    private fun reportStart(
        itemId: String,
        mediaSourceId: String?,
        positionMs: Long,
        playMethod: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = playbackApi.reportPlaybackStarted(
                    token = token,
                    request = PlaybackStartInfo(
                        itemId = itemId,
                        playSessionId = playSessionId,
                        mediaSourceId = mediaSourceId,
                        positionTicks = positionMs * 10_000L,  // ms → ticks
                        playMethod = playMethod,
                        canSeek = true,
                        isPaused = false
                    )
                )
                
                if (!response.isSuccessful) {
                    Log.w(TAG, "Failed to report playback start: HTTP ${response.code()}")
                } else {
                    Log.d(TAG, "Playback start reported successfully")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reporting playback start", e)
                // 静默失败，不影响播放体验
            }
        }
    }
    
    private fun startPeriodicReporting() {
        reportingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(PROGRESS_INTERVAL_MS)
                // 使用保存的位置值（由调用方在主线程更新）
                reportProgress(ProgressEvent.TIME_UPDATE, currentPositionMs, false)
            }
        }
    }
    
    private fun reportProgress(
        eventName: String,
        positionMs: Long,
        isPaused: Boolean
    ) {
        // 检查会话是否仍然有效
        if (playSessionId.isEmpty() || currentItemId.isEmpty()) {
            Log.d(TAG, "Skipping progress report: no active session")
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = playbackApi.reportPlaybackProgress(
                    token = token,
                    request = PlaybackProgressInfo(
                        itemId = currentItemId,
                        playSessionId = playSessionId,
                        mediaSourceId = currentMediaSourceId,
                        positionTicks = positionMs * 10_000L,  // ms → ticks
                        eventName = eventName,
                        isPaused = isPaused,
                        canSeek = true
                    )
                )
                
                if (!response.isSuccessful) {
                    Log.w(TAG, "Failed to report progress ($eventName): HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reporting progress ($eventName)", e)
                // 静默失败
            }
        }
    }
    
    private fun startPing() {
        pingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(PING_INTERVAL_MS)
                try {
                    val response = playbackApi.pingPlaybackSession(
                        playSessionId = playSessionId,
                        token = token
                    )
                    
                    if (!response.isSuccessful) {
                        Log.w(TAG, "Ping failed: HTTP ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error pinging session", e)
                }
            }
        }
    }
    
    private fun reportStopped(positionMs: Long, nextMediaType: String?) {
        // 检查会话是否仍然有效
        if (playSessionId.isEmpty() || currentItemId.isEmpty()) {
            Log.d(TAG, "Skipping stop report: no active session")
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = playbackApi.reportPlaybackStopped(
                    token = token,
                    request = PlaybackStopInfo(
                        itemId = currentItemId,
                        playSessionId = playSessionId,
                        positionTicks = positionMs * 10_000L,
                        nextMediaType = nextMediaType
                    )
                )
                
                if (!response.isSuccessful) {
                    Log.w(TAG, "Failed to report playback stop: HTTP ${response.code()}")
                } else {
                    Log.d(TAG, "Playback stop reported successfully")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reporting playback stop", e)
            }
        }
    }
    
    /**
     * 同步停止当前会话（用于切换视频时）
     * 这个方法会阻塞直到请求发送完成
     */
    private fun stopCurrentSessionSync() {
        if (playSessionId.isEmpty() || currentItemId.isEmpty()) {
            return
        }
        
        val oldSessionId = playSessionId
        val oldItemId = currentItemId
        val oldPosition = currentPositionMs
        
        Log.d(TAG, "Synchronously stopping session: itemId=$oldItemId, sessionId=$oldSessionId")
        
        // 取消当前的定时任务
        reportingJob?.cancel()
        pingJob?.cancel()
        
        // 使用runBlocking同步发送停止请求
        runBlocking(Dispatchers.IO) {
            try {
                // ⚠️ 关键修复：发送完整的停止信息，与网页端一致
                val stopResponse = playbackApi.reportPlaybackStopped(
                    token = token,
                    request = PlaybackStopInfo(
                        itemId = oldItemId,
                        playSessionId = oldSessionId,
                        positionTicks = oldPosition * 10_000L,
                        nextMediaType = null,
                        isPaused = true,
                        canSeek = true,
                        audioStreamIndex = 1,
                        subtitleStreamIndex = -1,
                        volumeLevel = 100,
                        isMuted = false,
                        playbackRate = 1.0f,
                        repeatMode = "RepeatNone",
                        shuffle = false,
                        playlistIndex = -1,
                        playlistLength = 0,
                        nowPlayingQueue = emptyList()
                    )
                )
                
                if (stopResponse.isSuccessful) {
                    Log.d(TAG, "Playback stop reported successfully: $oldSessionId")
                } else {
                    Log.w(TAG, "Failed to report playback stop: HTTP ${stopResponse.code()}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping old session", e)
                // 即使失败也继续，避免阻塞新会话的创建
            }
        }
        
        // 清空状态
        playSessionId = ""
        currentItemId = ""
        currentMediaSourceId = null
    }
    
    private fun reportStoppedWithSession(
        positionMs: Long,
        nextMediaType: String?,
        sessionId: String,
        itemId: String
    ) {
        if (sessionId.isEmpty() || itemId.isEmpty()) {
            Log.d(TAG, "Skipping stop report: empty session/item")
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // ⚠️ 关键修复：发送完整的停止信息，与网页端一致
                val stopResponse = playbackApi.reportPlaybackStopped(
                    token = token,
                    request = PlaybackStopInfo(
                        itemId = itemId,
                        playSessionId = sessionId,
                        positionTicks = positionMs * 10_000L,
                        nextMediaType = nextMediaType,
                        isPaused = true,  // 停止时通常是暂停状态
                        canSeek = true,
                        audioStreamIndex = 1,
                        subtitleStreamIndex = -1,
                        volumeLevel = 100,
                        isMuted = false,
                        playbackRate = 1.0f,
                        repeatMode = "RepeatNone",
                        shuffle = false,
                        playlistIndex = -1,  // 网页端退出时使用-1
                        playlistLength = 0,   // 网页端退出时使用0
                        nowPlayingQueue = emptyList()  // 空队列
                    )
                )
                
                if (stopResponse.isSuccessful) {
                    Log.d(TAG, "Playback stopped reported successfully")
                } else {
                    Log.w(TAG, "Failed to report playback stopped: HTTP ${stopResponse.code()}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error reporting playback stopped", e)
            }
        }
    }
}
