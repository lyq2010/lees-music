# 交接文档

更新时间：2026-09-26

## 当前状态

- 应用名 Lee's Music，包名 `com.lyq2010.leesmusic`，versionName 0.1.0，minSdk 26，target/compileSdk 37。
- 仓库 `D:\Desktop\codex\lees-music`，分支 `main`，已有本地提交，没有配置远程仓库。
- 官人已在 MuMu 上连上自己的 Navidrome，音乐库能显示专辑和封面。
- 能做的事：
  - 欢迎页 → 添加服务器（Navidrome / Emby / Plex）→ 配置一台服务器（主机、端口、路径；HTTPS 开关决定 `http://` 或 `https://`，不用手填协议）。
  - 首页、发现、搜索、设置。点专辑进入专辑页（封面、顺序播放、随机播放、曲目）。
  - 发现页「每日推荐」固定 50 首。每天本地 0 点自动换；左上角可手动刷新；右上角播放键按顺序播放并打开播放页；「查看全部」只进列表。
  - 点一首歌先打开播放页，不立刻出声。播放页有细进度条、细音量条、居中的控制行。顺序播放、随机播放、每日推荐的播放键才会开始播。
  - 没有当前歌曲时不显示底部播放条。有当前歌曲时，点播放条进入播放页，点条上的播放键才暂停或继续。
- 播放由 `PlaybackService` 承担，界面启动时不在主线程等待服务。`PlaybackService` 持有 ExoPlayer 和 `WAKE_MODE_NETWORK`。离开页面后应能继续播。锁屏很久会不会断，还没在真机上听满核对。
- 缓存：
  - 封面按封面编号存在内存和 `cacheDir/covers`。同一编号切页面不再重新下载。编号不变、只换了服务器上的图片时，会继续显示旧图。
  - 四份专辑列表（最近添加、最近播放、最常播放、随机推荐）写在 `filesDir/library-shelf.json`。下次打开先显示这份，再向服务器要新的；连不上就留着上次的，不清空。
  - 每日推荐写在 `filesDir/daily-mix.json`，带本地日期。过了 0 点再打开会重新抽。
- 还没做，或只有界面没有接到播放：
  - Emby、Plex 只能保存地址，不能浏览、不能播放。
  - 歌词、收藏、评分、scrobble、ReplayGain、整曲预取和断点续传。
  - 首页的歌曲、艺术家、下载格子，以及设置里除「服务器」以外的开关。
  - 搜索结果还不能点开。
  - 三星 S25 还没连上 USB 调试。

## 下一步

1. 在三星 S25 上锁屏连续听一段时间，核对通知栏和断连。通知权限被系统拦住时，通知可能不出现。
2. 歌词：从播放页进入单独页，优先 `getLyricsBySongId`，没有再退回 `getLyrics`。
3. 播放回写 Navidrome：正在播放、播放超过一半或满 4 分钟记一次，收藏和评分。
4. 防断连：整曲预取、断点续传、流媒体只用 HTTP/1.1。这是相对 Symfonium 的核心，还没做。

## 已定决策

- **技术栈**：Kotlin、Jetpack Compose、Media3 1.11.1。AGP 9.4.1、Gradle 9.6.0、Kotlin 2.3.21。不要加 `kotlin-android` 插件。字节码目标 17。Material 3 锁定 1.5.0-alpha29。
- **JDK**：Android Studio 自带的 JBR（OpenJDK 25.0.3）。不改全局 `JAVA_HOME`。
- **服务端**：Navidrome 用 Subsonic API，认证是 token + salt（`t = md5(密码 + salt)`）。Navidrome 还不支持 `apiKeyAuthentication`。Emby、Plex 已列入服务器类型，接口未写。
- **地址**：一台服务器只存一个地址。主机栏前自动显示 `http://` 或 `https://`。允许明文 HTTP。
- **密码**：Android Keystore AES-GCM 加密后放进 DataStore。
- **点击和播放**：点歌曲先进播放页，不自动出声。顺序播放、随机播放、每日推荐右上角播放键才会出声。
- **界面参照**：官人给的音流截图，以及 [liuyincs/musiver](https://github.com/liuyincs/musiver) 的使用说明。那个仓库没有应用源码。
- **图标**：L 声波，墨蓝 `#1E2250`，桃色 `#FFD6B8`，后半段珊瑚 `#FF8A65`。自适应图标加单色层。
- **隐私**：不做遥测。
- **代码与提交**：按职责拆文件；一次提交只做一件事；提交说明用中文，写为什么改，不加 AI 署名。

## 背景：Symfonium 的断连

- 锁屏后停、播到一半停或连跳、暂停很久后恢复不了。外网经过反向代理，没有开转码。
- 推测是流连接闲置后被代理或省电策略切断。还没有官人这边的日志证实。
- 防断连的预取和续传还没做。

## 环境

- Android Studio：`D:\Android\Android Studio`（zip 版，删目录即卸载）。SDK：`D:\Android\Sdk`。
- 构建：

```powershell
$env:JAVA_HOME = 'D:\Android\Android Studio\jbr'
Set-Location D:\Desktop\codex\lees-music
.\gradlew.bat assembleDebug
```

- MuMu 6.6.4：`D:\Program Files (x86)\Netease\MuMuPlayer`。adb 用 `nx_main\adb.exe`，`adb connect 127.0.0.1:16384`，安装加 `-s 127.0.0.1:16384`。架构 x86_64。
- 真机：三星 Galaxy S25，还没开 USB 调试。
- `GRADLE_USER_HOME=D:\DevCache\gradle`，`TEMP=D:\DevCache\Temp`。

## 注意事项

- Android Studio 的 exe 安装包改不了目录，总会装到 `C:\Program Files\Android\Android Studio`。要放 D 盘只能用官方 zip。
- `PlaybackService` 不能在界面第一次组合时同步 `future.get()`。那样主线程会卡住，应用看起来打不开。连接必须异步，`playerOrNull()` 为空时跳过播放操作。
- 主机栏如果已经粘了 `http://` 或 `https://`，保存时要去掉，否则会拼出主机名 `https`。
- 计划文件只放在 `C:\Users\leenb\.cursor\plans\`，不进仓库。
- 提交不要加 AI 署名。

## 待定

- 反向代理类型和配置：查断连时再向官人要。
- 展示字体还没选定，歌名暂用系统衬线。
- 第二版候选：离线下载、转码、跨设备队列、睡眠定时、均衡器、Android Auto、网络电台。
