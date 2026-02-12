#!/usr/bin/env python3
"""
Alex Accessibility Bridge - Python 客户端

替代原有的文件桥接方案，使用HTTP接口访问无障碍服务
"""

import requests
import json
import time
from typing import List, Dict, Optional, Tuple

BASE_URL = "http://localhost:8080"


class BridgeClient:
    """
    桥接客户端 - 通过HTTP访问无障碍服务
    """
    
    def __init__(self, base_url: str = BASE_URL):
        self.base_url = base_url
        self.timeout = 5
    
    def ping(self) -> bool:
        """检查服务是否运行"""
        try:
            r = requests.get(f"{self.base_url}/ping", timeout=self.timeout)
            return r.json().get("status") == "ok"
        except:
            return False
    
    def get_ui_tree(self) -> List[Dict]:
        """获取UI树"""
        try:
            r = requests.get(f"{self.base_url}/dump", timeout=self.timeout)
            data = r.json()
            if "error" in data:
                return []
            return data.get("elements", [])
        except Exception as e:
            print(f"获取UI树失败: {e}")
            return []
    
    def tap(self, x: int, y: int) -> bool:
        """点击坐标"""
        try:
            r = requests.get(
                f"{self.base_url}/tap",
                params={"x": x, "y": y},
                timeout=self.timeout
            )
            return r.json().get("success", False)
        except:
            return False
    
    def swipe(self, x1: int, y1: int, x2: int, y2: int, duration: int = 300) -> bool:
        """滑动"""
        try:
            r = requests.get(
                f"{self.base_url}/swipe",
                params={"x1": x1, "y1": y1, "x2": x2, "y2": y2, "duration": duration},
                timeout=self.timeout
            )
            return r.json().get("success", False)
        except:
            return False
    
    def back(self) -> bool:
        """返回"""
        try:
            r = requests.get(f"{self.base_url}/back", timeout=self.timeout)
            return r.json().get("success", False)
        except:
            return False
    
    def home(self) -> bool:
        """主页"""
        try:
            r = requests.get(f"{self.base_url}/home", timeout=self.timeout)
            return r.json().get("success", False)
        except:
            return False
    
    def wake(self) -> bool:
        """唤醒屏幕"""
        try:
            r = requests.get(f"{self.base_url}/wake", timeout=self.timeout)
            return r.json().get("success", False)
        except:
            return False
    
    def input_text(self, text: str) -> bool:
        """输入文本"""
        try:
            r = requests.get(
                f"{self.base_url}/input",
                params={"text": text},
                timeout=self.timeout
            )
            return r.json().get("success", False)
        except:
            return False


# 兼容性接口 - 与原有 android_body 保持一致
def get_body():
    """获取客户端实例（兼容原有接口）"""
    return BridgeClient()


def see() -> List[Dict]:
    """获取屏幕元素（兼容原有接口）"""
    client = BridgeClient()
    return client.get_ui_tree()


def tap(x: int, y: int) -> bool:
    """点击（兼容原有接口）"""
    client = BridgeClient()
    return client.tap(x, y)


def swipe(x1: int, y1: int, x2: int, y2: int, duration: int = 300) -> bool:
    """滑动（兼容原有接口）"""
    client = BridgeClient()
    return client.swipe(x1, y1, x2, y2, duration)


def swipe_down(times: int = 1, duration: int = 500) -> bool:
    """下滑（兼容原有接口）"""
    client = BridgeClient()
    # 屏幕中间偏右位置
    cx = 648  # 1080 * 0.6
    y1 = 819   # 2340 * 0.35
    y2 = 1521  # 2340 * 0.65
    
    for _ in range(times):
        if not client.swipe(cx, y1, cx, y2, duration):
            return False
        time.sleep(0.3)
    return True


def back() -> bool:
    """返回（兼容原有接口）"""
    client = BridgeClient()
    return client.back()


def home() -> bool:
    """主页（兼容原有接口）"""
    client = BridgeClient()
    return client.home()


if __name__ == "__main__":
    print("测试 BridgeClient...")
    client = BridgeClient()
    
    # 测试连接
    if client.ping():
        print("✅ 服务连接成功")
    else:
        print("❌ 服务未启动")
        exit(1)
    
    # 测试获取UI
    start = time.time()
    elements = client.get_ui_tree()
    elapsed = time.time() - start
    print(f"获取UI: {len(elements)} 个元素, 耗时 {elapsed*1000:.1f}ms")
    
    if elements:
        print("前5个元素:")
        for e in elements[:5]:
            text = e.get('text', '')[:20]
            print(f"  - {text} @ ({e.get('cx')}, {e.get('cy')})")
