package com.lalakiop.embyx.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lalakiop.embyx.data.local.PlaybackTouchBand
import com.lalakiop.embyx.data.local.UiSettings
import kotlin.math.roundToInt

data class PlayerControlsConfigDraft(
    val autoHideTop: Boolean,
    val autoHideRight: Boolean,
    val autoHideBottom: Boolean,
    val autoHideDelayMs: Int,
    val summonBand: PlaybackTouchBand,
    val pauseBand: PlaybackTouchBand
)

private enum class ZoneEditorTarget {
    SUMMON,
    PAUSE
}

@Composable
fun PlayerControlsSettingsScreen(
    settings: UiSettings,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onSave: (PlayerControlsConfigDraft) -> Unit
) {
    var autoHideTop by remember(settings) { mutableStateOf(settings.playerAutoHideTopArea) }
    var autoHideRight by remember(settings) { mutableStateOf(settings.playerAutoHideRightArea) }
    var autoHideBottom by remember(settings) { mutableStateOf(settings.playerAutoHideBottomArea) }
    var hideDelayMs by remember(settings) { mutableFloatStateOf(settings.playerAutoHideDelayMs.toFloat()) }
    var summonBand by remember(settings) { mutableStateOf(settings.playerSummonBand.normalized()) }
    var pauseBand by remember(settings) { mutableStateOf(settings.playerPauseBand.normalized()) }
    var showBandEditor by remember { mutableStateOf(false) }

    BackHandler(enabled = !showBandEditor, onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "返回"
                )
            }
            Text(
                text = "播放界面交互设置",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
        }

        Card(colors = CardDefaults.cardColors()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "控件区域隐藏策略", style = MaterialTheme.typography.titleMedium)
                ControlSwitchRow(
                    title = "顶部区域",
                    description = "播放源、顶部信息区跟随隐藏",
                    checked = autoHideTop,
                    onCheckedChange = { autoHideTop = it }
                )
                ControlSwitchRow(
                    title = "右侧区域",
                    description = "功能按钮区跟随隐藏",
                    checked = autoHideRight,
                    onCheckedChange = { autoHideRight = it }
                )
                ControlSwitchRow(
                    title = "底部区域",
                    description = "进度与时间区跟随隐藏",
                    checked = autoHideBottom,
                    onCheckedChange = { autoHideBottom = it }
                )

                Text(
                    text = "自动隐藏等待时间: ${hideDelayMs.roundToInt()} ms",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = hideDelayMs,
                    onValueChange = { hideDelayMs = it },
                    valueRange = 500f..8000f,
                    steps = 15
                )
            }
        }

        Card(colors = CardDefaults.cardColors()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "点击区域行为", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "区域编辑已拆分为全屏界面，便于看全貌和测试。区域条与屏幕等宽，仅可调整高度和纵向占用位置。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                BandSummaryRow(label = "呼出控件区域", band = summonBand)
                BandSummaryRow(label = "暂停/播放区域", band = pauseBand)

                Button(
                    onClick = { showBandEditor = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("打开全屏区域编辑")
                }
            }
        }

        Button(
            onClick = {
                onSave(
                    PlayerControlsConfigDraft(
                        autoHideTop = autoHideTop,
                        autoHideRight = autoHideRight,
                        autoHideBottom = autoHideBottom,
                        autoHideDelayMs = hideDelayMs.roundToInt(),
                        summonBand = summonBand.normalized(),
                        pauseBand = pauseBand.normalized()
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存并应用")
        }
    }

    if (showBandEditor) {
        PlayerTouchBandEditorDialog(
            summonBand = summonBand,
            pauseBand = pauseBand,
            autoHideTop = autoHideTop,
            autoHideRight = autoHideRight,
            autoHideBottom = autoHideBottom,
            onDismiss = { showBandEditor = false },
            onConfirm = { newSummonBand, newPauseBand ->
                summonBand = newSummonBand.normalized()
                pauseBand = newPauseBand.normalized()
                showBandEditor = false
            }
        )
    }
}

@Composable
private fun PlayerTouchBandEditorDialog(
    summonBand: PlaybackTouchBand,
    pauseBand: PlaybackTouchBand,
    autoHideTop: Boolean,
    autoHideRight: Boolean,
    autoHideBottom: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (PlaybackTouchBand, PlaybackTouchBand) -> Unit
) {
    var localSummonBand by remember(summonBand) { mutableStateOf(summonBand.normalized()) }
    var localPauseBand by remember(pauseBand) { mutableStateOf(pauseBand.normalized()) }
    var editorTarget by remember { mutableStateOf(ZoneEditorTarget.SUMMON) }

    val selectedBand = if (editorTarget == ZoneEditorTarget.SUMMON) localSummonBand else localPauseBand

    fun updateSelectedBand(transform: (PlaybackTouchBand) -> PlaybackTouchBand) {
        val updated = transform(selectedBand).normalized()
        if (editorTarget == ZoneEditorTarget.SUMMON) {
            localSummonBand = updated
        } else {
            localPauseBand = updated
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                    Text(
                        text = "全屏区域编辑",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "这是播放界面等比例缩略图。已禁用触摸拖拽，请使用下方滑块调节。区域条宽度固定为全屏宽度。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = editorTarget == ZoneEditorTarget.SUMMON,
                        onClick = { editorTarget = ZoneEditorTarget.SUMMON },
                        label = { Text("呼出控件区域") }
                    )
                    FilterChip(
                        selected = editorTarget == ZoneEditorTarget.PAUSE,
                        onClick = { editorTarget = ZoneEditorTarget.PAUSE },
                        label = { Text("暂停/播放区域") }
                    )
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 16f)
                        .heightIn(max = 340.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        .background(Color(0xFF0E1116))
                ) {
                    val maxHeightDp = maxHeight

                    val summonHeightDp = maxHeightDp * localSummonBand.heightFraction
                    val summonY = (maxHeightDp - summonHeightDp) * localSummonBand.topFraction

                    val pauseHeightDp = maxHeightDp * localPauseBand.heightFraction
                    val pauseY = (maxHeightDp - pauseHeightDp) * localPauseBand.topFraction

                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (autoHideTop) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(maxHeightDp * 0.16f)
                                    .background(Color.White.copy(alpha = 0.08f))
                            )
                        }
                        if (autoHideBottom) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .height(maxHeightDp * 0.2f)
                                    .background(Color.White.copy(alpha = 0.08f))
                            )
                        }
                        if (autoHideRight) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .size(width = 38.dp, height = maxHeightDp * 0.46f)
                                    .background(Color.White.copy(alpha = 0.08f))
                            )
                        }

                        if (editorTarget == ZoneEditorTarget.SUMMON) {
                            ZoneBox(
                                label = "暂停/播放",
                                color = Color(0xFFEA8C2C),
                                x = 0.dp,
                                y = pauseY,
                                widthFraction = 1f,
                                height = pauseHeightDp,
                                selected = false
                            )
                            ZoneBox(
                                label = "呼出控件",
                                color = Color(0xFF2A87F6),
                                x = 0.dp,
                                y = summonY,
                                widthFraction = 1f,
                                height = summonHeightDp,
                                selected = true
                            )
                        } else {
                            ZoneBox(
                                label = "呼出控件",
                                color = Color(0xFF2A87F6),
                                x = 0.dp,
                                y = summonY,
                                widthFraction = 1f,
                                height = summonHeightDp,
                                selected = false
                            )
                            ZoneBox(
                                label = "暂停/播放",
                                color = Color(0xFFEA8C2C),
                                x = 0.dp,
                                y = pauseY,
                                widthFraction = 1f,
                                height = pauseHeightDp,
                                selected = true
                            )
                        }
                    }
                }

                Text(
                    text = "当前编辑: ${if (editorTarget == ZoneEditorTarget.SUMMON) "呼出控件区域" else "暂停/播放区域"}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "高度 ${bandPercent(selectedBand.heightFraction)}%，位置 ${bandPercent(selectedBand.topFraction)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "当前编辑区域高度",
                    style = MaterialTheme.typography.labelMedium
                )
                Slider(
                    value = selectedBand.heightFraction,
                    onValueChange = { size ->
                        updateSelectedBand { it.copy(heightFraction = size) }
                    },
                    valueRange = 0.08f..0.9f
                )

                Text(
                    text = "当前编辑区域占用位置（纵向）",
                    style = MaterialTheme.typography.labelMedium
                )
                Slider(
                    value = selectedBand.topFraction,
                    onValueChange = { top ->
                        updateSelectedBand { it.copy(topFraction = top) }
                    },
                    valueRange = 0f..1f
                )

                Button(
                    onClick = {
                        onConfirm(localSummonBand, localPauseBand)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("完成并返回")
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun BandSummaryRow(
    label: String,
    band: PlaybackTouchBand
) {
    Text(
        text = "$label: 高度 ${bandPercent(band.heightFraction)}% · 位置 ${bandPercent(band.topFraction)}%",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun bandPercent(value: Float): Int {
    return (value * 100f).roundToInt().coerceIn(0, 100)
}

@Composable
private fun ZoneBox(
    label: String,
    color: Color,
    x: Dp,
    y: Dp,
    widthFraction: Float,
    height: Dp,
    selected: Boolean
) {
    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .fillMaxWidth(widthFraction)
            .height(height)
            .background(
                if (selected) color.copy(alpha = 0.22f) else color.copy(alpha = 0.14f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = color,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            )
            .padding(4.dp)
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun ControlSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
