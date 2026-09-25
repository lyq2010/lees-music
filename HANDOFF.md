# 交接文档

更新时间：2026-09-26

## 当前状态

- 工程骨架已建好，`gradlew assembleDebug` 通过，构建没有警告。
  - 目前只有一个显示 "Lee's Music" 的 Compose 页面，套在 `MaterialExpressiveTheme` 里。
- 应用图标已完成，是官人从 3 个概念里选的方向 1"L 声波"：
  - 背景：墨蓝 `#1E2250`。
  - 前景：桃色 `#FFD6B8` 的 L，底边延伸成声波；前半段桃色表示已播放，后半段珊瑚色 `#FF8A65` 表示已缓存，末端是一个珊瑚色圆点。
  - 单色层：后半段透明度 0.5，供主题图标使用。
  - 文件：`res/drawable/ic_launcher_foreground.xml`、`ic_launcher_monochrome.xml`、`res/values/ic_launcher_background.xml`、`res/mipmap-anydpi/ic_launcher*.xml`。
  - 已在浏览器里用同一套路径检查过：圆形、圆角方形、方形、水滴四种遮罩，24 到 96px 各尺寸，以及主题图标，图形都在 66dp 安全区内。
  - 还没在真机桌面上看过。
  - 预览页在 `D:\DevCache\scratch\lees-music-icon-preview.html`，要用本地 http 服务打开，浏览器工具不允许打开 `file://`。概念草图在 `C:\Users\leenb\.cursor\projects\d-Desktop-codex-lees-music\assets\`。
  - APK 信息：包名 `com.lyq2010.leesmusic`，应用名 `Lee's Music`，versionName 0.1.0，minSdk 26，target/compileSdk 37。
  - 版本：
    - AGP 9.4.1、Gradle 9.6.0（wrapper jar 已按官方 SHA-256 校验）、Kotlin 2.3.21；
    - Compose BOM 2026.09.00，Material 3 锁定 1.5.0-alpha29；
    - core-ktx 1.19.1、activity-compose 1.13.0、lifecycle 2.11.0。
  - 依赖目前只放了骨架需要的这些。Media3、OkHttp、kotlinx.serialization、Coil 3 在用到它们的那一步再加。
- 构建命令：

```powershell
$env:JAVA_HOME = 'D:\Android\Android Studio\jbr'
Set-Location D:\Desktop\codex\lees-music
.\gradlew.bat assembleDebug
```
- Android Studio 已装好：Quail 4 Patch 1（2026.1.4.8），用官方免安装 zip 版解压到 `D:\Android\Android Studio`，自带 JBR 是 OpenJDK 25.0.3。
  - 这个版本不写注册表，没有开始菜单快捷方式，也没有卸载项；删掉目录就算卸载。
  - 启动：`D:\Android\Android Studio\bin\studio64.exe`。
- Android SDK 已装好，位于 `D:\Android\Sdk`：
  - Platform `android-37.0`（Extension 22）、Build-Tools 36.0.0、Platform-Tools 37.0.1（adb）、Emulator 37.1.11；
  - Command-line Tools（`cmdline-tools\latest`，版本 15859902）。
  - 首次向导实际装到了默认的 `%LOCALAPPDATA%\Android\Sdk`。之后用 robocopy 搬到 D 盘，核对了文件数、字节数和抽样哈希，C 盘原目录已移到回收站。
  - Studio 配置里的 SDK 路径也改到了 D 盘：`%APPDATA%\Google\AndroidStudio2026.1.4\options\android.sdk.path.xml` 和 `jdk.table.xml`。
- 验证通过：`adb version`、`sdkmanager --list_installed`。
- 还没装模拟器系统镜像，也还没建 AVD。
- 仓库：`D:\Desktop\codex\lees-music`，分支 `main`，还没有提交，也没有配置远程仓库。
- 完整计划不放在仓库里，在 `C:\Users\leenb\.cursor\plans\navidrome_安卓播放器_bfcb71d4.plan.md`。本文件记录关键结论，不看计划也能接手。

## 下一步

1. **后台播放**：每日推荐按本地时间每天 0 点重新抽 50 首，应用开着会等到 0 点再换，关掉再打开如果已经过了 0 点也会换。手动刷新仍然保留。播放列表里点一首会用 ExoPlayer 播原始流，底部显示歌名，点一下暂停或继续。退出应用后播放会停，还没有前台服务。

后续顺序：API 客户端 → 内外网地址切换 → 防断连播放管线 → 浏览界面 → 播放服务 → ReplayGain → 歌词 → scrobble/收藏/评分 → 连接日志 → 真机验收。

## 已定决策

- **技术栈**
  - 原生 Kotlin + Jetpack Compose + Media3 1.11.1。
  - AGP 9.4.1 + Gradle 9.6.0 + Kotlin 2.3.21。
    - AGP 9 自带 Kotlin 支持，不要加 `kotlin-android` 插件。
    - AGP 9.4.1 的 pom 声明的 Kotlin 是 2.2.10，靠 `org.jetbrains.kotlin.plugin.compose` 2.3.21 把 Kotlin 版本抬上去（`gradlew --version` 显示 Kotlin 2.3.21）。
    - Kotlin 2.4.x 按 Gradle 兼容矩阵需要 Gradle 9.7 以上，暂不升级。
  - 字节码目标 17，minSdk 26。
- **Compose Material 3**：用 1.5.0-alpha 系列并锁定版本，目前锁定 alpha29（规划时最新是 alpha28，建工程时已出 alpha29）。Expressive API 在稳定版 1.4.0 里还没有。
- **JDK**：用 Android Studio 自带的 JBR（OpenJDK 25.0.3）。不用 `lees-pdf\.tools\jdk`（Temurin 25），不改全局 `JAVA_HOME` 和 PATH。
- **服务端**：Navidrome、Emby、Plex。登录页可以选择这三种，选择会存进 DataStore。
  - Navidrome 走 Subsonic / OpenSubsonic API，认证用 token + salt，因为 Navidrome 还不支持 `apiKeyAuthentication`。启动时调用 `getOpenSubsonicExtensions` 探测服务端能力。
  - Emby 和 Plex 的接口还没写。选了它们只能保存地址，不能播放。
- **服务器地址**：一台服务器只保存一个地址（主机、端口、路径、是否 HTTPS）。不再分内网和外网。
  - 密码用 Android Keystore 的 AES-GCM 加密后放进 DataStore。
  - 允许明文 HTTP（`network_security_config`），HTTPS 由配置页的开关决定。
  - 旧数据如果只有 `lan_url` 或 `wan_url`，读取时会当成这一个地址。保存后改存 `url`。
- **防断连（核心）**
  - 整首歌以原始格式（`format=raw`）预取进 `SimpleCache`，同时预取下一首；
  - 中断后用 `Range` 续传，收到 416 就从头重下，不跳歌；
  - 流媒体只走 HTTP/1.1，读取超时 30 秒；
  - 播放时设 `WAKE_MODE_NETWORK`；
  - 网络变化或暂停超过 5 分钟后，先清空连接池、重新选择地址，再恢复播放。
- **三星 One UI**：播放服务作为 `mediaPlayback` 类型的前台服务运行。设置页检查是否已豁免电池优化，没有就提示用户手动设置，不自动改系统设置。
- **界面**：封面驱动的 Material 3 Expressive。播放页按官人给的参考图排：圆角封面、细进度条、控制行和底栏。歌词不放在播放页，从音符按钮进入单独页面。第一版没有离线缓存功能，界面上不写缓存文案。
  - 2026-09-26：主页、播放页、歌词页已按预览写好，数据是 `SampleCatalog` 里的四首示例。歌名用系统衬线字体，还没选定专门的展示字体。
  - 设置、随机播放、投屏、睡眠定时、循环目前只有按钮。
- **隐私**：不做遥测；连接日志只保存在手机本地。
- **代码与提交**
  - 按职责拆文件：接口请求、地址选择、播放、缓存、界面各自独立，不把多种职责堆进同一个类。
  - 只写当前步骤需要的代码，不为以后可能用到的功能提前加抽象或配置。
  - 提交按职责分批。一次提交只做一件事，不把无关改动放进同一次提交。
  - 提交说明用中文，写为什么改，不加任何 AI 署名。

## 背景：Symfonium 的断连

- **现象**：锁屏后停播；播到一半停住或连跳几首；暂停较久后恢复不了。外网经过反向代理，没有开转码。
- **推测原因**：流连接闲置后被代理超时或手机省电策略切断。依据是 Symfonium 论坛的同类案例，官人这边的日志和代理配置都还没核实。
- **待核实**：用 Lee's Music 的连接日志确认；如有需要，再查反向代理的超时和 HTTP/2 设置、Navidrome 日志、UGOS 的硬盘休眠设置。

## 环境事实（2026-09-25 实测）

- Windows 11 家庭版 10.0.26200；i7-12700KF，32 GB 内存；D 盘剩余约 92 GB。
- 已有：git（`D:\Program Files\Git\cmd\git.exe`）、winget。能直连 Google Maven、Maven Central 和 Gradle 发行站，不走代理。
- `GRADLE_USER_HOME=D:\DevCache\gradle`，`TEMP=D:\DevCache\Temp`。
- Hypervisor 正在运行，`WinHvPlatform.dll` 存在，模拟器应该能用硬件加速；准确的启用状态需要管理员权限才能查，还没确认。
- 测试手机：三星 Galaxy S25。USB 调试尚未连接过。
- 模拟器：MuMu 模拟器 6.6.4，装在 `D:\Program Files (x86)\Netease\MuMuPlayer`。
  - 它自带的 adb 是 `nx_main\adb.exe`，模拟器起来后执行 `adb connect 127.0.0.1:16384`。
  - 2026-09-26 实测：架构 x86_64，调试包能安装，`MainActivity` 能到前台。
  - `adb devices` 会同时出现 `127.0.0.1:16384` 和 `emulator-5554`，安装时用 `-s 127.0.0.1:16384` 指定一台。

## 注意事项

- Cursor 的 `create_project` 工具在这台 Windows 上执行 `git init` 会失败（`spawn /bin/sh ENOENT`）。本仓库的 `git init` 是用本机 git 手动补上的。
- Android Studio 的 exe 安装包改不了安装目录，结果总是装到 `C:\Program Files\Android\Android Studio`：
  - winget 的 `--location`（它实际传的是 `/S /D=...`）不生效；
  - 图形界面向导里也改不了；
  - 在已经提权的进程里启动它，会返回 1223。
  要装到其他盘，只能用官方 zip 版，按下载页公布的 SHA-256 校验后解压。
- 安装过程中产生的残留都已移到回收站，官人同意过：C 盘的 Studio 目录、C 盘的 SDK、`D:\DevCache\Temp` 里的安装包和脚本。
  - 还剩一个空目录 `C:\Program Files\Android` 没动。
- 首次向导不一定会用向导里填的 SDK 路径。之后重装 Studio 或重置配置时，要检查 `android.sdk.path.xml` 实际写的是哪个路径。
- `sdkmanager` 已提示弃用，官方替代是 cmdline-tools 里的 `android` CLI（`android sdk`），目前 `sdkmanager` 仍然可用。
  - 在终端里跑之前，要先设 `$env:JAVA_HOME='D:\Android\Android Studio\jbr'`。
- 2026-09-26 0:18 前后，有一个来源不明的代理在本机执行过 winget 安装和 `winget list`，还往本文件里写过一行过时的内容（已删除）。官人说自己没有开其他对话。如果再看到不是本对话做的改动，先停下告诉官人。
- 设置用户级环境变量 `ANDROID_AVD_HOME`（打算用 `D:\Android\avd`）之前，要先问官人。
- 提交信息不要加任何 AI 署名。

## 待定问题

- 反向代理的类型和配置：排查服务端时需要官人提供。
- 标题用的英文展示字体：设计系统那一步和预览一起给官人确认。
- 第二版功能从这些候选里挑：离线下载、按网络类型转码、跨设备同步播放队列、睡眠定时、均衡器、Android Auto、多服务器、网络电台。
