package com.lalakiop.embyx.ui.profile

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lalakiop.embyx.data.local.PlaybackHistoryEntry
import com.lalakiop.embyx.ui.home.FavoritesPlayerScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

private const val HISTORY_PAGE_SIZE = 20

@Composable
fun PlaybackHistoryScreen(
    randomHistory: List<PlaybackHistoryEntry>,
    sequentialHistory: List<PlaybackHistoryEntry>,
    contentPadding: PaddingValues,
    onBack: () -> Unit
) {
    var historyTab by remember { mutableStateOf(HistoryTab.SEQUENTIAL) }
    var currentPage by remember { mutableIntStateOf(1) }
    var jumpPageInput by remember { mutableStateOf("1") }
    var playingIndex by remember { mutableIntStateOf(-1) }

    val selectedHistory = if (historyTab == HistoryTab.SEQUENTIAL) sequentialHistory else randomHistory
    val playbackVideos = selectedHistory.map { it.video }
    val totalPages = (ceil(selectedHistory.size / HISTORY_PAGE_SIZE.toDouble()).toInt()).coerceAtLeast(1)
    val safePage = currentPage.coerceIn(1, totalPages)
    val pageStart = (safePage - 1) * HISTORY_PAGE_SIZE
    val pageEnd = (pageStart + HISTORY_PAGE_SIZE).coerceAtMost(selectedHistory.size)
    val pageItems = if (pageStart in 0 until pageEnd) {
        selectedHistory.subList(pageStart, pageEnd)
    } else {
        emptyList()
    }

    LaunchedEffect(historyTab, totalPages) {
        currentPage = currentPage.coerceIn(1, totalPages)
        jumpPageInput = currentPage.toString()
        playingIndex = -1
    }

    LaunchedEffect(selectedHistory.size) {
        if (selectedHistory.isEmpty() || playingIndex > selectedHistory.lastIndex) {
            playingIndex = -1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 52.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "历史播放",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = historyTab == HistoryTab.SEQUENTIAL,
                            onClick = { historyTab = HistoryTab.SEQUENTIAL },
                            label = { Text("顺序") }
                        )
                        FilterChip(
                            selected = historyTab == HistoryTab.RANDOM,
                            onClick = { historyTab = HistoryTab.RANDOM },
                            label = { Text("随机") }
                        )
                    }
                }
            }

            if (pageItems.isEmpty()) {
                item {
                    Text(
                        text = "当前页没有记录",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                itemsIndexed(pageItems, key = { _, item -> item.video.id + "_" + item.playedAtMs }) { index, entry ->
                    val absoluteIndex = pageStart + index
                    HistoryItem(
                        entry = entry,
                        onClick = {
                            playingIndex = absoluteIndex
                        }
                    )
                }
            }

            item {
                Text(
                    text = "已显示 ${pageItems.size} 条",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
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
                    onClick = {
                        currentPage = (currentPage - 1).coerceAtLeast(1)
                        jumpPageInput = currentPage.toString()
                    },
                    enabled = safePage > 1,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("上一页", style = MaterialTheme.typography.labelSmall)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = jumpPageInput,
                        onValueChange = { jumpPageInput = it.filter { ch -> ch.isDigit() }.take(6) },
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
                        onClick = {
                            val target = jumpPageInput.toIntOrNull() ?: safePage
                            currentPage = target.coerceIn(1, totalPages)
                            jumpPageInput = currentPage.toString()
                        },
                        enabled = jumpPageInput.isNotBlank()
                    ) {
                        Text("确认", style = MaterialTheme.typography.labelSmall)
                    }
                }

                TextButton(
                    onClick = {
                        currentPage = (currentPage + 1).coerceAtMost(totalPages)
                        jumpPageInput = currentPage.toString()
                    },
                    enabled = safePage < totalPages,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("下一页", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (playingIndex >= 0 && playbackVideos.isNotEmpty()) {
            FavoritesPlayerScreen(
                videos = playbackVideos,
                initialIndex = playingIndex.coerceIn(0, playbackVideos.lastIndex),
                contentPadding = PaddingValues(0.dp),
                onClose = { playingIndex = -1 }
            )
        }
    }
}

private enum class HistoryTab {
    RANDOM,
    SEQUENTIAL
}

@Composable
private fun HistoryItem(
    entry: PlaybackHistoryEntry,
    onClick: () -> Unit
) {
    val dateText = remember(entry.playedAtMs) {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(entry.playedAtMs))
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = entry.video.title, style = MaterialTheme.typography.titleSmall)
            entry.sourceName?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = "来源: $it",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "时间: $dateText",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
