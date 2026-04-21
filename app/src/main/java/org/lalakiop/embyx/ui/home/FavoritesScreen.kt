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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

private const val FAVORITES_PAGE_SIZE = 20

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    contentPadding: PaddingValues,
    allowScreenOffPlayback: Boolean = false,
    onPlayerFullscreenChange: (Boolean) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var playingIndex by remember { mutableIntStateOf(-1) }
    var currentPage by remember { mutableIntStateOf(1) }
    var jumpPageInput by remember { mutableStateOf("1") }

    val totalPages = ((state.videos.size + FAVORITES_PAGE_SIZE - 1) / FAVORITES_PAGE_SIZE).coerceAtLeast(1)
    val safePage = currentPage.coerceIn(1, totalPages)
    val pageStart = (safePage - 1) * FAVORITES_PAGE_SIZE
    val pageEnd = (pageStart + FAVORITES_PAGE_SIZE).coerceAtMost(state.videos.size)
    val pageVideos = if (pageStart in 0 until pageEnd) {
        state.videos.subList(pageStart, pageEnd)
    } else {
        emptyList()
    }

    LaunchedEffect(state.videos.size) {
        if (state.videos.isEmpty()) {
            playingIndex = -1
            currentPage = 1
            jumpPageInput = "1"
        } else if (playingIndex > state.videos.lastIndex) {
            playingIndex = state.videos.lastIndex
        }
        currentPage = currentPage.coerceIn(1, totalPages)
        jumpPageInput = currentPage.toString()
    }

    LaunchedEffect(currentPage) {
        jumpPageInput = currentPage.toString()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "收藏夹",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(onClick = viewModel::refresh) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "刷新收藏",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            when {
                state.isLoading && state.videos.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                state.videos.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.errorMessage ?: "暂无收藏视频",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 8.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 44.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(pageVideos, key = { _, item -> item.id }) { index, item ->
                                val absoluteIndex = pageStart + index
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { playingIndex = absoluteIndex },
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
                                                maxLines = 1
                                            )
                                            Text(
                                                text = item.overview?.takeIf { it.isNotBlank() } ?: "暂无简介",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 2
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Outlined.PlayArrow,
                                            contentDescription = "播放",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        FavoritesPagingBar(
                            currentPage = safePage,
                            totalPages = totalPages,
                            jumpPageInput = jumpPageInput,
                            onJumpInputChange = { jumpPageInput = it.filter { ch -> ch.isDigit() }.take(6) },
                            onPreviousPage = {
                                currentPage = (safePage - 1).coerceAtLeast(1)
                            },
                            onNextPage = {
                                currentPage = (safePage + 1).coerceAtMost(totalPages)
                            },
                            onJumpPage = {
                                val target = jumpPageInput.toIntOrNull() ?: safePage
                                currentPage = target.coerceIn(1, totalPages)
                            },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }

        if (playingIndex >= 0 && state.videos.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            ) {
                FavoritesPlayerScreen(
                    videos = state.videos,
                    initialIndex = playingIndex.coerceIn(0, state.videos.lastIndex),
                    contentPadding = PaddingValues(0.dp),
                    allowScreenOffPlayback = allowScreenOffPlayback,
                    onFullscreenChange = onPlayerFullscreenChange,
                    onClose = { playingIndex = -1 }
                )
            }
        }
    }
}

@Composable
private fun FavoritesPagingBar(
    currentPage: Int,
    totalPages: Int,
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
                enabled = currentPage > 1,
                modifier = Modifier.weight(1f)
            ) {
                Text("上一页", style = MaterialTheme.typography.labelSmall)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    text = "/$totalPages",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                TextButton(
                    onClick = onJumpPage,
                    enabled = jumpPageInput.isNotBlank()
                ) {
                    Text("确认", style = MaterialTheme.typography.labelSmall)
                }
            }

            TextButton(
                onClick = onNextPage,
                enabled = currentPage < totalPages,
                modifier = Modifier.weight(1f)
            ) {
                Text("下一页", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
