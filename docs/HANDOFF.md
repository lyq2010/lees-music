# 开发交接

更新时间：2026-09-27。本文只记录当前状态；功能变更见 [CHANGELOG](CHANGELOG.md)，产品约束见 [PRODUCT](PRODUCT.md)，发布操作见 [RELEASE](RELEASE.md)。

## 当前版本

- Lee's Music，包名 `com.lyq2010.leesmusic`，版本 `0.1.0` / versionCode `1`，Android 8.0+（minSdk 26，compile/targetSdk 37）。
- 公开源码仓库：<https://github.com/lyq2010/lees-music>，本地分支 `main`，首版标签 `v0.1.0`。
- 用户已批准最终连续 L / 声波与浅扩散纹图标，并恢复公开测试版发布。图标已接入 Android 矢量资源，设计预览不进入 APK。
- GPL-3.0-only；正式签名已生成并在指定外部目录备份，私钥不进入 Git。发布采用远端 CI，COS / R2 在中央仓库独立运行镜像 job。
- [v0.1.0 公开测试版](https://github.com/lyq2010/lees-music/releases/tag/v0.1.0) 已发布；[构建 CI](https://github.com/lyq2010/lees-music/actions/runs/36258352319) 与 [COS / R2 镜像](https://github.com/lyq2010/lee-releases/actions/runs/36258766171) 均成功。两通道下载摘要一致。不能将 MuMu 测试结果当作 S25 真机验收。

## 已实现范围

- Navidrome：歌曲、专辑、艺术家、收藏、个人/共享歌单、发现与分类搜索。歌单支持新建和追加歌曲；共享歌单只浏览播放。
- 歌曲菜单：立即播放、下一首播放、加入队列、收藏、加入歌单、原音质下载及歌曲信息。普通列表和菜单可使用已完成的离线文件。
- 页面状态：曲库列表、筛选、滚动位置及成功空结果保存在会话缓存；账号切换隔离旧请求。封面、首页分区和每日推荐具有按账号隔离的磁盘缓存。
- 歌词：同步高亮、自动跟随、手动浏览、点句跳转、同时间戳分组、普通文本回退及失败重试；封面和歌词共用播放控制。
- 播放：MediaSessionService、真实队列编辑、随机/循环、倍速、睡眠定时、通知歌词、曲风均衡器、自动换曲 3 秒交叉淡化。
- 设置：深浅模式、主题色、在线音质、计费网络策略、存储管理、服务器卡片、法律声明和双通道更新。

## 播放可靠性

当前管线为 ExoPlayer → CrossfadePlayer → RecoveringPlayer → ManagedPlayer → MediaSession。播放服务启用网络唤醒；UI 异步连接服务，不在主线程阻塞等待。

- 当前曲与实际队列下一首预取，共享播放缓存；读取与后台写入分离，避免缓存写锁阻塞预取。
- 默认缓存上限 512 MB，可选 1 GB / 2 GB，LRU 淘汰旧缓存；播放结束不立即删除。手动清理保留在用缓存，不影响用户下载。
- 临时网络故障最多五次退避重试；支持 Range 续传和 416 完整请求回退。认证或资源不存在不无限重试。
- 流媒体 HTTP/1.1，连接/读取超时为 10/30 秒。暂停超过 5 分钟恢复时重建加载，保持歌曲与进度。
- 网络恢复可继续等待中的播放；主动暂停或换曲取消旧恢复任务。离线期间编辑待播队列不取消当前曲恢复。
- 这些机制不证明真实反向代理或三星后台策略下永不间断。

## 验证结果

- 本地最新 JVM 测试 58 项通过，release 构建及 lint 通过；lint 仍有 31 条警告和 4 条提示，不能写成零警告。
- 本轮优化包在 MuMu 检查音乐库、封面、播放进度和纯音乐歌词状态；崩溃日志为空。采用保留原数据的测试签名副本，测试后恢复 debug 版本。
- 此前已执行歌词、歌曲菜单、设置、队列和播放恢复等 MuMu 专项测试；本次没有重新执行全部设备测试，不能合并为一次全量验收。
- R8 将本地正式包从 17,744,441 字节降至约 3,079,628 字节，减少约 82.6%；远端正式 APK 为 3,077,148 字节（2.93 MiB）。签名、安装包内容及源码敏感信息检查通过。
- 生成的测试文件、构建目录及项目级缓存按用户要求清理；保留 `src/test`、`src/androidTest`、Gradle Wrapper、SDK 配置和外部签名备份。清理后的首次构建需要重新生成产物。

## 尚未实现或验收

- Emby、Plex、ReplayGain、评分、scrobble 收听记录回传、Android Auto 曲库浏览。
- 队列跨进程恢复、拖拽排序、独立用户队列、批量下载管理、启动自动播放。
- S25 长时锁屏、Wi-Fi/移动网络切换、长暂停恢复、耳机/蓝牙切换及通知布局。
- 正式签名首版至下一版本的系统安装升级链；MuMu 原 debug 签名不能直接覆盖正式版，不得为验收清除用户数据。

S25 验收应记录设备系统版本、网络条件及实际结果：锁屏连续播放至少 60 分钟、暂停 20 分钟后恢复、网络切换、连续换曲、系统安装授权和页面安全区域。当前版本没有 scrobble/评分功能，不将服务端播放次数或评分变化列为已实现功能的通过条件。

## 本地环境

- 工作区 `D:\Desktop\codex\lees-music`，PowerShell 7。
- JDK `D:\Android\Android Studio\jbr`；SDK `D:\Android\Sdk`，SDK 包名 `platforms;android-37.0`，build-tools `36.0.0`。
- MuMu adb：`D:\Program Files (x86)\Netease\MuMuPlayer\nx_main\adb.exe`，设备 `127.0.0.1:16384`。本轮未连接 S25。
- 共享 Gradle 下载和构建缓存已按授权清理；首次构建会重新下载分发包和依赖。Android 官方模拟器及 SDK 源码已卸载，编译平台和工具链保留；MuMu 仍可使用。

```powershell
$env:JAVA_HOME = 'D:\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

正式构建使用项目签名环境变量，见发布文档；不得把密码放入命令正文或提交源码。UI 验收保留真实账号和下载数据，不为验证写入真实服务器测试歌单。
