package com.lalakiop.embyx.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.lalakiop.embyx.core.model.MediaLibraryType

@Composable
fun SearchScreen(
    viewModel: HomeViewModel,
    contentPadding: PaddingValues,
    allowScreenOffPlayback: Boolean = false
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var playingIndex by remember { mutableIntStateOf(-1) }
    var sourcePickerExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.searchResults.size) {
        if (state.searchResults.isEmpty() || playingIndex > state.searchResults.lastIndex) {
            playingIndex = -1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(contentPadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "搜索",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Box {
                    Surface(
                        modifier = Modifier.clickable { sourcePickerExpanded = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentSearchSourceLabel(state),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "选",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = sourcePickerExpanded,
                        onDismissRequest = { sourcePickerExpanded = false }
                    ) {
                        val allSelected = state.searchSelectedLibraryId == null && state.searchSelectedLibraryType == null
                        DropdownMenuItem(
                            text = {
                                Text(if (allSelected) "✓ 全部媒体" else "全部媒体")
                            },
                            onClick = {
                                viewModel.selectSearchLibrary(null)
                                sourcePickerExpanded = false
                            }
                        )

                        state.libraries.forEach { library ->
                            val selected = state.searchSelectedLibraryId == library.id &&
                                state.searchSelectedLibraryType == library.type
                            DropdownMenuItem(
                                text = {
                                    Text((if (selected) "✓ " else "") + sourceOptionLabel(library))
                                },
                                onClick = {
                                    viewModel.selectSearchLibrary(library)
                                    sourcePickerExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Text(
                text = "默认搜索源为全部媒体，可点击右上角切换来源",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    state.isSearchLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }

                    !state.searchErrorMessage.isNullOrBlank() -> {
                        Text(
                            text = state.searchErrorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    state.searchResults.isEmpty() -> {
                        Text(
                            text = if (state.searchQuery.isBlank()) {
                                "在下方输入关键词后开始搜索"
                            } else {
                                "没有找到匹配内容"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(
                                items = state.searchResults,
                                key = { _, item -> item.id }
                            ) { index, item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { playingIndex = index },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        AsyncImage(
                                            model = item.imageUrl,
                                            contentDescription = "预览图",
                                            modifier = Modifier
                                                .size(width = 120.dp, height = 68.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.surface,
                                                    RoundedCornerShape(10.dp)
                                                ),
                                            contentScale = ContentScale.Crop
                                        )

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = item.title,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = item.overview?.takeIf { it.isNotBlank() } ?: "暂无简介",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        androidx.compose.material3.Icon(
                                            imageVector = Icons.Outlined.PlayArrow,
                                            contentDescription = "播放",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    singleLine = true,
                    label = { Text("搜索关键词") },
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = { viewModel.searchVideos() },
                    enabled = state.searchQuery.isNotBlank()
                ) {
                    Text("搜索")
                }
                TextButton(
                    onClick = { viewModel.clearSearch() },
                    enabled = state.searchQuery.isNotBlank() || state.searchResults.isNotEmpty()
                ) {
                    Text("清空")
                }
            }
        }

        if (playingIndex >= 0 && state.searchResults.isNotEmpty()) {
            FavoritesPlayerScreen(
                videos = state.searchResults,
                initialIndex = playingIndex.coerceIn(0, state.searchResults.lastIndex),
                contentPadding = PaddingValues(0.dp),
                allowScreenOffPlayback = allowScreenOffPlayback,
                onClose = { playingIndex = -1 }
            )
        }
    }
}

private fun currentSearchSourceLabel(state: HomeUiState): String {
    return when (state.searchSelectedLibraryType) {
        MediaLibraryType.FAVORITES -> "当前源: 收藏夹"
        MediaLibraryType.PLAYLIST,
        MediaLibraryType.LIBRARY -> {
            val name = state.libraries.firstOrNull { it.id == state.searchSelectedLibraryId }?.name
            if (name.isNullOrBlank()) "当前源: 媒体库" else "当前源: $name"
        }

        null -> "当前源: 全部媒体"
    }
}

private fun sourceOptionLabel(library: com.lalakiop.embyx.core.model.MediaLibrary): String {
    return when (library.type) {
        MediaLibraryType.PLAYLIST -> "列表 · ${library.name}"
        MediaLibraryType.LIBRARY -> "媒体库 · ${library.name}"
        MediaLibraryType.FAVORITES -> "收藏 · ${library.name}"
    }
}
