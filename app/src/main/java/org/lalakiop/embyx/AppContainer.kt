package com.lalakiop.embyx

import android.content.Context
import com.lalakiop.embyx.data.local.HomeCacheStore
import com.lalakiop.embyx.data.local.SessionStore
import com.lalakiop.embyx.data.local.UiSettingsStore
import com.lalakiop.embyx.data.remote.ApiClientFactory
import com.lalakiop.embyx.data.remote.EmbyPlaybackApi
import com.lalakiop.embyx.data.repository.AuthRepositoryImpl
import com.lalakiop.embyx.data.repository.EmbyVideoRepository
import com.lalakiop.embyx.data.repository.VideoRepository
import com.lalakiop.embyx.domain.repository.AuthRepository
import com.lalakiop.embyx.domain.usecase.GetFeedUseCase
import com.lalakiop.embyx.domain.usecase.GetLibrariesUseCase
import com.lalakiop.embyx.domain.usecase.SetFavoriteUseCase
import com.lalakiop.embyx.player.PlayerRuntimeConfig

/**
 * 应用全局依赖容器
 * 统一创建、管理整个App的核心工具类（存储、网络、仓库、业务、播放器）
 */
class AppContainer(context: Context) {
    // 全局应用上下文（防止内存泄漏，所有组件共用）
    private val appContext = context.applicationContext

    // 会话存储：保存登录状态、用户ID、服务器地址、Token
    private val sessionStore = SessionStore(context.applicationContext)

    // UI设置存储：保存语言、主题等界面配置
    val uiSettingsStore = UiSettingsStore(context.applicationContext)

    // 首页缓存：缓存首页视频列表数据
    val homeCacheStore = HomeCacheStore(
        context = context.applicationContext,
        sessionStore = sessionStore
    )

    // API客户端工厂：创建网络请求工具，与Emby服务器通信
    private val apiClientFactory = ApiClientFactory()

    // 播放器运行时配置：管理视频播放器的缓存、加载参数
    private val playerRuntimeConfig = PlayerRuntimeConfig(appContext)

    // 认证仓库：处理登录、登出、会话验证等逻辑
    val authRepository: AuthRepository = AuthRepositoryImpl(
        sessionStore = sessionStore,
        apiClientFactory = apiClientFactory
    )

    // 视频仓库：处理视频、媒体库、收藏等核心业务数据
    val videoRepository: VideoRepository = EmbyVideoRepository(
        sessionStore = sessionStore,
        apiClientFactory = apiClientFactory
    )

    // 业务用例：获取首页推荐视频流
    val getFeedUseCase = GetFeedUseCase(videoRepository)
    // 业务用例：获取媒体库分类（电影/剧集等）
    val getLibrariesUseCase = GetLibrariesUseCase(videoRepository)
    // 业务用例：设置/取消视频收藏
    val setFavoriteUseCase = SetFavoriteUseCase(videoRepository)

    // 创建播放器加载控制器
    fun newPlayerLoadControl() = playerRuntimeConfig.newLoadControl()

    // 创建带缓存的媒体源工厂（播放视频核心）
    fun newCachedMediaSourceFactory() = playerRuntimeConfig.newMediaSourceFactory()

    // 获取视频缓存文件夹路径
    fun playerCacheDirectory() = playerRuntimeConfig.cacheDirectory()

    // 更新播放器缓存作用域（按服务器+用户隔离缓存）
    fun updatePlayerCacheScope(server: String, userId: String) {
        playerRuntimeConfig.setCacheScope(server = server, userId = userId)
    }
    
    // 创建播放状态报告API
    fun createPlaybackApi(server: String, token: String): EmbyPlaybackApi {
        return apiClientFactory.createPlaybackApi(server = server, token = token)
    }
}