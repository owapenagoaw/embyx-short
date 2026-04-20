# Changelog

All notable changes to the Android app are documented in this file.

## [0.1.3] - 2026-04-20

### 新增

- 新增独立搜索页能力：搜索源默认“全部媒体”，并支持下拉切换媒体库/播放列表/收藏来源。
- 收藏播放器新增收藏/取消收藏按钮，支持在播放过程中直接管理收藏状态。

### 优化

- 缓存作用域升级为“服务器 + 用户”双维度隔离，避免多账号切换时缓存互相污染。
- 首页播放器与收藏播放器右侧控制统一为简约图标风格，交互与视觉一致性更好。
- 搜索、首页与收藏播放链路打通，搜索结果可直接进入播放器播放。

### 修复

- 修复应用重启后偶发首页视频不自动播放问题：启动阶段增加播放器缓存作用域初始化门闩，确保播放器创建顺序正确。

### 构建

- Android app versionName 升级到 0.1.3。

## [0.1.1] - 2026-04-20

### Added

- Added Debug Overlay switch in Profile page settings.
- Added global floating debug panel with:
  - CPU usage (sampled from /proc/stat)
  - GPU decoder info (active video decoder names)
  - App heap memory usage
  - Local media cache size
- Added Random/Sequential playback history persistence and rendering in Profile page.
- Added playlist list rendering in Profile page.
- Added decoder registry for Home and Favorites players.

### Changed

- Improved random playback strategy:
  - Use Emby TotalRecordCount for weighted library selection when source is ALL.
  - Use RandomSeed for random fetch requests.
- Split random and sequential playback sessions in HomeViewModel.
- Sequential mode now persists and restores state by library scope:
  - video list
  - current page
  - position
  - play/pause state
- Unified player runtime config for Home/Favorites:
  - shared cache-backed media source factory
  - conservative load control

### Data Model / API

- ItemsResponse now includes TotalRecordCount.
- EmbyMediaApi.getVideos now supports:
  - EnableTotalRecordCount
  - RandomSeed

### Build

- Android app versionName bumped to 0.1.1.
