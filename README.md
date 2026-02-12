# Alex Accessibility Bridge

Android 无障碍服务 + HTTP API，为 PRoot 环境提供低延迟的 UI 自动化能力。

## 问题背景

在 Termux + PRoot 环境中，无法直接调用 Android 无障碍 API（uiautomator），必须通过文件桥接与 Termux 通信，导致每次调用延迟 2-3 秒。

本方案通过 APK 暴露 HTTP 接口，将延迟降至 ~50ms。

## 架构

```
PRoot (Python) → HTTP GET localhost:8080/dump → APK → uiautomator → JSON
                ← 50ms                         ← 0ms       (直接在主环境)
```

## 功能

- ✅ 获取 UI 树（`GET /dump`）
- ✅ 点击坐标（`GET /tap?x=540&y=1000`）
- ✅ 滑动（`GET /swipe?x1=540&y1=800&x2=540&y2=1500&duration=300`）
- ✅ 返回/主页/唤醒（`GET /back`, `/home`, `/wake`）
- ✅ 输入文本（`GET /input?text=hello`）

## 安装

### 1. 编译 APK

```bash
cd AccessibilityBridge
./gradlew assembleDebug
```

APK 路径: `app/build/outputs/apk/debug/app-debug.apk`

### 2. 安装到手机

```bash
adb install app-debug.apk
```

### 3. 启动服务

1. 打开 Alex Bridge 应用
2. 点击"开启无障碍权限"（跳转到系统设置）
3. 找到 Alex Bridge 并开启
4. 返回应用，点击"启动 HTTP 服务"
5. 通知栏显示"无障碍HTTP服务运行中"

### 4. 测试

```bash
# 在 Termux 中测试
curl http://localhost:8080/ping
# 返回: {"status":"ok"}

curl http://localhost:8080/dump
# 返回完整的 UI 树 JSON
```

## Python 客户端

```python
import requests
import json

BASE_URL = "http://localhost:8080"

# 获取 UI 树
r = requests.get(f"{BASE_URL}/dump")
elements = r.json()["elements"]

# 点击
requests.get(f"{BASE_URL}/tap?x=540&y=1000")

# 下滑
requests.get(f"{BASE_URL}/swipe?x1=540&y1=800&x2=540&y2=1500")
```

## 性能对比

| 方案 | 单次调用延迟 | 说明 |
|------|-------------|------|
| 文件桥接 | 2-3秒 | PRoot ↔ Termux |
| HTTP桥接 | ~50ms | PRoot → APK (本方案) |

## 技术栈

- Kotlin
- Android Accessibility Service
- NanoHTTPD (轻量级HTTP服务器)
- 前台服务保活

## 注意事项

1. **无障碍权限**：必须手动开启，系统不允许自动开启
2. **后台保活**：已使用前台服务，但 Android 10+ 仍可能杀后台，建议保持充电状态
3. **端口占用**：使用 8080 端口，确保无冲突
4. **网络隔离**：localhost 访问通常不受 SELinux 限制

## License

MIT
