package com.lalakiop.embyx.ui.home

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import com.lalakiop.embyx.EmbyXApp
import com.lalakiop.embyx.core.model.MediaLibraryType
import com.lalakiop.embyx.core.model.PlaybackQualityPreset
import com.lalakiop.embyx.core.model.PlaybackQualityPresets
import com.lalakiop.embyx.data.local.PlaybackTouchBand
import com.lalakiop.embyx.data.local.UiSettings
import com.lalakiop.embyx.ui.debug.PlaybackDebugRegistry

@Composable
@OptIn(UnstableApi::class)
fun PlayerFeedScreen(
    viewModel: HomeViewModel,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    allowScreenOffPlayback: Boolean = false,
    onFullscreenChange: (Boolean) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val app = context.applicationContext as EmbyXApp
    val uiSettings by app.appContainer.uiSettingsStore.settingsFlow.collectAsStateWithLifecycle(
        initialValue = UiSettings()
    )
    val scope = rememberCoroutineScope()
    val activity = context.findActivity()
    val lifecycleOwner = LocalLifecycleOwner.current
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = contentPadding.calculateBottomPadding()
    val viewConfig = LocalViewConfiguration.current

    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekingFraction by remember { mutableFloatStateOf(0f) }
    var controlsVisible by remember { mutableStateOf(true) }
    var sourcePickerExpanded by remember { mutableStateOf(false) }
    var controlsAutoHideTick by remember { mutableIntStateOf(0) }
    var speedPanelVisible by remember { mutableStateOf(false) }
    var qualityPanelVisible by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var fitHeightMode by rememberSaveable { mutableStateOf(false) }
    var playbackSnapshotRestoreChecked by remember { mutableStateOf(false) }
    var selectedQualityId by rememberSaveable { mutableStateOf(PlaybackQualityPresets.ORIGINAL.id) }
    val qualityPresets = remember { PlaybackQualityPresets.all }
    var isQualityLoading by remember { mutableStateOf(false) }
    var qualityErrorMessage by remember { mutableStateOf<String?>(null) }
    val streamUrlOverrides = remember { mutableStateMapOf<String, String>() }
    val mediaSourceIdOverrides = remember { mutableStateMapOf<String, String>() }

    var viewportHeightPx by remember { mutableIntStateOf(1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var snapTargetOffsetY by remember { mutableFloatStateOf(0f) }
    var isDraggingPreview by remember { mutableStateOf(false) }
    var pendingPageDelta by remember { mutableIntStateOf(0) }
    var swipeBasePage by remember { mutableIntStateOf(0) }
    var suppressReboundAnimation by remember { mutableStateOf(false) }
    var switchMaskHold by remember { mutableStateOf(false) }
    var switchMaskToken by remember { mutableIntStateOf(0) }

    var displayPageState by remember { mutableIntStateOf(state.currentPage) }
    var awaitingViewModelPageSync by remember { mutableStateOf(false) }

    var activeSlot by remember { mutableIntStateOf(0) }
    var slot0Page by remember { mutableIntStateOf(-1) }
    var slot1Page by remember { mutableIntStateOf(-1) }
    var autoPlayEnabled by rememberSaveable { mutableStateOf(uiSettings.autoPlayHomeEnabled) }
    var preloadTargetPage by remember { mutableIntStateOf(-1) }
    var endedSignal by remember { mutableIntStateOf(0) }
    var endedSlot by remember { mutableIntStateOf(-1) }

    val player0 = remember {
        ExoPlayer.Builder(context)
            .setLoadControl(app.appContainer.newPlayerLoadControl())
            .setMediaSourceFactory(app.appContainer.newCachedMediaSourceFactory())
            .build()
            .apply {
                playWhenReady = true
                repeatMode = ExoPlayer.REPEAT_MODE_ONE
            }
    }
    val player1 = remember {
        ExoPlayer.Builder(context)
            .setLoadControl(app.appContainer.newPlayerLoadControl())
            .setMediaSourceFactory(app.appContainer.newCachedMediaSourceFactory())
            .build()
            .apply {
                playWhenReady = false
                repeatMode = ExoPlayer.REPEAT_MODE_ONE
            }
    }

    DisposableEffect(player0, player1) {
        val source0 = "home_p0"
        val source1 = "home_p1"

        val listener0 = object : AnalyticsListener {
            override fun onVideoDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long
            ) {
                PlaybackDebugRegistry.updateDecoder(source0, decoderName)
            }
        }

        val listener1 = object : AnalyticsListener {
            override fun onVideoDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long
            ) {
                PlaybackDebugRegistry.updateDecoder(source1, decoderName)
            }
        }

        val playbackStateListener0 = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    endedSlot = 0
                    endedSignal++
                }
            }
        }

        val playbackStateListener1 = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    endedSlot = 1
                    endedSignal++
                }
            }
        }

        player0.addAnalyticsListener(listener0)
        player1.addAnalyticsListener(listener1)
        player0.addListener(playbackStateListener0)
        player1.addListener(playbackStateListener1)

        onDispose {
            player0.removeAnalyticsListener(listener0)
            player1.removeAnalyticsListener(listener1)
            player0.removeListener(playbackStateListener0)
            player1.removeListener(playbackStateListener1)
            PlaybackDebugRegistry.clearSource(source0)
            PlaybackDebugRegistry.clearSource(source1)
        }
    }

    fun playerFor(slot: Int): ExoPlayer = if (slot == 0) player0 else player1

    fun pageFor(slot: Int): Int = if (slot == 0) slot0Page else slot1Page

    fun setPageFor(slot: Int, page: Int) {
        if (slot == 0) {
            slot0Page = page
        } else {
            slot1Page = page
        }
    }

    fun clearSlot(slot: Int) {
        val player = playerFor(slot)
        player.pause()
        player.playWhenReady = false
        player.stop()
        player.clearMediaItems()
        setPageFor(slot, -1)
    }

    fun prepareSlot(slot: Int, page: Int, play: Boolean) {
        val item = state.videos.getOrNull(page) ?: run {
            clearSlot(slot)
            return
        }
        val player = playerFor(slot)
        val playbackUrl = selectPlayableStreamUrl(
            candidateUrl = streamUrlOverrides[item.id] ?: item.streamUrl,
            fallbackUrl = item.streamUrl
        )
        if (pageFor(slot) != page) {
            val switched = runCatching {
                player.setMediaItem(MediaItem.fromUri(playbackUrl))
                player.prepare()
                true
            }.getOrElse { throwable ->
                val fallbackUrl = item.streamUrl
                streamUrlOverrides.remove(item.id)
                runCatching {
                    player.setMediaItem(MediaItem.fromUri(fallbackUrl))
                    player.prepare()
                    true
                }.getOrElse {
                    qualityErrorMessage = throwable.message ?: "切换清晰度失败"
                    clearSlot(slot)
                    false
                }
            }
            if (switched) {
                setPageFor(slot, page)
            }
        }
        player.playWhenReady = play
        if (play) {
            player.play()
        } else {
            player.pause()
        }
    }

    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    DisposableEffect(Unit) {
        onDispose {
            if (state.videos.isNotEmpty()) {
                viewModel.cachePlaybackSnapshot(
                    page = displayPageState,
                    positionMs = currentPositionMs,
                    wasPlaying = state.isPlaying
                )
            }
            activity?.let { applyPlayerFullscreenMode(it, false) }
            player0.release()
            player1.release()
        }
    }

    DisposableEffect(onFullscreenChange) {
        onDispose {
            onFullscreenChange(false)
        }
    }

    LaunchedEffect(isFullscreen, activity, onFullscreenChange) {
        onFullscreenChange(isFullscreen)
        activity?.let { applyPlayerFullscreenMode(it, isFullscreen) }
        fitHeightMode = isFullscreen
    }

    LaunchedEffect(uiSettings.preferredPlaybackPresetId) {
        selectedQualityId = PlaybackQualityPresets.findById(uiSettings.preferredPlaybackPresetId).id
    }

    LaunchedEffect(uiSettings.autoPlayHomeEnabled) {
        autoPlayEnabled = uiSettings.autoPlayHomeEnabled
    }

    LaunchedEffect(state.currentPage, state.videos.size) {
        if (state.videos.isEmpty()) {
            displayPageState = 0
            awaitingViewModelPageSync = false
            return@LaunchedEffect
        }
        val safePage = state.currentPage.coerceIn(0, state.videos.lastIndex)
        if (awaitingViewModelPageSync) {
            if (safePage == displayPageState) {
                awaitingViewModelPageSync = false
            }
        } else if (safePage != displayPageState) {
            displayPageState = safePage
        }
    }

    LaunchedEffect(state.isMuted) {
        val volume = if (state.isMuted) 0f else 1f
        player0.volume = volume
        player1.volume = volume
    }

    LaunchedEffect(playbackSpeed) {
        val params = PlaybackParameters(playbackSpeed)
        player0.playbackParameters = params
        player1.playbackParameters = params
    }

    LaunchedEffect(autoPlayEnabled) {
        val repeatMode = if (autoPlayEnabled) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE
        player0.repeatMode = repeatMode
        player1.repeatMode = repeatMode
        if (!autoPlayEnabled) {
            preloadTargetPage = -1
        }
    }

    LaunchedEffect(state.isPlaying, activeSlot) {
        val activePlayer = playerFor(activeSlot)
        val inactivePlayer = playerFor(1 - activeSlot)
        if (state.isPlaying) {
            activePlayer.playWhenReady = true
            activePlayer.play()
        } else {
            activePlayer.pause()
        }
        inactivePlayer.pause()
        inactivePlayer.playWhenReady = false
    }

    LaunchedEffect(state.videos, displayPageState, activeSlot, preloadTargetPage, autoPlayEnabled) {
        if (state.videos.isEmpty()) {
            clearSlot(0)
            clearSlot(1)
            return@LaunchedEffect
        }

        val last = state.videos.lastIndex
        val safePage = displayPageState.coerceIn(0, last)
        val inactiveSlot = 1 - activeSlot

        val slotWithCurrent = when (safePage) {
            slot0Page -> 0
            slot1Page -> 1
            else -> -1
        }

        if (slotWithCurrent >= 0 && slotWithCurrent != activeSlot) {
            activeSlot = slotWithCurrent
        }

        prepareSlot(activeSlot, safePage, play = state.isPlaying)

        if (!playbackSnapshotRestoreChecked) {
            playbackSnapshotRestoreChecked = true
            val snapshot = viewModel.consumePlaybackSnapshot(safePage)
            if (snapshot != null) {
                val activePlayer = playerFor(activeSlot)
                if (snapshot.positionMs > 0L) {
                    activePlayer.seekTo(snapshot.positionMs)
                    currentPositionMs = snapshot.positionMs
                }
                if (snapshot.wasPlaying) {
                    activePlayer.playWhenReady = true
                    activePlayer.play()
                } else {
                    activePlayer.pause()
                }
            }
        }

        val shouldKeepPreloaded = autoPlayEnabled && preloadTargetPage >= 0 && preloadTargetPage != safePage
        if (shouldKeepPreloaded) {
            if (pageFor(inactiveSlot) != preloadTargetPage) {
                prepareSlot(inactiveSlot, preloadTargetPage, play = false)
            }
        } else {
            clearSlot(inactiveSlot)
        }
    }

    LaunchedEffect(
        autoPlayEnabled,
        state.isPlaying,
        currentPositionMs,
        durationMs,
        displayPageState,
        activeSlot,
        state.videos.size
    ) {
        if (!autoPlayEnabled || !state.isPlaying || state.videos.isEmpty()) {
            preloadTargetPage = -1
            return@LaunchedEffect
        }

        val lastIndex = state.videos.lastIndex
        val safePage = displayPageState.coerceIn(0, lastIndex)
        val remainingMs = if (durationMs > 0L) durationMs - currentPositionMs else Long.MAX_VALUE
        val targetPage = if (remainingMs in 0L..8_000L) {
            nextPageByDirection(
                currentPage = safePage,
                direction = 1,
                lastIndex = lastIndex
            )
        } else {
            null
        }

        preloadTargetPage = targetPage ?: -1
        if (targetPage != null) {
            val inactiveSlot = 1 - activeSlot
            if (pageFor(inactiveSlot) != targetPage) {
                prepareSlot(inactiveSlot, targetPage, play = false)
            }
        }
    }

    LaunchedEffect(endedSignal, autoPlayEnabled, activeSlot, displayPageState, state.videos.size) {
        if (!autoPlayEnabled || endedSignal == 0 || endedSlot != activeSlot || state.videos.isEmpty()) {
            return@LaunchedEffect
        }

        val currentPage = displayPageState.coerceIn(0, state.videos.lastIndex)
        val targetPage = nextPageByDirection(
            currentPage = currentPage,
            direction = 1,
            lastIndex = state.videos.lastIndex
        ) ?: return@LaunchedEffect

        val previousActive = activeSlot
        val nextActive = 1 - previousActive
        prepareSlot(nextActive, targetPage, play = true)
        activeSlot = nextActive
        clearSlot(previousActive)
        preloadTargetPage = -1
        displayPageState = targetPage
        awaitingViewModelPageSync = true
        viewModel.onPageChanged(targetPage)
    }

    LaunchedEffect(allowScreenOffPlayback) {
        val wakeMode = if (allowScreenOffPlayback) C.WAKE_MODE_NETWORK else C.WAKE_MODE_NONE
        player0.setWakeMode(wakeMode)
        player1.setWakeMode(wakeMode)
    }

    DisposableEffect(activity, allowScreenOffPlayback, state.isPlaying, state.videos.isNotEmpty()) {
        val keepScreenOn = !allowScreenOffPlayback && state.isPlaying && state.videos.isNotEmpty()
        if (keepScreenOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        onDispose {
            if (keepScreenOn) {
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    DisposableEffect(lifecycleOwner, allowScreenOffPlayback, isFullscreen, activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && !allowScreenOffPlayback) {
                val isOrientationTransition = isFullscreen && (activity?.isChangingConfigurations == true)
                if (!isOrientationTransition) {
                    player0.pause()
                    player1.pause()
                    viewModel.pausePlayback()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(player0, player1, activeSlot) {
        while (isActive) {
            val activePlayer = playerFor(activeSlot)
            val duration = activePlayer.duration
            durationMs = if (duration > 0) duration else 0L
            if (!isSeeking) {
                currentPositionMs = activePlayer.currentPosition.coerceAtLeast(0L)
            }
            delay(260)
        }
    }

    LaunchedEffect(state.noticeMessage) {
        if (!state.noticeMessage.isNullOrBlank()) {
            delay(1200)
            viewModel.consumeNotice()
        }
    }

    LaunchedEffect(
        controlsAutoHideTick,
        isSeeking,
        controlsVisible,
        speedPanelVisible,
        qualityPanelVisible,
        uiSettings.playerAutoHideDelayMs,
        uiSettings.playerAutoHideTopArea,
        uiSettings.playerAutoHideRightArea,
        uiSettings.playerAutoHideBottomArea
    ) {
        val hasAutoHideArea = uiSettings.playerAutoHideTopArea ||
            uiSettings.playerAutoHideRightArea ||
            uiSettings.playerAutoHideBottomArea
        if (hasAutoHideArea && controlsVisible && !isSeeking && !speedPanelVisible && !qualityPanelVisible) {
            delay(uiSettings.playerAutoHideDelayMs.toLong())
            if (!isSeeking && !speedPanelVisible && !qualityPanelVisible) {
                controlsVisible = false
            }
        }
    }

    LaunchedEffect(controlsVisible) {
        if (!controlsVisible) {
            speedPanelVisible = false
            qualityPanelVisible = false
            sourcePickerExpanded = false
        }
    }

    LaunchedEffect(suppressReboundAnimation) {
        if (suppressReboundAnimation) {
            suppressReboundAnimation = false
        }
    }

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val pageBackgroundColor = if (isDarkTheme) Color.Black else Color.White
    val primaryForegroundColor = if (isDarkTheme) Color.White else Color.Black
    val secondaryForegroundColor = if (isDarkTheme) Color.White.copy(alpha = 0.72f) else Color.Black.copy(alpha = 0.7f)
    val floatingPanelColor = if (isDarkTheme) Color.Black.copy(alpha = 0.58f) else Color.White.copy(alpha = 0.74f)
    val effectiveStatusTopPadding = if (isFullscreen) 0.dp else statusBarPadding
    val effectiveBottomInset = if (isFullscreen) 0.dp else bottomInset
    val topAreaVisible = controlsVisible || !uiSettings.playerAutoHideTopArea
    val rightAreaVisible = controlsVisible || !uiSettings.playerAutoHideRightArea
    val bottomAreaVisible = controlsVisible || !uiSettings.playerAutoHideBottomArea

    val travelHeightPx = viewportHeightPx.toFloat().coerceAtLeast(1f)
    val settleOffsetY by animateFloatAsState(
        targetValue = snapTargetOffsetY,
        animationSpec = tween(
            durationMillis = when {
                suppressReboundAnimation -> 0
                pendingPageDelta != 0 -> 170
                else -> 220
            },
            easing = FastOutSlowInEasing
        ),
        label = "feedSwipeOffset",
        finishedListener = { endValue ->
            if (isDraggingPreview) return@animateFloatAsState

            if (pendingPageDelta != 0 && abs(endValue) >= travelHeightPx * 0.95f) {
                val fromPage = swipeBasePage.coerceIn(0, (state.videos.size - 1).coerceAtLeast(0))
                val last = state.videos.lastIndex
                val targetPage = (fromPage + pendingPageDelta).coerceIn(0, last.coerceAtLeast(0))
                val canSwitch = state.videos.isNotEmpty() && targetPage != fromPage
                if (canSwitch) {
                    val previousActive = activeSlot
                    val nextActive = 1 - previousActive
                    prepareSlot(nextActive, targetPage, play = true)
                    activeSlot = nextActive
                    clearSlot(previousActive)
                    displayPageState = targetPage
                    awaitingViewModelPageSync = true
                    viewModel.onPageChanged(targetPage)
                    switchMaskHold = true
                    switchMaskToken++
                }
                pendingPageDelta = 0
                dragOffsetY = 0f
                suppressReboundAnimation = true
                snapTargetOffsetY = 0f
            } else if (pendingPageDelta == 0 && abs(endValue) <= 1f) {
                dragOffsetY = 0f
                snapTargetOffsetY = 0f
            }
        }
    )
    val swipeOffsetY = if (isDraggingPreview) dragOffsetY else settleOffsetY

    LaunchedEffect(switchMaskToken) {
        if (switchMaskToken == 0) return@LaunchedEffect
        val player = playerFor(activeSlot)
        var waitedMs = 0
        while (waitedMs < 1200) {
            if (player.playbackState == Player.STATE_READY) {
                break
            }
            delay(40)
            waitedMs += 40
        }
        delay(90)
        switchMaskHold = false
    }

    val targetMaskAlpha = when {
        switchMaskHold -> 1f
        pendingPageDelta != 0 -> 1f
        isDraggingPreview -> {
            val progress = (abs(dragOffsetY) / travelHeightPx).coerceIn(0f, 1f)
            0.35f + (0.55f * progress)
        }
        else -> 0f
    }
    val maskAlpha by animateFloatAsState(
        targetValue = targetMaskAlpha,
        animationSpec = tween(
            durationMillis = if (targetMaskAlpha > 0f) 90 else 220,
            easing = FastOutSlowInEasing
        ),
        label = "feedSwitchMask"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                viewportHeightPx = size.height.coerceAtLeast(1)
            }
            .background(pageBackgroundColor)
    ) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            state.errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = state.errorMessage ?: "加载失败", color = primaryForegroundColor)
                    Button(onClick = { viewModel.refresh(showLoading = true) }) {
                        Text("重试")
                    }
                }
            }

            state.videos.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "暂无视频", color = primaryForegroundColor)
                    Button(onClick = { viewModel.refresh(showLoading = true) }) {
                        Text("刷新")
                    }
                }
            }

            else -> {
                val displayPage = displayPageState.coerceIn(0, (state.videos.size - 1).coerceAtLeast(0))
                val current = state.videos.getOrNull(displayPage)
                val sourceLabel = currentSourceLabel(state)
                val selectedQualityPreset = PlaybackQualityPresets.findById(selectedQualityId)

                LaunchedEffect(current?.id, selectedQualityId) {
                    val item = current ?: run {
                        isQualityLoading = false
                        qualityErrorMessage = null
                        return@LaunchedEffect
                    }

                    isQualityLoading = true
                    qualityErrorMessage = null

                    app.appContainer.videoRepository
                        .resolvePlaybackStream(
                            itemId = item.id,
                            preset = selectedQualityPreset,
                            mediaSourceId = mediaSourceIdOverrides[item.id],
                            startTimeTicks = playerFor(activeSlot).currentPosition.coerceAtLeast(0L) * 10_000L
                        )
                        .onSuccess { resolved ->
                            streamUrlOverrides[item.id] = resolved.streamUrl
                            val resolvedSourceId = resolved.mediaSourceId
                            if (!resolvedSourceId.isNullOrBlank()) {
                                mediaSourceIdOverrides[item.id] = resolvedSourceId
                            }

                            val currentActivePlayer = playerFor(activeSlot)
                            val activeUri = currentActivePlayer.currentMediaItem
                                ?.localConfiguration
                                ?.uri
                                ?.toString()
                            if (activeUri != resolved.streamUrl) {
                                val resumeAt = currentActivePlayer.currentPosition.coerceAtLeast(0L)
                                val shouldPlay = state.isPlaying
                                runCatching {
                                    applyResolvedStream(
                                        player = currentActivePlayer,
                                        streamUrl = resolved.streamUrl,
                                        fallbackUrl = item.streamUrl,
                                        resumeAtMs = resumeAt,
                                        shouldPlay = shouldPlay
                                    )
                                }.onFailure { throwable ->
                                    qualityErrorMessage = throwable.message ?: "切换清晰度失败"
                                    streamUrlOverrides[item.id] = item.streamUrl
                                }
                            }
                        }
                        .onFailure { throwable ->
                            streamUrlOverrides[item.id] = item.streamUrl
                            qualityErrorMessage = throwable.message
                        }

                    isQualityLoading = false
                }

                val activePlayer = playerFor(activeSlot)
                val inactivePlayer = playerFor(1 - activeSlot)
                val playerResizeMode = if (fitHeightMode) {
                    AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                } else {
                    AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                }

                val shouldShowIncomingContainer = !isDraggingPreview && pendingPageDelta != 0
                val expectedInactivePage = if (shouldShowIncomingContainer) {
                    nextPageByDirection(
                        currentPage = displayPage,
                        direction = pendingPageDelta,
                        lastIndex = state.videos.lastIndex
                    )
                } else {
                    null
                }
                val inactiveHasExpectedVideo = expectedInactivePage != null && pageFor(1 - activeSlot) == expectedInactivePage
                val inactiveBaseOffset = if (pendingPageDelta >= 0) travelHeightPx else -travelHeightPx
                val hideSwipeVisual = isDraggingPreview || pendingPageDelta != 0 || switchMaskHold

                if (inactiveHasExpectedVideo) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationY = if (hideSwipeVisual) {
                                    inactiveBaseOffset
                                } else {
                                    swipeOffsetY + inactiveBaseOffset
                                }
                                alpha = if (hideSwipeVisual) 0f else 1f
                            },
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                setKeepContentOnPlayerReset(true)
                                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                                resizeMode = playerResizeMode
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                player = inactivePlayer
                            }
                        },
                        update = { view ->
                            view.resizeMode = playerResizeMode
                            view.player = inactivePlayer
                        }
                    )
                }

                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationY = if (hideSwipeVisual) 0f else swipeOffsetY
                        },
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            setKeepContentOnPlayerReset(true)
                            setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                            resizeMode = playerResizeMode
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            player = activePlayer
                        }
                    },
                    update = { view ->
                        view.resizeMode = playerResizeMode
                        view.player = activePlayer
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f)
                        .pointerInput(state.videos.size, displayPage, activeSlot) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val pointerId = down.id
                                val start = down.position
                                val touchSlop = viewConfig.touchSlop
                                val verticalStartSlop = touchSlop * 0.75f
                                val containerWidth = size.width.toFloat().coerceAtLeast(1f)
                                val containerHeight = size.height.toFloat().coerceAtLeast(1f)
                                var totalDx = 0f
                                var totalDy = 0f
                                var dragStarted = false
                                var lastMoveTime = down.uptimeMillis
                                var recentSampleTime = down.uptimeMillis
                                var recentSampleOffsetY = 0f

                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Main)
                                    val change = event.changes.firstOrNull { it.id == pointerId } ?: continue
                                    if (!change.pressed) {
                                        break
                                    }

                                    val delta: Offset = change.position - change.previousPosition
                                    totalDx += delta.x
                                    totalDy += delta.y
                                    lastMoveTime = change.uptimeMillis

                                    if (!dragStarted) {
                                        val absDx = abs(totalDx)
                                        val absDy = abs(totalDy)
                                        if (absDy > verticalStartSlop && absDy >= absDx * 0.85f) {
                                            dragStarted = true
                                            isDraggingPreview = true
                                            speedPanelVisible = false
                                            qualityPanelVisible = false
                                        }
                                    }

                                    if (dragStarted) {
                                        val offset = (change.position.y - start.y)
                                            .coerceIn(-containerHeight, containerHeight)
                                        dragOffsetY = offset
                                        if (change.uptimeMillis - recentSampleTime >= 120L) {
                                            recentSampleTime = change.uptimeMillis
                                            recentSampleOffsetY = offset
                                        }

                                        change.consume()
                                    }
                                }

                                if (dragStarted) {
                                    val distanceThreshold = containerHeight * 0.12f
                                    val velocityThreshold = containerHeight * 1.05f
                                    val velocityWindowMs = (lastMoveTime - recentSampleTime).coerceAtLeast(1L)
                                    val velocityWindowSec = velocityWindowMs / 1000f
                                    val releaseVelocityY = (dragOffsetY - recentSampleOffsetY) / velocityWindowSec
                                    val canGoNext = displayPage < state.videos.lastIndex
                                    val canGoPrev = displayPage > 0
                                    val delta = when {
                                        (dragOffsetY <= -distanceThreshold || releaseVelocityY <= -velocityThreshold) && canGoNext -> 1
                                        (dragOffsetY >= distanceThreshold || releaseVelocityY >= velocityThreshold) && canGoPrev -> -1
                                        else -> 0
                                    }

                                    pendingPageDelta = delta
                                    isDraggingPreview = false
                                    swipeBasePage = displayPage

                                    snapTargetOffsetY = when (delta) {
                                        1 -> -containerHeight
                                        -1 -> containerHeight
                                        else -> 0f
                                    }
                                    if (delta == 0) {
                                        pendingPageDelta = 0
                                    }
                                } else {
                                    val moved = abs(totalDx) > touchSlop || abs(totalDy) > touchSlop
                                    val centerTop = containerHeight * 0.3f
                                    val centerBottom = containerHeight * 0.7f
                                    val inCenterArea = down.position.y in centerTop..centerBottom
                                    val inBottomControlArea = down.position.y >= containerHeight * 0.72f
                                    val inSummonZone = isPointInsideTouchBand(
                                        x = down.position.x,
                                        y = down.position.y,
                                        width = containerWidth,
                                        height = containerHeight,
                                        band = uiSettings.playerSummonBand
                                    )
                                    val inPauseZone = isPointInsideTouchBand(
                                        x = down.position.x,
                                        y = down.position.y,
                                        width = containerWidth,
                                        height = containerHeight,
                                        band = uiSettings.playerPauseBand
                                    )

                                    isDraggingPreview = false
                                    pendingPageDelta = 0
                                    snapTargetOffsetY = 0f

                                    if (!moved) {
                                        val inOverlapZone = inSummonZone && inPauseZone
                                        val inSummonOnlyZone = inSummonZone && !inPauseZone
                                        val inPauseOnlyZone = inPauseZone && !inSummonZone

                                        if (inSummonOnlyZone) {
                                            if (controlsVisible) {
                                                controlsVisible = false
                                                speedPanelVisible = false
                                                qualityPanelVisible = false
                                                sourcePickerExpanded = false
                                                isSeeking = false
                                            } else {
                                                controlsVisible = true
                                                controlsAutoHideTick++
                                            }
                                        } else if (inPauseOnlyZone) {
                                            controlsVisible = true
                                            controlsAutoHideTick++
                                            viewModel.togglePlayPause()
                                        } else if (inOverlapZone) {
                                            if (!controlsVisible) {
                                                controlsVisible = true
                                                controlsAutoHideTick++
                                            } else {
                                                controlsVisible = true
                                                controlsAutoHideTick++
                                                viewModel.togglePlayPause()
                                            }
                                        } else if (controlsVisible && !inBottomControlArea) {
                                            // Paused state takes priority: tap should resume playback first.
                                            if (!state.isPlaying) {
                                                controlsVisible = true
                                                controlsAutoHideTick++
                                                viewModel.togglePlayPause()
                                            } else {
                                                // While playing, tap outside progress bar manually hides controls.
                                                controlsVisible = false
                                                speedPanelVisible = false
                                                qualityPanelVisible = false
                                                isSeeking = false
                                            }
                                        } else if (inCenterArea) {
                                            controlsVisible = true
                                            controlsAutoHideTick++
                                            viewModel.togglePlayPause()
                                        } else if (inBottomControlArea && !controlsVisible) {
                                            controlsVisible = true
                                            controlsAutoHideTick++
                                        }
                                    }
                                }
                            }
                        }
                )

                AnimatedVisibility(
                    visible = topAreaVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .zIndex(2f)
                        .padding(start = 16.dp, top = effectiveStatusTopPadding + 14.dp)
                ) {
                    Box {
                        Surface(
                            modifier = Modifier.clickable {
                                sourcePickerExpanded = !sourcePickerExpanded
                                qualityPanelVisible = false
                                speedPanelVisible = false
                                controlsVisible = true
                                controlsAutoHideTick++
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = floatingPanelColor
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(Color(0xFF2A87F6), CircleShape)
                                )
                                Text(
                                    text = sourceLabel,
                                    color = secondaryForegroundColor,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = if (sourcePickerExpanded) "收" else "选",
                                    color = primaryForegroundColor,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = sourcePickerExpanded,
                            onDismissRequest = { sourcePickerExpanded = false },
                            modifier = Modifier.background(floatingPanelColor)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (state.selectedLibraryId == null) "✓ 全部视频" else "全部视频",
                                        color = primaryForegroundColor
                                    )
                                },
                                onClick = {
                                    viewModel.selectLibrary(null)
                                    sourcePickerExpanded = false
                                }
                            )

                            state.libraries.forEach { library ->
                                val selected = state.selectedLibraryId == library.id && state.selectedLibraryType == library.type
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = (if (selected) "✓ " else "") + sourceOptionLabel(library),
                                            color = primaryForegroundColor
                                        )
                                    },
                                    onClick = {
                                        viewModel.selectLibrary(library)
                                        sourcePickerExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = rightAreaVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .zIndex(3f)
                        .padding(end = 10.dp, bottom = effectiveBottomInset + 126.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                    IconButton(onClick = {
                        controlsVisible = true
                        controlsAutoHideTick++
                        viewModel.toggleMuted()
                    }) {
                        Icon(
                            imageVector = if (state.isMuted) {
                                Icons.AutoMirrored.Outlined.VolumeOff
                            } else {
                                Icons.AutoMirrored.Outlined.VolumeUp
                            },
                            contentDescription = "静音",
                            tint = primaryForegroundColor
                        )
                    }
                    IconButton(
                        onClick = {
                            controlsVisible = true
                            controlsAutoHideTick++
                            viewModel.toggleFavoriteCurrent()
                        },
                        enabled = !state.isFavoriteUpdating
                    ) {
                        Icon(
                            imageVector = if (current?.isFavorite == true) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "收藏",
                            tint = if (current?.isFavorite == true) Color(0xFF2A87F6) else primaryForegroundColor
                        )
                    }
                    IconButton(onClick = {
                        controlsVisible = true
                        controlsAutoHideTick++
                        viewModel.toggleRandomMode()
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Shuffle,
                            contentDescription = "随机换一批",
                            tint = if (state.isRandomMode) Color(0xFF2A87F6) else primaryForegroundColor
                        )
                    }
                    IconButton(onClick = {
                        controlsVisible = true
                        controlsAutoHideTick++
                        viewModel.refresh(showLoading = false)
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "刷新",
                            tint = primaryForegroundColor
                        )
                    }
                    IconButton(onClick = {
                        controlsVisible = true
                        controlsAutoHideTick++
                        autoPlayEnabled = !autoPlayEnabled
                        scope.launch {
                            app.appContainer.uiSettingsStore.setAutoPlayHomeEnabled(autoPlayEnabled)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Autorenew,
                            contentDescription = "自动播放",
                            tint = if (autoPlayEnabled) Color(0xFF2A87F6) else primaryForegroundColor
                        )
                    }
                    IconButton(onClick = {
                        controlsVisible = true
                        controlsAutoHideTick++
                        fitHeightMode = !fitHeightMode
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.AspectRatio,
                            contentDescription = "画面适配",
                            tint = if (fitHeightMode) Color(0xFF2A87F6) else primaryForegroundColor
                        )
                    }
                    IconButton(onClick = {
                        controlsVisible = true
                        controlsAutoHideTick++
                        isFullscreen = !isFullscreen
                    }) {
                        Icon(
                            imageVector = if (isFullscreen) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                            contentDescription = if (isFullscreen) "退出全屏" else "进入全屏",
                            tint = primaryForegroundColor
                        )
                    }
                }
                }

                current?.let { item ->
                    AnimatedVisibility(
                        visible = bottomAreaVisible,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .zIndex(4f)
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, bottom = effectiveBottomInset + 2.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = floatingPanelColor),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = primaryForegroundColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${displayPage + 1}/${state.videos.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = secondaryForegroundColor
                                    )
                                }

                                val hasDuration = durationMs > 0L
                                val sliderValue = when {
                                    !hasDuration -> 0f
                                    isSeeking -> seekingFraction
                                    else -> (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Slider(
                                        value = sliderValue,
                                        onValueChange = { value ->
                                            controlsVisible = true
                                            controlsAutoHideTick++
                                            speedPanelVisible = false
                                            qualityPanelVisible = false
                                            if (hasDuration) {
                                                isSeeking = true
                                                seekingFraction = value.coerceIn(0f, 1f)
                                            }
                                        },
                                        onValueChangeFinished = {
                                            controlsVisible = true
                                            controlsAutoHideTick++
                                            if (hasDuration) {
                                                val seekTo = (durationMs * seekingFraction)
                                                    .toLong()
                                                    .coerceIn(0L, durationMs)
                                                activePlayer.seekTo(seekTo)
                                                currentPositionMs = seekTo
                                            }
                                            isSeeking = false
                                        },
                                        valueRange = 0f..1f,
                                        enabled = hasDuration,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(if (isSeeking) 24.dp else 14.dp)
                                    )

                                    Row(
                                        modifier = Modifier.padding(start = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            modifier = Modifier.clickable {
                                                controlsVisible = true
                                                controlsAutoHideTick++
                                                qualityPanelVisible = false
                                                speedPanelVisible = !speedPanelVisible
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isDarkTheme) {
                                                Color.White.copy(alpha = 0.16f)
                                            } else {
                                                Color.Black.copy(alpha = 0.12f)
                                            }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(44.dp)
                                                    .padding(vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${formatPlaybackSpeed(playbackSpeed)}x",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = primaryForegroundColor,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        Surface(
                                            modifier = Modifier.clickable {
                                                controlsVisible = true
                                                controlsAutoHideTick++
                                                speedPanelVisible = false
                                                sourcePickerExpanded = false
                                                qualityPanelVisible = !qualityPanelVisible
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isDarkTheme) {
                                                Color.White.copy(alpha = 0.16f)
                                            } else {
                                                Color.Black.copy(alpha = 0.12f)
                                            }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(56.dp)
                                                    .padding(vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = qualityBadgeText(selectedQualityPreset),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (selectedQualityPreset.id == PlaybackQualityPresets.ORIGINAL.id) {
                                                        primaryForegroundColor
                                                    } else {
                                                        Color(0xFF2A87F6)
                                                    },
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatMillisToClock(
                                            if (isSeeking) {
                                                (durationMs * seekingFraction).toLong()
                                            } else {
                                                currentPositionMs
                                            }
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = secondaryForegroundColor
                                    )
                                    Text(
                                        text = formatMillisToClock(durationMs),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = secondaryForegroundColor
                                    )
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = speedPanelVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .zIndex(5f)
                        .padding(end = 14.dp, bottom = effectiveBottomInset + 118.dp)
                ) {
                    val speedOptions = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkTheme) {
                                Color.Black.copy(alpha = 0.78f)
                            } else {
                                Color.White.copy(alpha = 0.92f)
                            }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.width(96.dp)) {
                            speedOptions.forEach { option ->
                                val selected = abs(playbackSpeed - option) < 0.001f
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (selected) {
                                                if (isDarkTheme) {
                                                    Color.White.copy(alpha = 0.16f)
                                                } else {
                                                    Color.Black.copy(alpha = 0.1f)
                                                }
                                            } else {
                                                Color.Transparent
                                            }
                                        )
                                        .clickable {
                                            playbackSpeed = option
                                            speedPanelVisible = false
                                            controlsVisible = true
                                            controlsAutoHideTick++
                                        }
                                        .padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${formatPlaybackSpeed(option)}x",
                                        color = if (selected) {
                                            Color(0xFF2A87F6)
                                        } else {
                                            primaryForegroundColor
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = qualityPanelVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .zIndex(5f)
                        .padding(end = 14.dp, bottom = effectiveBottomInset + 190.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkTheme) {
                                Color.Black.copy(alpha = 0.78f)
                            } else {
                                Color.White.copy(alpha = 0.92f)
                            }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.width(184.dp)) {
                            Text(
                                text = "清晰度",
                                color = primaryForegroundColor,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )

                            if (isQualityLoading) {
                                Text(
                                    text = "正在加载...",
                                    color = secondaryForegroundColor,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            } else {
                                qualityPresets.forEach { preset ->
                                    val selected = selectedQualityId == preset.id
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (selected) {
                                                    if (isDarkTheme) {
                                                        Color.White.copy(alpha = 0.16f)
                                                    } else {
                                                        Color.Black.copy(alpha = 0.1f)
                                                    }
                                                } else {
                                                    Color.Transparent
                                                }
                                            )
                                            .clickable {
                                                selectedQualityId = preset.id
                                                qualityPanelVisible = false
                                                controlsVisible = true
                                                controlsAutoHideTick++

                                                scope.launch {
                                                    app.appContainer.uiSettingsStore.setPreferredPlaybackPresetId(preset.id)
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 9.dp)
                                    ) {
                                        Text(
                                            text = preset.label,
                                            color = if (selected) Color(0xFF2A87F6) else primaryForegroundColor,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            if (!qualityErrorMessage.isNullOrBlank()) {
                                Text(
                                    text = qualityErrorMessage.orEmpty(),
                                    color = Color(0xFFE67E7E),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = !state.isPlaying,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .zIndex(4.5f)
                ) {
                    Surface(
                        modifier = Modifier.clickable {
                            controlsVisible = true
                            controlsAutoHideTick++
                            if (!state.isPlaying) {
                                viewModel.togglePlayPause()
                            }
                        },
                        shape = CircleShape,
                        color = if (isDarkTheme) {
                            Color.Black.copy(alpha = 0.45f)
                        } else {
                            Color.White.copy(alpha = 0.7f)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Pause,
                            contentDescription = "已暂停",
                            tint = primaryForegroundColor,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }

                if (!state.noticeMessage.isNullOrBlank()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .zIndex(5f)
                            .padding(top = effectiveStatusTopPadding + 10.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDarkTheme) {
                            Color.Black.copy(alpha = 0.75f)
                        } else {
                            Color.White.copy(alpha = 0.9f)
                        }
                    ) {
                        Text(
                            text = state.noticeMessage.orEmpty(),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            color = primaryForegroundColor,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(0.5f)
                        .background(Color.Black.copy(alpha = maskAlpha))
                )
            }
        }
    }
}

private fun nextPageByDirection(currentPage: Int, direction: Int, lastIndex: Int): Int? {
    if (lastIndex < 0) return null
    if (direction == 0) return null
    val target = currentPage + direction
    return if (target in 0..lastIndex) target else null
}

private fun fallbackPreloadPage(currentPage: Int, lastIndex: Int): Int? {
    if (lastIndex < 0) return null
    return when {
        currentPage < lastIndex -> currentPage + 1
        currentPage > 0 -> currentPage - 1
        else -> null
    }
}

private fun isPointInsideTouchBand(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    band: PlaybackTouchBand
): Boolean {
    if (width <= 0f || height <= 0f) return false
    val normalized = band.normalized()
    val bandHeight = (height * normalized.heightFraction).coerceIn(1f, height)
    val top = (height - bandHeight).coerceAtLeast(0f) * normalized.topFraction
    val bottom = top + bandHeight
    return x in 0f..width && y in top..bottom
}

private fun currentSourceLabel(state: HomeUiState): String {
    return when (state.selectedLibraryType) {
        MediaLibraryType.FAVORITES -> "当前源: 收藏夹"
        MediaLibraryType.PLAYLIST,
        MediaLibraryType.LIBRARY -> {
            val name = state.libraries.firstOrNull { it.id == state.selectedLibraryId }?.name
            if (name.isNullOrBlank()) "当前源: 媒体库" else "当前源: $name"
        }

        null -> "当前源: 全部视频"
    }
}

private fun sourceOptionLabel(library: com.lalakiop.embyx.core.model.MediaLibrary): String {
    return when (library.type) {
        MediaLibraryType.PLAYLIST -> "列表 · ${library.name}"
        MediaLibraryType.LIBRARY -> "媒体库 · ${library.name}"
        MediaLibraryType.FAVORITES -> "收藏 · ${library.name}"
    }
}

private fun qualityBadgeText(preset: PlaybackQualityPreset?): String {
    val id = preset?.id ?: return "清"
    return when (id) {
        PlaybackQualityPresets.ORIGINAL.id -> "原"
        PlaybackQualityPresets.P1440_40M.id -> "2K40"
        PlaybackQualityPresets.P1440_20M.id -> "2K20"
        PlaybackQualityPresets.P1080_20M.id -> "1080-20"
        PlaybackQualityPresets.P1080_10M.id -> "1080-10"
        PlaybackQualityPresets.P720_10M.id -> "720-10"
        PlaybackQualityPresets.P720_5M.id -> "720-5"
        else -> "清"
    }
}

private fun applyResolvedStream(
    player: ExoPlayer,
    streamUrl: String,
    fallbackUrl: String,
    resumeAtMs: Long,
    shouldPlay: Boolean
) {
    val safeUrl = selectPlayableStreamUrl(
        candidateUrl = streamUrl,
        fallbackUrl = fallbackUrl
    )
    require(safeUrl.isNotBlank()) { "返回播放地址为空" }
    player.setMediaItem(MediaItem.fromUri(safeUrl), resumeAtMs.coerceAtLeast(0L))
    player.prepare()
    player.playWhenReady = shouldPlay
    if (shouldPlay) {
        player.play()
    } else {
        player.pause()
    }
}

private fun selectPlayableStreamUrl(candidateUrl: String, fallbackUrl: String): String {
    val prefer = candidateUrl.trim()
    if (prefer.isBlank()) {
        return fallbackUrl
    }
    val isHls = prefer.contains("m3u8", ignoreCase = true) ||
        prefer.contains("/hls", ignoreCase = true)
    return if (isHls && !isHlsModuleAvailable()) {
        fallbackUrl
    } else {
        prefer
    }
}

private fun isHlsModuleAvailable(): Boolean {
    return runCatching {
        Class.forName("androidx.media3.exoplayer.hls.HlsMediaSource\$Factory")
        true
    }.getOrDefault(false)
}

private fun formatMillisToClock(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun formatPlaybackSpeed(speed: Float): String {
    val rounded = (speed * 100).toInt() / 100f
    return if (abs(rounded - rounded.toInt().toFloat()) < 0.001f) {
        rounded.toInt().toString()
    } else {
        "%.2f".format(rounded).trimEnd('0').trimEnd('.')
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private fun applyPlayerFullscreenMode(activity: Activity, fullscreen: Boolean) {
    activity.requestedOrientation = if (fullscreen) {
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    } else {
        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    WindowCompat.setDecorFitsSystemWindows(activity.window, !fullscreen)
    val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
    if (fullscreen) {
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    } else {
        controller.show(WindowInsetsCompat.Type.systemBars())
    }
}

