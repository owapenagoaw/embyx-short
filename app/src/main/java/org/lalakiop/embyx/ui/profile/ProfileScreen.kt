package com.lalakiop.embyx.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lalakiop.embyx.core.model.MediaLibrary
import com.lalakiop.embyx.core.model.Session
import com.lalakiop.embyx.data.local.PlaybackHistoryEntry
import com.lalakiop.embyx.data.local.ThemeMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    session: Session,
    themeMode: ThemeMode,
    allowScreenOffPlayback: Boolean,
    debugOverlayEnabled: Boolean,
    playlists: List<MediaLibrary>,
    randomHistory: List<PlaybackHistoryEntry>,
    sequentialHistory: List<PlaybackHistoryEntry>,
    onThemeModeChange: (ThemeMode) -> Unit,
    onScreenOffPlaybackChange: (Boolean) -> Unit,
    onDebugOverlayEnabledChange: (Boolean) -> Unit,
    onOpenHistoryClick: () -> Unit,
    onOpenAboutClick: () -> Unit,
    onOpenPlayerControlsSettingsClick: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onLogoutClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(text = "我的", style = MaterialTheme.typography.headlineSmall)
        }

        item {
            Card(colors = CardDefaults.cardColors()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "服务器: ${session.server}")
                    Text(text = "用户名: ${session.username}")
                    Text(text = "UserId: ${session.userId}")
                }
            }
        }

        item {
            Text(text = "主题", style = MaterialTheme.typography.titleMedium)
        }

        item {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = themeMode == ThemeMode.LIGHT,
                    onClick = { onThemeModeChange(ThemeMode.LIGHT) },
                    label = { Text("Light") },
                    colors = FilterChipDefaults.filterChipColors()
                )
                FilterChip(
                    selected = themeMode == ThemeMode.DARK,
                    onClick = { onThemeModeChange(ThemeMode.DARK) },
                    label = { Text("Dark") },
                    colors = FilterChipDefaults.filterChipColors()
                )
                FilterChip(
                    selected = themeMode == ThemeMode.SYSTEM,
                    onClick = { onThemeModeChange(ThemeMode.SYSTEM) },
                    label = { Text("跟随系统") },
                    colors = FilterChipDefaults.filterChipColors()
                )
            }
        }

        item {
            SettingSwitchRow(
                title = "息屏继续播放",
                description = "开启后会在锁屏时保持播放",
                checked = allowScreenOffPlayback,
                onCheckedChange = onScreenOffPlaybackChange
            )
        }

        item {
            SettingSwitchRow(
                title = "调试模式浮窗",
                description = "显示 CPU、GPU(解码器)、内存、本地缓存占用",
                checked = debugOverlayEnabled,
                onCheckedChange = onDebugOverlayEnabledChange
            )
        }

        item {
            Button(
                onClick = onOpenPlayerControlsSettingsClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("播放界面交互设置")
            }
        }

        item {
            Text(text = "随机/顺序历史播放", style = MaterialTheme.typography.titleMedium)
        }

        item {
            Card(colors = CardDefaults.cardColors()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "顺序: ${sequentialHistory.size} 条  ·  随机: ${randomHistory.size} 条",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onOpenHistoryClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("进入历史播放列表")
                    }
                }
            }
        }

        item {
            Button(
                onClick = onOpenAboutClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("关于")
            }
        }

        item {
            Button(
                onClick = onLogoutClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("退出登录")
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = description, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun HistoryItem(entry: PlaybackHistoryEntry) {
    val dateText = remember(entry.playedAtMs) {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(entry.playedAtMs))
    }
    Card(colors = CardDefaults.cardColors()) {
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
