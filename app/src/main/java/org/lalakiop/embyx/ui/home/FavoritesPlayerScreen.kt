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
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.lalakiop.embyx.core.model.PlaybackQualityPreset
import com.lalakiop.embyx.core.model.PlaybackQualityPresets
import com.lalakiop.embyx.core.model.VideoItem
import com.lalakiop.embyx.data.local.PlaybackTouchBand
import com.lalakiop.embyx.data.local.UiSettings
import com.lalakiop.embyx.ui.debug.PlaybackDebugRegistry

@Composable
@OptIn(UnstableApi::class)
fun FavoritesPlayerScreen(
    videos: List<VideoItem>,
    initialIndex: Int,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    allowScreenOffPlayback: Boolean = false,
    onFullscreenChange: (Boolean) -> Unit = {},
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as EmbyXApp
    val uiSettings by app.appContainer.uiSettingsStore.settingsFlow.collectAsStateWithLifecycle(
        initialValue = UiSettings()
    )
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewConfig = LocalViewConfiguration.current
    val activity = context.findActivity()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = contentPadding.calculateBottomPadding()

    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekingFraction by remember { mutableFloatStateOf(0f) }
    var controlsVisible by remember { mutableStateOf(true) }
    var controlsAutoHideTick by remember { mutableIntStateOf(0) }
    var speedPanelVisible by remember { mutableStateOf(false) }
    var qualityPanelVisible by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var isPlaying by rememberSaveable { mutableStateOf(true) }
    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var fitHeightMode by rememberSaveable { mutableStateOf(false) }
    var selectedQualityId by rememberSaveable { mutableStateOf(PlaybackQualityPresets.ORIGINAL.id) }
    val qualityPresets = remember { PlaybackQualityPresets.all }
    var isQualityLoading by remember { mutableStateOf(false) }
    var qualityErrorMessage by remember { mutableStateOf<String?>(null) }
    val streamUrlOverrides = remember { mutableStateMapOf<String, String>() }
    val mediaSourceIdOverrides = remember { mutableStateMapOf<String, String>() }
    val favoriteOverrides = remember { mutableStateMapOf<String, Boolean>() }
    var isFavoriteUpdating by remember { mutableStateOf(false) }
    var favoriteErrorMessage by remember { mutableStateOf<String?>(null) }

    var viewportHeightPx by remember { mutableIntStateOf(1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var snapTargetOffsetY by remember { mutableFloatStateOf(0f) }
    var isDraggingPreview by remember { mutableStateOf(false) }
    var pendingPageDelta by remember { mutableIntStateOf(0) }
    var swipeBasePage by remember { mutableIntStateOf(0) }
    var suppressReboundAnimation by remember { mutableStateOf(false) }
    var switchMaskHold by remember { mutableStateOf(false) }
    var switchMaskToken by remember { mutableIntStateOf(0) }

    var displayPageState by rememberSaveable {
        mutableIntStateOf(initialIndex.coerceIn(0, (videos.size - 1).coerceAtLeast(0)))
    }

    var activeSlot by remember { mutableIntStateOf(0) }
    var slot0Page by remember { mutableIntStateOf(-1) }
    var slot1Page by remember { mutableIntStateOf(-1) }
    var autoPlayEnabled by rememberSaveable { mutableStateOf(uiSettings.autoPlayFavoritesEnabled) }
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
        val source0 = "favorites_p0"
        val source1 = "favorites_p1"

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
        val item = videos.getOrNull(page) ?: run {
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

    LaunchedEffect(videos.size, initialIndex) {
        if (videos.isEmpty()) {
            displayPageState = 0
            isPlaying = false
        } else {
            val safe = initialIndex.coerceIn(0, videos.lastIndex)
            if (displayPageState > videos.lastIndex) {
                displayPageState = safe
            }
        }
    }

    BackHandler {
        if (isFullscreen) {
            isFullscreen = false
        } else {
            onClose()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
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

    LaunchedEffect(displayPageState, videos) {
        favoriteErrorMessage = null
    }

    LaunchedEffect(uiSettings.autoPlayFavoritesEnabled) {
        autoPlayEnabled = uiSettings.autoPlayFavoritesEnabled
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

    LaunchedEffect(isPlaying, activeSlot) {
        val activePlayer = playerFor(activeSlot)
        val inactivePlayer = playerFor(1 - activeSlot)

        if (isPlaying) {
            activePlayer.playWhenReady = true
            activePlayer.play()
        } else {
            activePlayer.pause()
        }

        inactivePlayer.pause()
        inactivePlayer.playWhenReady = false
    }

    LaunchedEffect(videos, displayPageState, activeSlot, isPlaying, preloadTargetPage, autoPlayEnabled) {
        if (videos.isEmpty()) {
            clearSlot(0)
            clearSlot(1)
            return@LaunchedEffect
        }

        val last = videos.lastIndex
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

        prepareSlot(activeSlot, safePage, play = isPlaying)

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
        isPlaying,
        currentPositionMs,
        durationMs,
        displayPageState,
        activeSlot,
        videos.size
    ) {
        if (!autoPlayEnabled || !isPlaying || videos.isEmpty()) {
            preloadTargetPage = -1
            return@LaunchedEffect
        }

        val lastIndex = videos.lastIndex
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

    LaunchedEffect(endedSignal, autoPlayEnabled, activeSlot, displayPageState, videos.size) {
        if (!autoPlayEnabled || endedSignal == 0 || endedSlot != activeSlot || videos.isEmpty()) {
            return@LaunchedEffect
        }

        val currentPage = displayPageState.coerceIn(0, videos.lastIndex)
        val targetPage = nextPageByDirection(
            currentPage = currentPage,
            direction = 1,
            lastIndex = videos.lastIndex
        ) ?: return@LaunchedEffect

        val previousActive = activeSlot
        val nextActive = 1 - previousActive
        prepareSlot(nextActive, targetPage, play = true)
        activeSlot = nextActive
        clearSlot(previousActive)
        preloadTargetPage = -1
        displayPageState = targetPage
        isPlaying = true
    }

    LaunchedEffect(allowScreenOffPlayback) {
        val wakeMode = if (allowScreenOffPlayback) C.WAKE_MODE_NETWORK else C.WAKE_MODE_NONE
        player0.setWakeMode(wakeMode)
        player1.setWakeMode(wakeMode)
    }

    DisposableEffect(activity, allowScreenOffPlayback, isPlaying, videos.isNotEmpty()) {
        val keepScreenOn = !allowScreenOffPlayback && isPlaying && videos.isNotEmpty()
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
                    isPlaying = false
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
        }
    }

    LaunchedEffect(suppressReboundAnimation) {
        if (suppressReboundAnimation) {
            suppressReboundAnimation = false
        }
    }

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
        label = "favoritesSwipeOffset",
        finishedListener = { endValue ->
            if (isDraggingPreview) return@animateFloatAsState

            if (pendingPageDelta != 0) {
                val fromPage = swipeBasePage.coerceIn(0, (videos.size - 1).coerceAtLeast(0))
                val last = videos.lastIndex
                val targetPage = (fromPage + pendingPageDelta).coerceIn(0, last.coerceAtLeast(0))
                val canSwitch = videos.isNotEmpty() && targetPage != fromPage

                if (canSwitch) {
                    val previousActive = activeSlot
                    val nextActive = 1 - previousActive
                    prepareSlot(nextActive, targetPage, play = true)
                    activeSlot = nextActive
                    clearSlot(previousActive)
                    displayPageState = targetPage
                    isPlaying = true
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
        // Keep a short visual buffer so the first frame is stable before revealing.
        delay(90)
        switchMaskHold = false
    }

    val targetMaskAlpha = when {
        switchMaskHold -> 1f
        pendingPageDelta != 0 -> 1f
        isDraggingPreview -> {
            val progress = (abs(dragOffsetY) / travelHeightPx).coerceIn(0f, 1f)
            // Soften darkening speed during drag to reduce abrupt black flash.
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
        label = "favoritesSwitchMask"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                if (isFullscreen) {
                    PaddingValues(0.dp)
                } else {
                    contentPadding
                }
            )
            .onSizeChanged { size ->
                viewportHeightPx = size.height.coerceAtLeast(1)
            }
            .background(Color.Black)
    ) {
        if (videos.isEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "收藏夹暂无可播放视频", color = Color.White)
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "关闭",
                        tint = Color.White
                    )
                }
            }
            return@Box
        }

        val displayPage = displayPageState.coerceIn(0, videos.lastIndex)
        val current = videos.getOrNull(displayPage)
        val selectedQualityPreset = PlaybackQualityPresets.findById(selectedQualityId)
        val activePlayer = playerFor(activeSlot)
        val inactivePlayer = playerFor(1 - activeSlot)
        val playerResizeMode = if (fitHeightMode) {
            AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
        } else {
            AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
        }
        val effectiveStatusTopPadding = if (isFullscreen) 0.dp else statusBarPadding
        val effectiveBottomInset = if (isFullscreen) 0.dp else bottomInset
        val topAreaVisible = controlsVisible || !uiSettings.playerAutoHideTopArea
        val rightAreaVisible = controlsVisible || !uiSettings.playerAutoHideRightArea
        val bottomAreaVisible = controlsVisible || !uiSettings.playerAutoHideBottomArea
        val currentIsFavorite = current?.let { video ->
            favoriteOverrides[video.id] ?: video.isFavorite
        } ?: false

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
                        val shouldPlay = isPlaying
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

        val shouldShowIncomingContainer = !isDraggingPreview && pendingPageDelta != 0
        val expectedInactivePage = if (shouldShowIncomingContainer) {
            nextPageByDirection(
                currentPage = displayPage,
                direction = pendingPageDelta,
                lastIndex = videos.lastIndex
            )
        } else {
            null
        }
        val inactiveHasExpectedVideo = expectedInactivePage != null && pageFor(1 - activeSlot) == expectedInactivePage
        val showIncomingContainer = shouldShowIncomingContainer && inactiveHasExpectedVideo
        val inactiveBaseOffset = if (pendingPageDelta >= 0) travelHeightPx else -travelHeightPx
        val hideSwipeVisual = isDraggingPreview || pendingPageDelta != 0 || switchMaskHold

        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = if (showIncomingContainer && !hideSwipeVisual) {
                        swipeOffsetY + inactiveBaseOffset
                    } else {
                        inactiveBaseOffset
                    }
                    alpha = if (showIncomingContainer && !hideSwipeVisual) 1f else 0f
                },
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    setKeepContentOnPlayerReset(false)
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
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

        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = if (hideSwipeVisual) 0f else swipeOffsetY
                },
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    setKeepContentOnPlayerReset(false)
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
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
                .pointerInput(videos.size, displayPage, activeSlot, isPlaying) {
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
                            val canGoNext = displayPage < videos.lastIndex
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
                                        isSeeking = false
                                    } else {
                                        controlsVisible = true
                                        controlsAutoHideTick++
                                    }
                                } else if (inPauseOnlyZone) {
                                    controlsVisible = true
                                    controlsAutoHideTick++
                                    isPlaying = !isPlaying
                                } else if (inOverlapZone) {
                                    if (!controlsVisible) {
                                        controlsVisible = true
                                        controlsAutoHideTick++
                                    } else {
                                        controlsVisible = true
                                        controlsAutoHideTick++
                                        isPlaying = !isPlaying
                                    }
                                } else if (controlsVisible && !inBottomControlArea) {
                                    if (!isPlaying) {
                                        controlsVisible = true
                                        controlsAutoHideTick++
                                        isPlaying = true
                                    } else {
                                        controlsVisible = false
                                        speedPanelVisible = false
                                        qualityPanelVisible = false
                                        isSeeking = false
                                    }
                                } else if (inCenterArea) {
                                    controlsVisible = true
                                    controlsAutoHideTick++
                                    isPlaying = !isPlaying
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
                .padding(start = 12.dp, top = effectiveStatusTopPadding + 10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.Black.copy(alpha = 0.52f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "关闭",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = current?.title ?: "收藏播放",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(124.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = rightAreaVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .zIndex(4.2f)
                .padding(end = 12.dp, bottom = effectiveBottomInset + 94.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                Surface(
                    modifier = Modifier
                        .clickable {
                            controlsVisible = true
                            controlsAutoHideTick++
                            autoPlayEnabled = !autoPlayEnabled
                            scope.launch {
                                app.appContainer.uiSettingsStore.setAutoPlayFavoritesEnabled(autoPlayEnabled)
                            }
                        },
                    shape = RoundedCornerShape(9.dp),
                    color = Color.Black.copy(alpha = 0.52f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Autorenew,
                        contentDescription = "自动播放",
                        tint = if (autoPlayEnabled) Color(0xFF2A87F6) else Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Surface(
                    modifier = Modifier
                        .clickable(enabled = current != null && !isFavoriteUpdating) {
                            val target = current ?: return@clickable
                            controlsVisible = true
                            controlsAutoHideTick++

                            val previous = favoriteOverrides[target.id] ?: target.isFavorite
                            val next = !previous
                            favoriteOverrides[target.id] = next
                            favoriteErrorMessage = null
                            isFavoriteUpdating = true

                            scope.launch {
                                app.appContainer.videoRepository
                                    .setFavorite(itemId = target.id, favorite = next)
                                    .onFailure { throwable ->
                                        favoriteOverrides[target.id] = previous
                                        favoriteErrorMessage = throwable.message ?: "收藏同步失败"
                                    }
                                isFavoriteUpdating = false
                            }
                        },
                    shape = RoundedCornerShape(9.dp),
                    color = Color.Black.copy(alpha = 0.52f)
                ) {
                    Icon(
                        imageVector = if (currentIsFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "收藏",
                        tint = if (currentIsFavorite) Color(0xFF2A87F6) else Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Surface(
                    modifier = Modifier
                        .clickable {
                            fitHeightMode = !fitHeightMode
                            controlsVisible = true
                            controlsAutoHideTick++
                        },
                    shape = RoundedCornerShape(9.dp),
                    color = Color.Black.copy(alpha = 0.52f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AspectRatio,
                        contentDescription = "画面适配",
                        tint = if (fitHeightMode) Color(0xFF2A87F6) else Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Surface(
                    modifier = Modifier
                        .clickable {
                            controlsVisible = true
                            controlsAutoHideTick++
                            isFullscreen = !isFullscreen
                        },
                    shape = RoundedCornerShape(9.dp),
                    color = Color.Black.copy(alpha = 0.52f)
                ) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                        contentDescription = if (isFullscreen) "退出全屏" else "进入全屏",
                        tint = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.56f)),
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
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${displayPage + 1}/${videos.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.78f)
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
                                    color = Color.White.copy(alpha = 0.14f)
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
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier.clickable {
                                        controlsVisible = true
                                        controlsAutoHideTick++
                                        speedPanelVisible = false
                                        qualityPanelVisible = !qualityPanelVisible
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.14f)
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
                                                Color.White
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
                                color = Color.White.copy(alpha = 0.78f)
                            )
                            Text(
                                text = formatMillisToClock(durationMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.78f)
                            )
                        }

                        if (!favoriteErrorMessage.isNullOrBlank()) {
                            Text(
                                text = favoriteErrorMessage.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE67E7E)
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
                .padding(end = 14.dp, bottom = effectiveBottomInset + 138.dp)
        ) {
            val speedOptions = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.78f)
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
                                        Color.White.copy(alpha = 0.16f)
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
                                color = if (selected) Color(0xFF2A87F6) else Color.White,
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
                .padding(end = 14.dp, bottom = effectiveBottomInset + 188.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.78f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.width(184.dp)) {
                    Text(
                        text = "清晰度",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )

                    if (isQualityLoading) {
                        Text(
                            text = "正在加载...",
                            color = Color.White.copy(alpha = 0.78f),
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
                                            Color.White.copy(alpha = 0.16f)
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
                                    color = if (selected) Color(0xFF2A87F6) else Color.White,
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
            visible = !isPlaying,
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
                    if (!isPlaying) {
                        isPlaying = true
                    }
                },
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = "播放",
                    tint = Color.White,
                    modifier = Modifier.padding(18.dp)
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

private fun nextPageByDirection(currentPage: Int, direction: Int, lastIndex: Int): Int? {
    if (lastIndex < 0) return null
    if (direction == 0) return null
    val target = currentPage + direction
    return if (target in 0..lastIndex) target else null
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
