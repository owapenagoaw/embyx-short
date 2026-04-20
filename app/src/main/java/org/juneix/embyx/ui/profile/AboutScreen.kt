package com.lalakiop.embyx.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
            Text(
                text = "关于",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
        }

        Card(colors = CardDefaults.cardColors()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "EmbyX 安卓客户端", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "这是一个面向 Emby 的轻量化竖屏播放客户端，支持随机/顺序播放、媒体库浏览和收藏播放。",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Card(colors = CardDefaults.cardColors()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = "开发者：狐言", style = MaterialTheme.typography.titleMedium)
                Text(text = "联系方式：huyanyyh@qq.com", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "GitHub：https://github.com/owapenagoaw/embyx-short",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Card(colors = CardDefaults.cardColors()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = "致谢", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "本项目在功能与架构思路上参考并引用了以下优秀项目，特别感谢：",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "- https://github.com/juneix/embyx",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
