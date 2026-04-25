package com.lalakiop.embyx.ui.home

import com.lalakiop.embyx.core.model.MediaLibrary
import com.lalakiop.embyx.core.model.MediaLibraryType
import com.lalakiop.embyx.core.model.VideoItem

/**
 * 首页UI状态数据类
 * 统一管理首页所有界面状态与数据
 */
data class HomeUiState(
    val isLoading: Boolean = false,                  // 全局加载状态
    val videos: List<VideoItem> = emptyList(),       // 首页视频列表
    val libraries: List<MediaLibrary> = emptyList(),// 媒体库列表
    val selectedLibraryId: String? = null,           // 当前选中的媒体库ID
    val selectedLibraryType: MediaLibraryType? = null, // 当前选中的媒体库类型
    val errorMessage: String? = null,                // 全局错误提示信息
    val currentPage: Int = 0,                       // 首页当前分页页码
    val isRandomMode: Boolean = false,              // 是否开启随机模式
    val isMuted: Boolean = false,                   // 是否静音
    val isPlaying: Boolean = true,                  // 是否正在播放
    val isFavoriteUpdating: Boolean = false,        // 收藏状态更新中
    val noticeMessage: String? = null,              // 通知提示消息
    val browsingLibraryId: String? = null,          // 正在浏览的媒体库ID
    val browsingLibraryName: String? = null,       // 正在浏览的媒体库名称
    val browsingVideos: List<VideoItem> = emptyList(), // 浏览的视频列表
    val isBrowsingLoading: Boolean = false,        // 媒体库浏览加载状态
    val browsingErrorMessage: String? = null,      // 媒体库浏览错误信息
    val browsingCurrentPage: Int = 1,              // 浏览页当前页码
    val browsingHasNextPage: Boolean = false,      // 浏览页是否有下一页
    val searchSelectedLibraryId: String? = null,    // 搜索选中的媒体库ID
    val searchSelectedLibraryType: MediaLibraryType? = null, // 搜索选中的媒体库类型
    val searchQuery: String = "",                  // 搜索关键词
    val searchResults: List<VideoItem> = emptyList(), // 搜索结果列表
    val isSearchLoading: Boolean = false,          // 搜索加载状态
    val searchErrorMessage: String? = null          // 搜索错误信息
)