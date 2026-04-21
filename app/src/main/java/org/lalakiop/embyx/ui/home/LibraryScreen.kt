package com.lalakiop.embyx.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.lalakiop.embyx.core.model.MediaLibrary
import com.lalakiop.embyx.core.model.MediaLibraryType
import com.lalakiop.embyx.core.model.VideoItem
import kotlin.math.max

@Composable
fun LibraryScreen(
    viewModel: HomeViewModel,
    contentPadding: PaddingValues,
    onPlayerFullscreenChange: (Boolean) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var playingIndex by remember { mutableIntStateOf(-1) }
    var jumpPageInput by remember(state.browsingLibraryId) { mutableStateOf("1") }

    val playlists = state.libraries.filter { it.type == MediaLibraryType.PLAYLIST }
    val libraries = state.libraries.filter { it.type == MediaLibraryType.LIBRARY }
    val isBrowsingLibrary = state.browsingLibraryId != null

    if (!isBrowsingLibrary && playingIndex >= 0) {
        playingIndex = -1
    }

    LaunchedEffect(state.browsingCurrentPage, state.browsingLibraryId) {
        jumpPageInput = state.browsingCurrentPage.toString()
    }

    LaunchedEffect(state.browsingCurrentPage) {
        if (playingIndex >= 0) {
            playingIndex = -1
        }
    }

    BackHandler(enabled = isBrowsingLibrary && playingIndex < 0) {
        viewModel.closeLibraryBrowser()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isBrowsingLibrary) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 52.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            IconButton(
                                onClick = viewModel::closeLibraryBrowser,
                                modifier = Modifier.align(Alignment.CenterStart)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "返回媒体库列表",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Text(
                                text = state.browsingLibraryName ?: "媒体库内容",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 48.dp, end = 48.dp)
                            )
                            IconButton(
                                onClick = viewModel::refreshLibraryBrowser,
                                modifier = Modifier.align(Alignment.CenterEnd)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = "刷新库内容",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }

                    when {
                        state.browsingVideos.isEmpty() && state.isBrowsingLoading -> {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }

                        state.browsingVideos.isEmpty() && state.browsingErrorMessage != null -> {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = state.browsingErrorMessage ?: "加载失败",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Button(onClick = viewModel::refreshLibraryBrowser) {
                                        Text("重试")
                                    }
                                }
                            }
                        }

                        state.browsingVideos.isEmpty() -> {
                            item {
                                Text(
                                    text = "该库暂无可播放媒体",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        else -> {
                            itemsIndexed(state.browsingVideos, key = { _, it -> it.id }) { index, video ->
                                LibraryMediaTile(
                                    video = video,
                                    onClick = {
                                        playingIndex = index
                                    }
                                )
                            }

                            item {
                                if (state.isBrowsingLoading) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }
                    }
                }

                LibraryPagingBar(
                    currentPage = state.browsingCurrentPage,
                    hasNextPage = state.browsingHasNextPage,
                    isLoading = state.isBrowsingLoading,
                    jumpPageInput = jumpPageInput,
                    onJumpInputChange = { input ->
                        jumpPageInput = input.filter { it.isDigit() }.take(6)
                    },
                    onPreviousPage = viewModel::goToPreviousLibraryBrowserPage,
                    onNextPage = viewModel::goToNextLibraryBrowserPage,
                    onJumpPage = {
                        jumpPageInput.toIntOrNull()?.let { page ->
                            viewModel.goToLibraryBrowserPage(page)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "媒体库",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
                        IconButton(
                            onClick = viewModel::loadLibraries,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "刷新媒体库",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                if (playlists.isNotEmpty()) {
                    item { SectionTitle(text = "播放列表") }
                    items(playlists, key = { it.id }) { library ->
                        LibraryTile(
                            library = library,
                            selected = false,
                            onClick = {
                                viewModel.openLibraryBrowser(library)
                            }
                        )
                    }
                }

                if (libraries.isNotEmpty()) {
                    item { SectionTitle(text = "媒体库") }
                    items(libraries, key = { it.id }) { library ->
                        LibraryTile(
                            library = library,
                            selected = false,
                            onClick = {
                                viewModel.openLibraryBrowser(library)
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "提示：播放源切换请到首页播放器顶部“当前源”中选择",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.libraries.isEmpty() && !state.isLoading) {
                Text(
                    text = "暂无可用媒体库",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        if (isBrowsingLibrary && playingIndex >= 0 && state.browsingVideos.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            ) {
                FavoritesPlayerScreen(
                    videos = state.browsingVideos,
                    initialIndex = playingIndex.coerceIn(0, state.browsingVideos.lastIndex),
                    contentPadding = PaddingValues(0.dp),
                    onFullscreenChange = onPlayerFullscreenChange,
                    onClose = { playingIndex = -1 }
                )
            }
        }
    }
}

@Composable
private fun LibraryPagingBar(
    currentPage: Int,
    hasNextPage: Boolean,
    isLoading: Boolean,
    jumpPageInput: String,
    onJumpInputChange: (String) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onJumpPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TextButton(
                onClick = onPreviousPage,
                enabled = !isLoading && currentPage > 1,
                modifier = Modifier.weight(1f)
            ) {
                Text("上一页", style = MaterialTheme.typography.labelSmall)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = jumpPageInput,
                    onValueChange = onJumpInputChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .width(64.dp)
                        .height(44.dp)
                )
                Text(
                    text = "/$currentPage",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                TextButton(
                    onClick = onJumpPage,
                    enabled = !isLoading && jumpPageInput.isNotBlank()
                ) {
                    Text("确认", style = MaterialTheme.typography.labelSmall)
                }
            }

            TextButton(
                onClick = onNextPage,
                enabled = !isLoading && hasNextPage,
                modifier = Modifier.weight(1f)
            ) {
                Text("下一页", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun LibraryTile(
    library: MediaLibrary,
    selected: Boolean,
    onClick: () -> Unit
) {
    val icon = when (library.type) {
        MediaLibraryType.PLAYLIST -> Icons.AutoMirrored.Outlined.PlaylistPlay
        MediaLibraryType.LIBRARY -> Icons.Outlined.Folder
        MediaLibraryType.FAVORITES -> Icons.Outlined.Folder
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f)
            }
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            Text(
                text = library.name,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 32.dp)
            )
        }
    }
}

@Composable
private fun LibraryMediaTile(
    video: VideoItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = video.imageUrl,
                    contentDescription = "媒体封面",
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(width = 84.dp, height = 48.dp)
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(8.dp)
                        ),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = video.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 94.dp, end = 64.dp)
                )
                val duration = video.durationSec?.let { sec -> formatDuration(sec) }
                if (duration != null) {
                    Text(
                        text = duration,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }

            if (!video.overview.isNullOrBlank()) {
                Text(
                    text = video.overview,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2
                )
            }
        }
    }
}

private fun formatDuration(totalSec: Long): String {
    val safe = max(0, totalSec.toInt())
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val seconds = safe % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
