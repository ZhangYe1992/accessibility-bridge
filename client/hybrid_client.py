#!/usr/bin/env python3
"""
Alex Accessibility Bridge - 混合客户端 (骚方案)

获取UI树: HTTP → APK (快，~50ms)
执行操作: ADB桥接 → su -c input (稳，可靠)
"""

import requests
import json
import os
import time
from typing import List, Dict, Optional, Tuple

# HTTP配置 - 用于获取UI树
HTTP_BASE_URL = "http://localhost:8080"

# 桥接配置 - 用于执行操作
TERMUX_BRIDGE_DIR = "/data/data/com.termux/files/home/.android-bridge"
BRIDGE_INPUT = f"{TERMUX_BRIDGE_DIR}/input.json"
BRIDGE_OUTPUT = f"{TERMUX_BRIDGE_DIR}/output.json"
BRIDGE_LOCK = f"{TERMUX_BRIDGE_DIR}/lock"


class HybridBridgeClient:
    """
    混合桥接客户端
    - 看: HTTP → APK
    - 干: ADB桥接
    """
    
    def __init__(self):
        self.screen_width = 1080
        self.screen_height = 2340
        self._get_screen_size()
    
    def ensure_bridge(self):
        """确保桥接目录存在"""
        os.makedirs(TERMUX_BRIDGE_DIR, exist_ok=True)
    
    def _exec_adb(self, command: str, timeout: int = 10) -> Tuple[bool, str]:
        """
        通过文件桥接执行ADB命令
        用于: tap, swipe, input, back, home 等操作
        """
        self.ensure_bridge()
        
        # 清理旧文件
        for f in [BRIDGE_INPUT, BRIDGE_OUTPUT, BRIDGE_LOCK]:
            if os.path.exists(f):
                os.remove(f)
        
        # 包装root命令
        root_command = f'su -c "{command}"'
        
        bridge_data = {
            "command": root_command,
            "timestamp": time.time()
        }
        
        with open(BRIDGE_INPUT, 'w') as f:
            json.dump(bridge_data, f)
        
        with open(BRIDGE_LOCK, 'w') as f:
            f.write('1')
        
        # 等待结果
        start_time = time.time()
        while time.time() - start_time < timeout:
            if os.path.exists(BRIDGE_OUTPUT):
                try:
                    with open(BRIDGE_OUTPUT, 'r') as f:
                        result = json.load(f)
                    for f in [BRIDGE_INPUT, BRIDGE_OUTPUT, BRIDGE_LOCK]:
                        if os.path.exists(f):
                            os.remove(f)
                    return result.get('success', False), result.get('output', '')
                except:
                    pass
            time.sleep(0.05)
        
        return False, "超时"
    
    def _get_screen_size(self):
        """获取屏幕尺寸"""
        success, output = self._exec_adb("wm size")
        if success and 'x' in output:
            try:
                size = output.split(':')[-1].strip()
                w, h = size.split('x')
                self.screen_width = int(w)
                self.screen_height = int(h)
            except:
                pass
    
    # ========== HTTP获取UI树 (快) ==========
    
    def ping(self) -> bool:
        """检查服务是否运行"""
        try:
            r = requests.get(f"{HTTP_BASE_URL}/ping", timeout=2)
            return r.json().get("status") == "ok"
        except:
            return False
    
    def get_ui_tree(self) -> List[Dict]:
        """
        获取UI树 - HTTP方式，~50ms
        """
        try:
            r = requests.get(f"{HTTP_BASE_URL}/dump", timeout=5)
            data = r.json()
            if "error" in data:
                return []
            return data.get("elements", [])
        except Exception as e:
            print(f"获取UI树失败: {e}")
            return []
    
    def see_text(self) -> str:
        """以文本形式查看可点击元素"""
        elements = self.get_ui_tree()
        if not elements:
            return "无法获取屏幕内容"
        
        lines = ["📱 当前屏幕:", "=" * 50]
        clickable = [e for e in elements if e.get('clickable')]
        
        for i, elem in enumerate(clickable[:15], 1):
            text = elem.get('text', '') or elem.get('desc', '') or '[无文本]'
            text = text[:20]
            lines.append(f"{i}. {text} @ ({elem.get('cx')}, {elem.get('cy')})")
        
        if len(clickable) > 15:
            lines.append(f"... 还有 {len(clickable) - 15} 个元素")
        
        return "\n".join(lines)
    
    # ========== ADB执行操作 (稳) ==========
    
    def tap(self, x: int, y: int) -> bool:
        """点击 - ADB方式"""
        print(f"👆 点击 ({x}, {y})")
        success, _ = self._exec_adb(f"input tap {x} {y}")
        time.sleep(0.3)
        return success
    
    def tap_text(self, text: str) -> bool:
        """根据文本点击（自动查找坐标）"""
        elements = self.get_ui_tree()  # HTTP获取UI树
        for elem in elements:
            if text in elem.get('text', '') or text in elem.get('desc', ''):
                cx, cy = elem.get('cx'), elem.get('cy')
                if cx and cy:
                    print(f"🎯 找到'{text}'，点击 ({cx}, {cy})")
                    return self.tap(cx, cy)
        print(f"❌ 未找到文本: {text}")
        return False
    
    def swipe(self, x1: int, y1: int, x2: int, y2: int, duration: int = 300) -> bool:
        """滑动 - ADB方式"""
        print(f"👆 滑动 ({x1},{y1}) → ({x2},{y2})")
        success, _ = self._exec_adb(f"input swipe {x1} {y1} {x2} {y2} {duration}")
        time.sleep(0.3)
        return success
    
    def swipe_up(self, times: int = 1, duration: int = 500) -> bool:
        """上滑"""
        cx = int(self.screen_width * 0.6)
        y1 = int(self.screen_height * 0.65)
        y2 = int(self.screen_height * 0.35)
        for _ in range(times):
            self.swipe(cx, y1, cx, y2, duration)
            time.sleep(0.3)
        return True
    
    def swipe_down(self, times: int = 1, duration: int = 500) -> bool:
        """下滑"""
        cx = int(self.screen_width * 0.6)
        y1 = int(self.screen_height * 0.35)
        y2 = int(self.screen_height * 0.65)
        for _ in range(times):
            self.swipe(cx, y1, cx, y2, duration)
            time.sleep(0.3)
        return True
    
    def type_text(self, text: str) -> bool:
        """输入文本 - ADB方式"""
        safe_text = text.replace('"', '\\"').replace(' ', '%s')
        print(f"⌨️ 输入: {text}")
        success, _ = self._exec_adb(f'input text "{safe_text}"')
        return success
    
    def back(self) -> bool:
        """返回"""
        print("🔙 返回")
        success, _ = self._exec_adb("input keyevent KEYCODE_BACK")
        time.sleep(0.3)
        return success
    
    def home(self) -> bool:
        """主页"""
        print("🏠 主页")
        success, _ = self._exec_adb("input keyevent KEYCODE_HOME")
        time.sleep(0.3)
        return success
    
    def power(self) -> bool:
        """电源键"""
        success, _ = self._exec_adb("input keyevent KEYCODE_POWER")
        return success
    
    def open_url(self, url: str) -> bool:
        """打开URL"""
        print(f"🌐 打开: {url}")
        cmd = f"am start -a android.intent.action.VIEW -d '{url}'"
        success, _ = self._exec_adb(cmd)
        time.sleep(2)
        return success


# 全局实例
_client = None

def get_client() -> HybridBridgeClient:
    """获取客户端实例"""
    global _client
    if _client is None:
        _client = HybridBridgeClient()
    return _client


# 快捷函数（与原android_body兼容）
def see() -> List[Dict]:
    return get_client().get_ui_tree()

def see_text() -> str:
    return get_client().see_text()

def tap(x: int, y: int) -> bool:
    return get_client().tap(x, y)

def tap_text(text: str) -> bool:
    return get_client().tap_text(text)

def swipe_up(times: int = 1, duration: int = 500) -> bool:
    return get_client().swipe_up(times, duration)

def swipe_down(times: int = 1, duration: int = 500) -> bool:
    return get_client().swipe_down(times, duration)

def type_text(text: str) -> bool:
    return get_client().type_text(text)

def back() -> bool:
    return get_client().back()

def home() -> bool:
    return get_client().home()

def open_url(url: str) -> bool:
    return get_client().open_url(url)


if __name__ == "__main__":
    print("🤖 Alex Hybrid Bridge Client")
    print("=" * 50)
    
    client = get_client()
    
    # 测试HTTP连接
    if client.ping():
        print("✅ HTTP UI服务连接成功")
    else:
        print("❌ HTTP UI服务未启动，请先启动APK服务")
        exit(1)
    
    # 测试获取UI树
    start = time.time()
    elements = client.get_ui_tree()
    elapsed = (time.time() - start) * 1000
    print(f"✅ UI树获取: {len(elements)} 个元素, 耗时 {elapsed:.1f}ms")
    
    # 显示前5个可点击元素
    clickable = [e for e in elements if e.get('clickable')][:5]
    if clickable:
        print("\n可点击元素:")
        for e in clickable:
            text = (e.get('text', '') or e.get('desc', ''))[:20]
            print(f"  - {text} @ ({e.get('cx')}, {e.get('cy')})")
    
    print("\n骚方案就绪: HTTP看 + ADB干")
