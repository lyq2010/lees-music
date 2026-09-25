# Lee's Music

连接自建音乐库的安卓播放器，支持内网和外网远程访问。服务器可以是 Navidrome、Emby 或 Plex。

> 状态：工程骨架已能构建，功能尚未开发。进度与交接信息见 [HANDOFF.md](HANDOFF.md)。

## 为什么做它

之前用 Symfonium 通过反向代理听 Navidrome，会出现三种问题：锁屏后停播，播到一半停住或连跳几首，暂停较久后恢复不了。网络质量本身没有问题。

Lee's Music 的首要目标是在同样的网络和代理条件下稳定播放：每首歌开始后先把整首快速下载到本地缓存，播放不依赖任何长时间闲置的网络连接。

## 第一版功能（规划）

- 浏览艺人、专辑、歌单，搜索
- 后台播放，通知栏、锁屏和蓝牙控制
- 内外网双地址：在家用内网，出门自动切到外网
- 同步歌词
- 服务器：Navidrome、Emby、Plex。目前只有 Navidrome 能登录并探测地址；Emby 和 Plex 可以先保存地址，接口还没接
- 同步回 Navidrome：正在播放、播放次数、收藏、评分
- 无缝播放、ReplayGain 音量均衡
- 封面驱动的 Material 3 Expressive 界面：配色随当前专辑封面变化

## 技术栈

- Kotlin + Jetpack Compose，Compose Material 3 锁定 1.5.0-alpha29
- Media3 1.11.1（ExoPlayer + `MediaLibraryService`）
- OkHttp + kotlinx.serialization，对接 Subsonic / OpenSubsonic API
- Coil 3 加载封面
- AGP 9.4.1 + Gradle 9.6.0 + Kotlin 2.3.21；minSdk 26，targetSdk 37

## 开发环境（Windows）

- Android Studio 2026.1.4 Patch 1（Quail 4），官方 zip 版解压在 `D:\Android\Android Studio`
- Android SDK：`D:\Android\Sdk`
- JDK：Android Studio 自带的 JBR
- 在终端里构建时，只在当前会话临时设置 JDK：

```powershell
$env:JAVA_HOME = 'D:\Android\Android Studio\jbr'
.\gradlew assembleDebug
```

- 测试设备：三星 Galaxy S25

## 隐私

- 不内置遥测，不接入任何第三方统计或崩溃上报。
- 服务器密码经 Android Keystore 加密后保存在手机本地。
- 连接日志只保存在手机上，需要手动导出。
