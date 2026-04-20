package com.lalakiop.embyx.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun DebugMetricsOverlay(
    enabled: Boolean,
    cacheDir: File,
    modifier: Modifier = Modifier
) {
    if (!enabled) {
        return
    }

    val decoderMap by PlaybackDebugRegistry.decoderBySource.collectAsState()
    val decoders = decoderMap.values
    var cpuPercent by remember { mutableFloatStateOf(0f) }
    var heapUsedBytes by remember { mutableLongStateOf(0L) }
    var heapMaxBytes by remember { mutableLongStateOf(0L) }
    var cacheBytes by remember { mutableLongStateOf(0L) }
    var lastCpu by remember { mutableStateOf<CpuSnapshot?>(null) }

    LaunchedEffect(enabled) {
        while (enabled) {
            val currentCpu = readCpuSnapshot()
            val previous = lastCpu
            if (currentCpu != null && previous != null) {
                val totalDelta = (currentCpu.total - previous.total).coerceAtLeast(1L)
                val idleDelta = (currentCpu.idle - previous.idle).coerceAtLeast(0L)
                val usage = (totalDelta - idleDelta).toFloat() / totalDelta.toFloat()
                cpuPercent = (usage * 100f).coerceIn(0f, 100f)
            }
            if (currentCpu != null) {
                lastCpu = currentCpu
            }

            val runtime = Runtime.getRuntime()
            heapUsedBytes = (runtime.totalMemory() - runtime.freeMemory()).coerceAtLeast(0L)
            heapMaxBytes = runtime.maxMemory().coerceAtLeast(0L)
            cacheBytes = folderSize(cacheDir)
            delay(1000L)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopEnd
    ) {
        Column(
            modifier = Modifier
                .padding(top = 16.dp, end = 12.dp)
                .background(
                    color = Color(0xCC111111),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "调试监控",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "CPU: ${"%.1f".format(cpuPercent)}%",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
            Text(
                text = "GPU(解码器): ${formatDecoders(decoders)}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
            Text(
                text = "内存: ${formatBytes(heapUsedBytes)} / ${formatBytes(heapMaxBytes)}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
            Text(
                text = "本地缓存: ${formatBytes(cacheBytes)}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        }
    }
}

private data class CpuSnapshot(
    val total: Long,
    val idle: Long
)

private fun readCpuSnapshot(): CpuSnapshot? {
    return runCatching {
        val line = File("/proc/stat").useLines { lines ->
            lines.firstOrNull { it.startsWith("cpu ") }
        } ?: return null
        val parts = line.trim().split(Regex("\\s+"))
        if (parts.size < 5) {
            return null
        }
        val user = parts.getOrNull(1)?.toLongOrNull() ?: 0L
        val nice = parts.getOrNull(2)?.toLongOrNull() ?: 0L
        val system = parts.getOrNull(3)?.toLongOrNull() ?: 0L
        val idle = parts.getOrNull(4)?.toLongOrNull() ?: 0L
        val iowait = parts.getOrNull(5)?.toLongOrNull() ?: 0L
        val irq = parts.getOrNull(6)?.toLongOrNull() ?: 0L
        val softirq = parts.getOrNull(7)?.toLongOrNull() ?: 0L
        val steal = parts.getOrNull(8)?.toLongOrNull() ?: 0L

        val total = user + nice + system + idle + iowait + irq + softirq + steal
        CpuSnapshot(total = total, idle = idle + iowait)
    }.getOrNull()
}

private fun folderSize(file: File): Long {
    if (!file.exists()) {
        return 0L
    }
    if (file.isFile) {
        return file.length().coerceAtLeast(0L)
    }
    val children = file.listFiles().orEmpty()
    var sum = 0L
    for (child in children) {
        sum += folderSize(child)
    }
    return sum
}

private fun formatDecoders(decoders: Collection<String>): String {
    if (decoders.isEmpty()) {
        return "未检测"
    }
    return decoders
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(separator = " | ")
}

private fun formatBytes(bytes: Long): String {
    val safe = bytes.coerceAtLeast(0L)
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    val value = safe.toDouble()
    return when {
        value >= gb -> "${"%.2f".format(value / gb)} GB"
        value >= mb -> "${"%.1f".format(value / mb)} MB"
        value >= kb -> "${"%.1f".format(value / kb)} KB"
        else -> "$safe B"
    }
}
