package com.lalakiop.embyx.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.lalakiop.embyx.data.local.ThemeMode

private val AppLightColorScheme = lightColorScheme()
private val AppDarkColorScheme = darkColorScheme()

@Composable
fun EmbyXTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (isDark) AppDarkColorScheme else AppLightColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
