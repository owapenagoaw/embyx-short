# EmbyX Short (Android)

EmbyX Short 是基于 Kotlin + Jetpack Compose + Media3 的安卓原生 Emby 客户端，主打“短视频流式滑动播放”体验。

本 README 仅面向安卓原生工程（android-native），不包含 Emby SDK 子目录的维护说明。

## 1. 基础信息

- 仓库名：embyx-short
- 仓库地址：https://github.com/owapenagoaw/embyx-short
- 应用包名：com.lalakiop.embyx
- 最低系统版本：Android 7.0+（minSdk 24）
- 编译目标：targetSdk 35
- 当前版本：0.1.1

## 2. 技术栈

- Kotlin + Coroutines
- Jetpack Compose + Material3
- Navigation Compose
- DataStore Preferences（本地设置与缓存）
- Retrofit + Gson（Emby API）
- Media3 ExoPlayer（播放内核）
- Coil（图片加载）

## 3. 目录结构（android-native）

```text
android-native/
├── app/
│   ├── src/main/java/org/juneix/embyx/
│   │   ├── data/             # 数据层（API、本地存储、Repository）
│   │   ├── domain/           # UseCase 与领域接口
│   │   ├── ui/               # Compose 页面与根导航
│   │   ├── player/           # 播放运行时配置
│   │   └── AppContainer.kt   # 依赖注入容器
│   ├── release/
│   │   └── app-release.apk   # release 构建产物
│   └── build.gradle.kts
├── gradlew / gradlew.bat
└── README.md
```

## 4. 主要功能

- 首页双槽位视频滑动播放
- 媒体库 / 播放列表浏览与加载更多
- 收藏列表播放
- 顺序 / 随机播放模式切换
- 全部媒体库加权随机选库（按库内容数量权重）
- 顺序播放状态持久化恢复（按媒体库维度）
- 清晰度切换与倍速控制
- 2GB 本地媒体缓存（LRU）
- 调试浮窗（CPU / 解码器 / 内存 / 本地缓存占用）
- 我的页展示随机/顺序历史播放与播放列表

## 5. 页面与按钮功能详解

以下说明尽量对应到代码行为，便于排查和二次开发。

### 5.1 底部导航

- 首页：进入主播放流
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
- 高/宽按钮：切换画面适配策略（fitHeightMode）
- 全/退按钮：切换全屏状态（isFullscreen）

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

### 5.3 收藏页（FavoritesScreen + FavoritesPlayerScreen）

- 顶部“刷新”按钮：刷新收藏列表
- 列表项点击：打开播放器并从对应索引开始
- 播放器顶部按钮：关闭、宽高切换、全屏切换
- 底部控制条：与首页播放器一致（进度/倍速/清晰度）

### 5.4 媒体库页（LibraryScreen）

- 顶部“刷新媒体库”按钮：重新拉取媒体库与播放列表
- “播放列表”分组：展示 Emby Playlist
- “媒体库”分组：展示普通库
- 点击卡片：进入库内媒体浏览
- 库内“刷新”按钮：重载当前库
- “加载更多”按钮：分页请求下一批媒体
- 返回按钮：关闭库浏览回到库列表

### 5.5 我的页（ProfileScreen）

#### 主题设置

- Light / Dark / 跟随系统

#### 开关

- 息屏继续播放：控制锁屏时播放策略
- 调试模式浮窗：开启后全局显示性能浮窗

#### 随机/顺序历史播放

- 标签页切换：顺序 / 随机
- 展示最近历史记录（标题、来源、时间）

#### 播放列表展示

- 展示当前缓存的播放列表项（名称、ID）

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
- 缓冲参数：保守 LoadControl，限制占用增长
- 双槽位策略：非活跃槽位 clearSlot，释放资源更及时

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

2. 播放列表为空？
先到媒体库页点刷新媒体库，成功后“我的”页会展示缓存到本地的播放列表。

3. 顺序播放恢复异常？
检查是否在随机模式下；随机模式与顺序模式是分开的状态体系。

## 11. 致谢

本项目在功能与架构思路上参考并引用了以下优秀项目，特别感谢：

- https://github.com/juneix/embyx
