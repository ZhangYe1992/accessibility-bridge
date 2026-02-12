# Accessibility Bridge - 快速开始

## 方式1: 本地编译（推荐）

### 环境要求
- Android Studio 或命令行构建工具
- JDK 11+
- Android SDK 34

### 编译步骤

```bash
# 1. 克隆仓库
git clone https://github.com/ZhangYe1992/accessibility-bridge.git
cd accessibility-bridge

# 2. 编译APK
./gradlew assembleDebug

# 3. APK输出路径
app/build/outputs/apk/debug/app-debug.apk
```

### 安装
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 方式2: GitHub Actions自动构建

在仓库创建 `.github/workflows/build.yml`，每次推送自动编译APK。

## 使用

### 1. 启动服务
- 打开 Alex Bridge 应用
- 点击"开启无障碍权限"
- 在系统设置中找到 Alex Bridge 并开启
- 返回应用，点击"启动HTTP服务"

### 2. 测试连接
```bash
curl http://localhost:8080/ping
# 返回: {"status":"ok"}
```

### 3. Python使用
```python
import sys
sys.path.insert(0, '/path/to/bridge/client')
from bridge_client import BridgeClient

client = BridgeClient()
elements = client.get_ui_tree()
print(f"获取到 {len(elements)} 个元素")

# 点击
client.tap(540, 1000)

# 下滑
client.swipe(648, 819, 648, 1521, 400)
```

## 保活配置（Root设备）

### 使用 Migisk 模块
创建 `/data/adb/modules/alex-bridge/service.sh`:

```bash
#!/system/bin/sh
# 开机启动 Bridge 服务

while [ ! -d /data/data/com.alex.bridge ]; do
    sleep 5
done

# 启动应用
am start -n com.alex.bridge/.MainActivity

# 等待无障碍服务启动
sleep 3

# 启动HTTP服务
am startservice -n com.alex.bridge/.BridgeHttpService
```

或使用 `pm grant` + `settings put` 自动授权。

## API文档

| 端点 | 方法 | 参数 | 说明 |
|------|------|------|------|
| `/ping` | GET | - | 健康检查 |
| `/dump` | GET | - | 获取UI树 |
| `/tap` | GET | x, y | 点击坐标 |
| `/swipe` | GET | x1, y1, x2, y2, duration | 滑动 |
| `/back` | GET | - | 返回 |
| `/home` | GET | - | 主页 |
| `/wake` | GET | - | 唤醒 |
| `/input` | GET | text | 输入文本 |

## 性能对比

| 方案 | 延迟 | 说明 |
|------|------|------|
| 文件桥接 | 2-3秒 | PRoot ↔ Termux |
| **HTTP桥接** | **~50ms** | **本方案** |
