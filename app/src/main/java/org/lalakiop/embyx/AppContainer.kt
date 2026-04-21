package com.lalakiop.embyx

import android.content.Context
import com.lalakiop.embyx.data.local.HomeCacheStore
import com.lalakiop.embyx.data.local.SessionStore
import com.lalakiop.embyx.data.local.UiSettingsStore
import com.lalakiop.embyx.data.remote.ApiClientFactory
import com.lalakiop.embyx.data.repository.AuthRepositoryImpl
import com.lalakiop.embyx.data.repository.EmbyVideoRepository
import com.lalakiop.embyx.data.repository.VideoRepository
import com.lalakiop.embyx.domain.repository.AuthRepository
import com.lalakiop.embyx.domain.usecase.GetFeedUseCase
import com.lalakiop.embyx.domain.usecase.GetLibrariesUseCase
import com.lalakiop.embyx.domain.usecase.SetFavoriteUseCase
import com.lalakiop.embyx.player.PlayerRuntimeConfig

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val sessionStore = SessionStore(context.applicationContext)
    val uiSettingsStore = UiSettingsStore(context.applicationContext)
    val homeCacheStore = HomeCacheStore(
        context = context.applicationContext,
        sessionStore = sessionStore
    )
    private val apiClientFactory = ApiClientFactory()
    private val playerRuntimeConfig = PlayerRuntimeConfig(appContext)

    val authRepository: AuthRepository = AuthRepositoryImpl(
        sessionStore = sessionStore,
        apiClientFactory = apiClientFactory
    )

    val videoRepository: VideoRepository = EmbyVideoRepository(
        sessionStore = sessionStore,
        apiClientFactory = apiClientFactory
    )

    val getFeedUseCase = GetFeedUseCase(videoRepository)
    val getLibrariesUseCase = GetLibrariesUseCase(videoRepository)
    val setFavoriteUseCase = SetFavoriteUseCase(videoRepository)

    fun newPlayerLoadControl() = playerRuntimeConfig.newLoadControl()

    fun newCachedMediaSourceFactory() = playerRuntimeConfig.newMediaSourceFactory()

    fun playerCacheDirectory() = playerRuntimeConfig.cacheDirectory()

    fun updatePlayerCacheScope(server: String, userId: String) {
        playerRuntimeConfig.setCacheScope(server = server, userId = userId)
    }
}
