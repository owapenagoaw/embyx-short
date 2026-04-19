package com.lalakiop.embyx.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lalakiop.embyx.core.model.Session
import com.lalakiop.embyx.data.local.ThemeMode

@Composable
fun ProfileScreen(
    session: Session,
    themeMode: ThemeMode,
    allowScreenOffPlayback: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onScreenOffPlaybackChange: (Boolean) -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "我的", style = MaterialTheme.typography.headlineSmall)
        Text(text = "服务器: ${session.server}")
        Text(text = "用户名: ${session.username}")
        Text(text = "UserId: ${session.userId}")

        Text(text = "主题", style = MaterialTheme.typography.titleMedium)
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

        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = "息屏继续播放", style = MaterialTheme.typography.titleMedium)
                Text(text = "开启后会在锁屏时保持播放", style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = allowScreenOffPlayback,
                onCheckedChange = onScreenOffPlaybackChange
            )
        }

        Button(
            onClick = onLogoutClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("退出登录")
        }
    }
}
