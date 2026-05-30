<p align="center">
  <img src="https://socialify.git.ci/AnkioTomas/bluetooth/image?description=1&font=Source%20Code%20Pro&forks=1&issues=1&logo=https%3A%2F%2Fpic.dreamn.cn%2FuPic%2F2023_04_22_23_52_24_1682178744_1682178744595_CRaKET.png&name=1&pattern=Floating%20Cogs&pulls=1&stargazers=1&theme=Auto" alt="BluetoothDebug" width="640" height="320" />
</p>

*使用其他语言阅读：[English](README.md)、简体中文。*

## 简介

**蓝牙调试（BluetoothDebug）** 是一款 Android 低功耗蓝牙（BLE）调试工具，支持设备扫描、厂商识别、广播数据查看、本机/对外模拟，以及 WebDAV 配置同步。

## 特别说明

> **禁止用于各类打卡用途。**
>
> 部分打卡软件会偷偷上传定位，如果因此被抓到，**和本项目无关**。

## 功能概览

| 模块 | 说明 |
| --- | --- |
| **蓝牙（扫描）** | 实时扫描，显示厂商、RSSI、广播数据；支持公司关键词、空名称、信号强度过滤 |
| **模拟 → 本机模拟** | 通过 LSPosed Hook 系统 `com.android.bluetooth`（`GattService`），向本机应用注入伪造扫描结果 |
| **模拟 → 对外发送** | 前台 BLE 广播服务，按模拟页配置的 MAC、广播数据、RSSI 对外广播 |
| **WebDAV → 发送到 WebDAV** | 每 5 分钟按**扫描页相同过滤条件**扫描，上传首个匹配设备 |
| **WebDAV → 从 WebDAV 同步** | 定时从 WebDAV 拉取配置写入本地 |
| **设置** | WebDAV 账号、主题、语言、Hook 调试日志开关 |

## 环境要求

- Android 12+（API 30+）
- 支持 BLE 的设备
- **[LSPosed](https://github.com/LSPosed/LSPosed)**，且 **API ≥ 93**（**本机模拟**模式必需）
- 定位 / 蓝牙运行时权限（应用会在需要时申请）

## LSPosed 配置

1. 从 [Releases](https://github.com/AnkioTomas/bluetooth/releases) 安装最新 APK。
2. 在 LSPosed 中启用模块。
3. 作用域勾选：
   - `com.android.bluetooth`（系统蓝牙进程，本机模拟必需）
   - `net.ankio.bluetooth`（本应用）
4. 安装或更新模块后，请重启蓝牙栈（开关蓝牙或重启系统）。

> **对外发送** 与 **WebDAV** 不依赖 Xposed；**本机模拟** 必须启用 Hook。

## 使用说明

### 1. 从扫描页采集设备

1. 打开 **蓝牙** 页并授予扫描权限。
2. 可在 **筛选** 中设置公司关键词、RSSI 阈值、是否过滤空名称。
3. 点击扫描结果，MAC、广播数据、RSSI 会自动写入 **模拟** 页。

### 2. 本机模拟（Xposed）

1. 在 **模拟** 页确认或修改 MAC、广播数据、信号强度。
2. 在 **首页** 将 **模拟模式** 设为 **本机模拟**。
3. 本机其他 BLE 扫描应用将看到配置的设备。

排查 Hook 问题时，可在 **设置** 中开启 **Hook 调试日志**。

### 3. 对外发送（无需 Xposed）

1. 在 **模拟** 页配置 MAC、广播数据、RSSI。
2. 在 **首页** 将 **模拟模式** 设为 **对外发送**。
3. 应用启动前台 BLE 广播，附近设备可扫描到模拟数据。

广播运行 10 分钟后自动停止，或手动关闭模式即可停止。

### 4. WebDAV 同步

请先在 **设置** 中配置 WebDAV。

#### 发送到 WebDAV（发送端）

1. 在 **蓝牙** 页设置扫描过滤条件（与定时上传使用相同规则）。
2. 在 **首页** 将 **WebDAV** 设为 **发送到 WebDAV**。
3. 前台服务每 5 分钟扫描一次，上传首个匹配设备的数据。

#### 从 WebDAV 同步（接收端）

1. 在 **首页** 将 **WebDAV** 设为 **从 WebDAV 同步**。
2. 前台服务每 5 分钟拉取远程数据并写入本地配置。

## 编译

```bash
./gradlew :app:assembleDebug
```

日常使用请直接安装 GitHub Releases 中的 APK。

## 参与贡献

欢迎提交 Issue 和 Pull Request。

1. Fork 本仓库
2. 创建功能分支（`git checkout -b feature/my-change`）
3. 提交修改（`git commit -m 'feat: 描述变更'`）
4. 推送到分支（`git push origin feature/my-change`）
5. 发起 Pull Request

Commit 格式：

```
[类型]: [描述]

feat:     新功能
fix:      修复问题
docs:     文档
style:    格式调整
refactor: 重构
perf:     性能优化
test:     测试
chore:    工具 / 杂项
deps:     依赖更新
```

## 截图

| | | |
| --- | --- | --- |
| ![扫描](images/img_2.png) | ![模拟](images/img_1.png) | ![首页](images/img.png) |
| ![设置](images/img_3.png) | ![筛选](images/img_4.png) | |
| ![模拟模式](images/img_5.png) | ![WebDAV](images/img_6.png) | |

## 协议

GPL-3.0
