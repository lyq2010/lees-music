# 发布与恢复

## 签名

APK 使用项目自有的 RSA 4096 正式密钥签名，不依赖 Google Play 开发者账号。
私钥、密码及恢复说明备份于维护者指定的外部安全目录，不进入 Git。
后续覆盖更新必须保留同一签名密钥。MuMu 中已有的 debug 签名版本不能直接覆盖为正式版；不得为测试更新而擅自卸载或清除用户数据。

GitHub Actions 使用三个 Secrets：`LEES_MUSIC_KEYSTORE_BASE64`、`LEES_MUSIC_STORE_PASSWORD`、`LEES_MUSIC_KEY_PASSWORD`。
本地构建对应环境变量为 `LEES_MUSIC_KEYSTORE`（文件路径）、`LEES_MUSIC_STORE_PASSWORD`、`LEES_MUSIC_KEY_PASSWORD`，只设置在当前进程。

## 分发

源码与安装包仓库：<https://github.com/lyq2010/lees-music>。

- GitHub Release：签名 APK、SHA-256、更新清单及对应源码归档。
- 主通道：`https://releases.angelolee.cn/latest-lees-music.json`。
- 备用通道：`https://plt-releases.leenbsl.workers.dev/latest-lees-music.json`。
- COS 与 R2 各自运行独立镜像 job，先上传安装包及源码，再更新清单；一个镜像失败不阻止另一个。
- 镜像使用 `lyq2010/lee-releases` 的已有 Secrets，避免在新项目复制云服务凭据。

打标签并推送后，由本仓库 `Android release` CI 完成构建、签名和上传。CI 成功后执行：

```sh
gh workflow run lees-music-mirrors.yml --repo lyq2010/lee-releases -f tag=v0.1.1
```

这是单独的镜像流程；COS 与 R2 无相互依赖，可分别检查和重试。后续发布替换为对应标签。

如果标签事件没有启动构建，可手动选择既有标签；不会移动标签或替换已发布文件：

```sh
gh workflow run release.yml --repo lyq2010/lees-music --ref main -f tag=v0.1.1
```

正式包仅保留中文及英文回退资源，启用 R8 代码和资源精简，预览工具仅加入 debug。`audit_apk.py` 在 CI 中检查包内容；R8 映射表作为独立 CI artifact 保存，不进入 APK。更新依赖后执行 `releaseDependencyInventory` 和 `tools/release/notices.py`，同步第三方声明。

更新器只在用户点击检查时联网；比较两个通道的有效版本，下载时校验长度与 SHA-256，安装前检查包名、版本和签名。安装由 Android 系统确认。无可用清单时明确报错，不显示“已是最新版本”。

## 版本规则与门禁

标签格式 `v主版本.次版本.修订号`，修订号到 50 时进位。`versionCode` 每次发布必须增加。已发布版本不得覆盖重传。

发布前完成：JVM 测试、Android 构建与 lint、MuMu 交互回归、正式 APK 签名核验、源码敏感信息检查、许可证及依赖声明检查。
S25 连续后台播放及网络切换已由用户验收通过。独立长时锁屏、蓝牙切换、退出崩溃修复版、小组件及媒体胶囊仍按实际结果分别记录；长暂停恢复不作为交付阻碍，不能用 MuMu 通过代替未完成的真机验收。

## Google 开发者验证

APK 签名和 Google Play 开发者账号是两件事。GitHub 可分发自行签名的 APK。
Android 正在分阶段引入开发者身份及应用注册要求，范围依地区和设备而异；不应将“不上架 Play”理解为永远无需验证。
发布前复核 [Android 开发者验证](https://developer.android.com/developer-verification) 与 [应用签名说明](https://developer.android.com/studio/publish/app-signing)。

## GPL

本项目采用 GPL-3.0-only。每个二进制版本同时提供对应源码，包括构建脚本、许可证和第三方声明。
签名私钥、服务器凭据及用户数据不属于公开源码附件。
