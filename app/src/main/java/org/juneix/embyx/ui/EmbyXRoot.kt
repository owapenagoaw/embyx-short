package com.lalakiop.embyx.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lalakiop.embyx.EmbyXApp
import com.lalakiop.embyx.data.local.ThemeMode
import com.lalakiop.embyx.data.local.PlaybackHistoryEntry
import com.lalakiop.embyx.ui.auth.AuthViewModel
import com.lalakiop.embyx.ui.auth.LoginScreen
import com.lalakiop.embyx.ui.debug.DebugMetricsOverlay
import com.lalakiop.embyx.ui.home.FavoritesScreen
import com.lalakiop.embyx.ui.home.FavoritesViewModel
import com.lalakiop.embyx.ui.home.HomeViewModel
import com.lalakiop.embyx.ui.home.LibraryScreen
import com.lalakiop.embyx.ui.home.PlayerFeedScreen
import com.lalakiop.embyx.ui.home.SearchScreen
import com.lalakiop.embyx.ui.profile.AboutScreen
import com.lalakiop.embyx.ui.profile.PlayerControlsSettingsScreen
import com.lalakiop.embyx.ui.profile.PlayerControlsConfigDraft
import com.lalakiop.embyx.ui.profile.PlaybackHistoryScreen
import com.lalakiop.embyx.ui.profile.ProfileScreen
import kotlinx.coroutines.launch

private object ProfileRoutes {
    const val history = "profile/history"
    const val about = "profile/about"
    const val playerControls = "profile/player-controls"
}

@Composable
fun EmbyXRoot() {
    val app = LocalContext.current.applicationContext as EmbyXApp
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.Factory(app.appContainer.authRepository)
    )
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val uiSettings by app.appContainer.uiSettingsStore.settingsFlow.collectAsStateWithLifecycle(
        initialValue = com.lalakiop.embyx.data.local.UiSettings()
    )
    val scope = rememberCoroutineScope()

    EmbyXTheme(themeMode = uiSettings.themeMode) {

        if (authState.isRestoring) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("正在恢复登录状态...")
            }
            return@EmbyXTheme
        }

        if (!authState.session.isLoggedIn) {
            LoginScreen(
                state = authState,
                onServerChange = authViewModel::onServerChange,
                onUsernameChange = authViewModel::onUsernameChange,
                onPasswordChange = authViewModel::onPasswordChange,
                onLoginClick = authViewModel::login
            )
            return@EmbyXTheme
        }

        var playerCacheScopeReady by remember(authState.session.server, authState.session.userId) {
            mutableStateOf(false)
        }

        LaunchedEffect(authState.session.server, authState.session.userId) {
            app.appContainer.updatePlayerCacheScope(
                server = authState.session.server,
                userId = authState.session.userId
            )
            playerCacheScopeReady = true
        }

        if (!playerCacheScopeReady) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("正在初始化播放器...")
            }
            return@EmbyXTheme
        }

        val homeViewModel: HomeViewModel = viewModel(
            factory = HomeViewModel.Factory(
                getFeedUseCase = app.appContainer.getFeedUseCase,
                getLibrariesUseCase = app.appContainer.getLibrariesUseCase,
                setFavoriteUseCase = app.appContainer.setFavoriteUseCase,
                uiSettingsStore = app.appContainer.uiSettingsStore,
                homeCacheStore = app.appContainer.homeCacheStore
            )
        )

        val favoritesViewModel: FavoritesViewModel = viewModel(
            factory = FavoritesViewModel.Factory(
                getFeedUseCase = app.appContainer.getFeedUseCase,
                homeCacheStore = app.appContainer.homeCacheStore
            )
        )
        val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
        val randomHistory by app.appContainer.homeCacheStore.randomHistoryFlow.collectAsStateWithLifecycle(
            initialValue = emptyList<PlaybackHistoryEntry>()
        )
        val sequentialHistory by app.appContainer.homeCacheStore.sequentialHistoryFlow.collectAsStateWithLifecycle(
            initialValue = emptyList<PlaybackHistoryEntry>()
        )
        val playlists by app.appContainer.homeCacheStore.playlistsFlow.collectAsStateWithLifecycle(
            initialValue = emptyList()
        )

        val navController = rememberNavController()
        var homeFullscreen by remember { mutableStateOf(false) }

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    if (!homeFullscreen) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 0.dp, vertical = 0.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp
                            ) {
                                NavigationBar(
                                    containerColor = Color.Transparent,
                                    tonalElevation = 0.dp
                                ) {
                                    BottomTabs.entries.forEach { tab ->
                                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                                        val currentDestination = navBackStackEntry?.destination
                                        val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true

                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = {
                                                navController.navigate(tab.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                                            label = { Text(tab.label) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = BottomTabs.Home.route
                ) {
                    composable(BottomTabs.Home.route) {
                        PlayerFeedScreen(
                            viewModel = homeViewModel,
                            contentPadding = innerPadding,
                            allowScreenOffPlayback = uiSettings.allowScreenOffPlayback,
                            onFullscreenChange = { fullscreen ->
                                homeFullscreen = fullscreen
                            }
                        )
                    }
                    composable(BottomTabs.Search.route) {
                        SearchScreen(
                            viewModel = homeViewModel,
                            contentPadding = innerPadding,
                            allowScreenOffPlayback = uiSettings.allowScreenOffPlayback
                        )
                    }
                    composable(BottomTabs.Library.route) {
                        LibraryScreen(
                            viewModel = homeViewModel,
                            contentPadding = innerPadding
                        )
                    }
                    composable(BottomTabs.Favorites.route) {
                        FavoritesScreen(
                            viewModel = favoritesViewModel,
                            contentPadding = innerPadding,
                            allowScreenOffPlayback = uiSettings.allowScreenOffPlayback
                        )
                    }
                    composable(BottomTabs.Profile.route) {
                        ProfileScreen(
                            session = authState.session,
                            themeMode = uiSettings.themeMode,
                            allowScreenOffPlayback = uiSettings.allowScreenOffPlayback,
                            debugOverlayEnabled = uiSettings.debugOverlayEnabled,
                            playlists = playlists.ifEmpty {
                                homeState.libraries.filter { it.type == com.lalakiop.embyx.core.model.MediaLibraryType.PLAYLIST }
                            },
                            randomHistory = randomHistory,
                            sequentialHistory = sequentialHistory,
                            onThemeModeChange = { mode: ThemeMode ->
                                scope.launch {
                                    app.appContainer.uiSettingsStore.setThemeMode(mode)
                                }
                            },
                            onScreenOffPlaybackChange = { enabled: Boolean ->
                                scope.launch {
                                    app.appContainer.uiSettingsStore.setAllowScreenOffPlayback(enabled)
                                }
                            },
                            onDebugOverlayEnabledChange = { enabled: Boolean ->
                                scope.launch {
                                    app.appContainer.uiSettingsStore.setDebugOverlayEnabled(enabled)
                                }
                            },
                            onOpenHistoryClick = {
                                navController.navigate(ProfileRoutes.history)
                            },
                            onOpenAboutClick = {
                                navController.navigate(ProfileRoutes.about)
                            },
                            onOpenPlayerControlsSettingsClick = {
                                navController.navigate(ProfileRoutes.playerControls)
                            },
                            contentPadding = innerPadding,
                            onLogoutClick = authViewModel::logout
                        )
                    }
                    composable(ProfileRoutes.history) {
                        PlaybackHistoryScreen(
                            randomHistory = randomHistory,
                            sequentialHistory = sequentialHistory,
                            contentPadding = innerPadding,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(ProfileRoutes.about) {
                        AboutScreen(
                            contentPadding = innerPadding,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(ProfileRoutes.playerControls) {
                        PlayerControlsSettingsScreen(
                            settings = uiSettings,
                            contentPadding = innerPadding,
                            onBack = { navController.popBackStack() },
                            onSave = { draft: PlayerControlsConfigDraft ->
                                scope.launch {
                                    app.appContainer.uiSettingsStore.setPlayerControlAreaAutoHide(
                                        topArea = draft.autoHideTop,
                                        rightArea = draft.autoHideRight,
                                        bottomArea = draft.autoHideBottom
                                    )
                                    app.appContainer.uiSettingsStore.setPlayerAutoHideDelayMs(draft.autoHideDelayMs)
                                    app.appContainer.uiSettingsStore.setPlayerSummonBand(draft.summonBand)
                                    app.appContainer.uiSettingsStore.setPlayerPauseBand(draft.pauseBand)
                                    navController.popBackStack()
                                }
                            }
                        )
                    }
                }
            }

            DebugMetricsOverlay(
                enabled = uiSettings.debugOverlayEnabled,
                cacheDir = app.appContainer.playerCacheDirectory()
            )
        }
    }
}

@Composable
private fun PlaceholderPage(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$title 页面框架已就绪",
            style = MaterialTheme.typography.headlineSmall
        )
    }
}
