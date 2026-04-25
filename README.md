# EmbyX Short (Android)

EmbyX Short 是基于 Kotlin + Jetpack Compose + Media3 的安卓原生 Emby 客户端，主打“短视频流式滑动播放”体验。

本 README 仅面向安卓原生工程（android-native），不包含 Emby SDK 子目录的维护说明。

## 1. 基础信息

- 仓库名：embyx-short
- 仓库地址：https://github.com/owapenagoaw/embyx-short
- 应用包名：com.lalakiop.embyx
- 最低系统版本：Android 7.0+（minSdk 24）
- 编译目标：targetSdk 35
- 当前版本：0.1.6

## 2. 技术栈

- Kotlin + Coroutines
- Jetpack Compose + Material3
- Navigation Compose
- DataStore Preferences（本地设置与缓存）
- Retrofit + Gson（Emby API）
- Media3 ExoPlayer（播放内核）
- Coil（图片加载）

## 3. 目录结构（android-native）

. 技术架构
采用 Clean Architecture（整洁架构） 设计模式，分为三层：
数据层 (Data Layer)
远程数据源 (remote/)
EmbyAuthApi.kt - 认证API接口
EmbyMediaApi.kt - 媒体API接口（视频、库、收藏等）
ApiClientFactory.kt - API客户端工厂
model/ - 网络响应模型
本地数据源 (local/)
SessionStore.kt - 会话存储（登录状态）
UiSettingsStore.kt - UI设置存储（主题、开关等）
HomeCacheStore.kt - 首页缓存（视频列表、播放历史等）
仓库层 (repository/)
AuthRepositoryImpl.kt - 认证仓库实现
EmbyVideoRepository.kt - 视频仓库实现
VideoRepository.kt - 视频仓库接口
领域层 (Domain Layer)
仓库接口 (domain/repository/)
AuthRepository.kt - 认证仓库接口
用例 (domain/usecase/)
LoginUseCase.kt - 登录用例
GetFeedUseCase.kt - 获取视频流用例
GetLibrariesUseCase.kt - 获取媒体库用例
SetFavoriteUseCase.kt - 设置收藏用例
表现层 (UI Layer)
核心组件
MainActivity.kt - 主Activity入口
EmbyXApp.kt - Application类
AppContainer.kt - 依赖注入容器
UI模块 (ui/)
EmbyXRoot.kt - 根导航组件
BottomTabs.kt - 底部导航定义
EmbyXTheme.kt - 主题配置
auth/ - 认证模块
LoginScreen.kt - 登录界面
AuthViewModel.kt - 认证ViewModel
home/ - 首页模块
PlayerFeedScreen.kt - 主播放器界面（85.6KB，核心功能）
HomeViewModel.kt - 首页ViewModel（730行）
FavoritesScreen.kt - 收藏列表
FavoritesPlayerScreen.kt - 收藏播放器（70.7KB）
LibraryScreen.kt - 媒体库浏览
SearchScreen.kt - 搜索界面
profile/ - 个人中心模块
ProfileScreen.kt - 个人主页
PlaybackHistoryScreen.kt - 播放历史
PlayerControlsSettingsScreen.kt - 播放器控制设置
AboutScreen.kt - 关于页面
debug/ - 调试模块
DebugMetricsOverlay.kt - 性能监控浮窗
PlaybackDebugRegistry.kt - 播放调试注册表
播放器模块 (player/)
PlayerRuntimeConfig.kt - 播放器运行时配置
核心模型 (core/model/)
Session.kt - 会话模型
VideoItem.kt - 视频项模型
MediaLibrary.kt - 媒体库模型
PlaybackQualityPreset.kt - 清晰度预设
ResolvedPlaybackStream.kt - 解析后的播放流

## 4. 主要功能

- 首页双槽位视频滑动播放
- 媒体库 / 播放列表浏览与分页
- 收藏列表播放
- 收藏列表分页浏览
- 顺序 / 随机播放模式切换
- 全部媒体库加权随机选库（按库内容数量权重）
- 顺序播放状态持久化恢复（按媒体库维度）
- 清晰度切换与倍速控制
- 2GB 本地媒体缓存（LRU）
- 调试浮窗（CPU / 解码器 / 内存 / 本地缓存占用）
- 我的页展示随机/顺序历史播放

## 5. 页面与按钮功能详解

以下说明尽量对应到代码行为，便于排查和二次开发。

### 5.1 底部导航

- 首页：进入主播放流
- 搜索：进入独立搜索页（默认全媒体源，可切换来源）
- 媒体库：进入库与播放列表浏览
- 收藏：进入收藏视频列表
- 我的：进入账户、设置、历史与调试开关

### 5.2 首页播放器（PlayerFeedScreen）

#### 顶部来源条

- 首页播放 + 当前源
- 点击后展开下拉：
	- 全部视频（不指定 ParentId）
	- 具体媒体库/播放列表（指定 ParentId）
- 切换来源后刷新 feed

#### 右侧操作按钮（从上到下）

- 静音按钮：切换 isMuted，调用 viewModel.toggleMuted()
- 收藏按钮：对当前视频收藏/取消收藏，调用 viewModel.toggleFavoriteCurrent()
- 随机按钮：切换随机/顺序模式，调用 viewModel.toggleRandomMode()
- 刷新按钮：刷新当前来源数据，调用 viewModel.refresh(showLoading = false)
- 自动播放按钮（循环箭头图标）：切换自动连播
- 画面适配按钮（比例图标）：切换画面适配策略（fitHeightMode）
- 全屏按钮（全屏图标）：切换全屏状态（isFullscreen）

#### 底部控制条

- 标题与当前页码
- 进度滑杆：拖动后 seekTo
- 倍速芯片：0.75x / 1x / 1.25x / 1.5x / 2x
- 清晰度芯片：原画、2K、1080、720 等预设
- 左右时间文本：当前位置/总时长

#### 手势逻辑

- 竖向拖动：切换上一条/下一条
- 中央轻触：播放/暂停（依状态）
- 非底部区域轻触：显示/隐藏控制层
- 可配置触控区：
	- 呼出控件区域与暂停/播放区域为“全宽区域带”，支持设置高度与纵向位置
	- 区域编辑入口：我的 -> 播放界面交互设置 -> 打开全屏区域编辑
	- 全屏区域编辑已禁用缩略图触摸拖动，仅通过下方滑块调节
- 区域行为逻辑：
	- 非交集位置：
		- 呼出控件区域：呼出控件 / 隐藏控件
		- 暂停/播放区域：播放 / 暂停
	- 交集位置：保持原有逻辑
		- 控件隐藏时：先呼出控件
		- 控件显示时：执行播放/暂停

### 5.3 收藏页（FavoritesScreen + FavoritesPlayerScreen）

- 顶部“刷新”按钮：刷新收藏列表
- 列表分页条：上一页 / 跳页 / 下一页
- 列表项点击：打开播放器并从对应索引开始
- 播放器右侧按钮：自动播放、收藏/取消收藏、画面适配、全屏切换
- 底部控制条：与首页播放器一致（进度/倍速/清晰度）

### 5.6 搜索页（SearchScreen）

- 搜索源默认“全部媒体”
- 右上角下拉可切换搜索源（媒体库/播放列表/收藏）
- 列表展示封面与简介，点击项后复用收藏播放器播放

### 5.4 媒体库页（LibraryScreen）

- 顶部“刷新媒体库”按钮：重新拉取媒体库与播放列表
- “播放列表”分组：展示 Emby Playlist
- “媒体库”分组：展示普通库
- 点击卡片：进入库内媒体浏览
- 库内“刷新”按钮：重载当前库
- 库内分页条：上一页 / 跳页 / 下一页
- 返回按钮：关闭库浏览回到库列表

### 5.5 我的页（ProfileScreen）

#### 主题设置

- Light / Dark / 跟随系统

#### 开关

- 息屏继续播放：控制锁屏时播放策略
- 调试模式浮窗：开启后全局显示性能浮窗

#### 随机/顺序历史播放

- 标签页切换：顺序 / 随机
- 历史页分页：上一页 / 跳页 / 下一页
- 展示历史记录（标题、来源、时间）
- 历史记录支持点击播放（复用收藏页播放器）
- 顺序/随机两张历史表各自最多保留 200 条

## 6. 调试浮窗说明

开启路径：我的 -> 调试模式浮窗

显示指标：

- CPU：通过 /proc/stat 采样估算占用率
- GPU(解码器)：展示当前播放器视频解码器名称（如硬解码器名）
- 内存：应用堆内存 已用/上限
- 本地缓存：播放器缓存目录实时占用

说明：安卓通用场景下，GPU 百分比不可稳定跨机型获取，因此本项目以“解码器信息”作为 GPU 侧调试信息。

## 7. 播放逻辑与缓存策略

### 7.1 随机/顺序逻辑

- 随机模式：
	- 指定库时，直接对该库随机请求
	- 全部视频时，先按库总数做加权随机选库，再请求该库随机内容
- 顺序模式：
	- 按媒体库维度保存播放状态（列表、页码、进度、播放状态）
	- 退出后重进可恢复
	- 从随机切回顺序，优先恢复顺序会话

### 7.2 缓存与内存

- 本地磁盘缓存：2GB LRU
- 缓存作用域：按“服务器 + 用户”隔离（本地列表缓存与播放器磁盘缓存均隔离）
- 缓冲参数：保守 LoadControl，限制占用增长
- 双槽位策略：非活跃槽位 clearSlot，释放资源更及时
- 启动保护：登录恢复后先完成播放器缓存作用域初始化，再创建播放器，避免重启后首页不播放
- 顺序播放列表刷新策略：
	- 每次刷新后使用“当前正在看的视频 ID”去新列表重新定位页码
	- 若服务端新增媒体导致顺序变动，优先保持当前视频不跳错
	- 若当前视频已不存在（被删/不可见），回退到旧页码的安全范围

## 8. 本地开发与构建

### 8.1 环境要求

- JDK 17
- Android SDK（compileSdk 35）
- 可用网络（访问 Emby 服务）

### 8.2 常用命令

在 android-native 目录执行：

```bash
./gradlew :app:compileDebugKotlin --no-daemon
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

Windows：

```powershell
.\gradlew.bat :app:compileDebugKotlin --no-daemon
.\gradlew.bat :app:assembleRelease
```

### 8.3 构建产物

- Release APK：app/release/app-release.apk
- 版本号：见 app/build.gradle.kts（versionName）

## 9. 数据持久化项

- UiSettingsStore：
	- 主题
	- 息屏播放开关
	- 随机模式开关
	- 默认清晰度
	- 调试浮窗开关
- HomeCacheStore：
	- 首页视频缓存
	- 媒体库缓存
	- 收藏缓存
	- 顺序播放状态表
	- 随机历史 / 顺序历史

## 10. 常见问题

1. 调试浮窗看不到“GPU占用百分比”？
当前实现显示的是“GPU(解码器)”信息，不是 GPU 百分比。该方案跨设备稳定性更高。

2. 看不到“我的”页播放列表？
新版本已移除“我的”页播放列表展示，播放源切换请在首页播放器顶部“当前源”中选择。

3. 顺序播放恢复异常？
检查是否在随机模式下；随机模式与顺序模式是分开的状态体系。

4. 播放时频繁显示“加载中”提示？
已优化加载指示器逻辑，**仅在视频因网络原因暂停超过2秒时才显示**：
- 短暂的网络波动不会触发加载提示
- 后台预加载完全不会干扰观看体验
- 清晰度切换也采用同样的2秒延迟策略
- 每500ms检查一次缓冲状态，确保及时更新网速显示

5. 播放进度是否同步到服务器？
✅ v0.1.6 已完整实现播放状态报告功能：
- 播放开始/停止自动报告
- 每10秒上报播放进度
- 支持多设备进度同步
- 服务端“继续观看”更准确



## 11. 致谢

本项目在功能与架构思路上参考并引用了以下优秀项目，特别感谢：
- https://github.com/juneix/embyx
