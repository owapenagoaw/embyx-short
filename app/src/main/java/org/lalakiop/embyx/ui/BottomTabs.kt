package com.lalakiop.embyx.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomTabs(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    Home("home", "首页", Icons.Outlined.Home),
    Search("search", "搜索", Icons.Outlined.Search),
    Library("library", "媒体库", Icons.Outlined.VideoLibrary),
    Favorites("favorites", "收藏", Icons.Outlined.FavoriteBorder),
    Profile("profile", "我的", Icons.Outlined.AccountCircle)
}
