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
- COS 与 R2 各自运行独立镜像 job，先上传并校验 APK、源码和校验文件，再更新并复核清单；一个镜像失败不阻止另一个。
- 每个镜像仅保留最新已验证版本，随后删除同项目更旧的 APK、源码包和校验文件；保留更新清单，不影响其他项目和 GitHub 历史版本。列举对象失败时不删除，完整性验证失败时不进入清理。
- 镜像 workflow 从音乐仓库 main 获取维护中的工具，发布资产仍按输入标签从不可变 GitHub Release 下载。
- 镜像使用 `lyq2010/lee-releases` 的已有 Secrets，避免在新项目复制云服务凭据。

打标签并推送后，由本仓库 `Android release` CI 完成构建、签名和上传。CI 成功后执行：

```sh
gh workflow run lees-music-mirrors.yml --repo lyq2010/lee-releases -f tag=v1.0.0
```

这是单独的镜像流程；COS 与 R2 无相互依赖，可分别检查和重试。后续发布替换为对应标签。

如果标签事件没有启动构建，可手动选择既有标签；不会移动标签或替换已发布文件：

```sh
gh workflow run release.yml --repo lyq2010/lees-music --ref main -f tag=v1.0.0
```

正式包仅保留中文及英文回退资源，启用 R8 代码和资源精简，预览工具仅加入 debug。`audit_apk.py` 在 CI 中检查包内容；R8 映射表作为独立 CI artifact 保存，不进入 APK。更新依赖后执行 `releaseDependencyInventory` 和 `tools/release/notices.py`，同步第三方声明。

更新器只在用户点击检查时联网；比较两个通道的有效版本，下载时校验长度与 SHA-256，安装前检查包名、版本和签名。安装由 Android 系统确认。升级成功后首次启动清理已安装的更新包，下载失败或取消时清理残留，未安装的新包保留；清理与下载在后台互斥执行。无可用清单时明确报错，不显示“已是最新版本”。

## 版本规则与门禁

标签格式 `v主版本.次版本.修订号`，修订号到 50 时进位。`versionCode` 每次发布必须增加。已发布版本不得覆盖重传。

从 1.0.0 起发布正式版：GitHub Release 不标记为预发布，并设为最新版本；应用内记录、发布标题和首页状态同步为正式版。

### 更新记录写法

- 默认使用中文，采用正式、简洁的产品文案，直接说明用户能感知的新增、改进和修复。每条集中表达一件事，避免技术原理、开发流水账和空泛宣传。
- 可以适当使用 emoji，建议每条至多一个；不用连续装饰符号，也不夸大未验证的效果。
- `docs/APP_CHANGELOG.md` 只保留最新一个版本，发布时整体替换，不追加历史版本。应用离线显示随包记录，发现新版本后显示新版本记录，二者不重复堆叠。
- 更新清单的 `notes` 直接读取 `docs/APP_CHANGELOG.md`，确保应用内记录与发布版本一致。
- `docs/RELEASE_NOTES.md` 为本次 GitHub Release 简介，沿用同样的简洁写法，另保留必要的兼容性和真机验收说明。
- `docs/CHANGELOG.md` 保留完整中文历史，按版本记录每次变更；详细验证证据、技术边界和交接事项写入 `docs/HANDOFF.md`，不塞进应用内更新记录。
- 发布前核对版本标题、`versionName`、`versionCode` 和标签，确保记录描述的是本次实际交付内容。

发布前完成：JVM 测试、Android 构建与 lint、MuMu 交互回归、正式 APK 签名核验、源码敏感信息检查、许可证及依赖声明检查。
S25 连续后台播放及网络切换已由用户验收通过。独立长时锁屏、蓝牙切换、退出崩溃修复版、小组件及媒体胶囊仍按实际结果分别记录；长暂停恢复不作为交付阻碍，不能用 MuMu 通过代替未完成的真机验收。

## Google 开发者验证

APK 签名和 Google Play 开发者账号是两件事。GitHub 可分发自行签名的 APK。
Android 正在分阶段引入开发者身份及应用注册要求，范围依地区和设备而异；不应将“不上架 Play”理解为永远无需验证。
发布前复核 [Android 开发者验证](https://developer.android.com/developer-verification) 与 [应用签名说明](https://developer.android.com/studio/publish/app-signing)。

## GPL

本项目采用 GPL-3.0-only。每个二进制版本同时提供对应源码，包括构建脚本、许可证和第三方声明。
签名私钥、服务器凭据及用户数据不属于公开源码附件。
