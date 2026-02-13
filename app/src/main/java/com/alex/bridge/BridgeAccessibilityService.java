package com.alex.bridge;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 极简无障碍服务 - 只负责获取UI树
 * 其他操作（点击/滑动/输入）通过 ADB 桥接执行
 */
public class BridgeAccessibilityService extends AccessibilityService {
    private static final String TAG = "AlexBridge";
    private static BridgeAccessibilityService instance;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.i(TAG, "无障碍服务已创建");
    }
    
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.i(TAG, "无障碍服务已连接");
    }
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 不需要处理事件
    }
    
    @Override
    public void onInterrupt() {
        Log.i(TAG, "无障碍服务被中断");
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        Log.i(TAG, "无障碍服务已销毁");
    }
    
    public static BridgeAccessibilityService getInstance() {
        return instance;
    }
    
    /**
     * 获取当前UI树 - 核心功能
     */
    public List<Map<String, Object>> getUiTree() {
        List<Map<String, Object>> elements = new ArrayList<>();
        AccessibilityNodeInfo root = getRootInActiveWindow();
        
        if (root != null) {
            traverseNode(root, elements, 0);
        }
        
        return elements;
    }
    
    private void traverseNode(AccessibilityNodeInfo node, List<Map<String, Object>> elements, int depth) {
        if (node == null || depth > 50) return;
        
        Map<String, Object> element = new HashMap<>();
        
        // 文本信息
        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();
        element.put("text", text != null ? text.toString() : "");
        element.put("desc", desc != null ? desc.toString() : "");
        
        // 标识信息
        element.put("id", node.getViewIdResourceName() != null ? node.getViewIdResourceName() : "");
        element.put("class", node.getClassName() != null ? node.getClassName().toString() : "");
        element.put("package", node.getPackageName() != null ? node.getPackageName().toString() : "");
        
        // 可交互状态
        element.put("clickable", node.isClickable());
        element.put("scrollable", node.isScrollable());
        element.put("focusable", node.isFocusable());
        element.put("enabled", node.isEnabled());
        
        // 边界坐标
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        element.put("x1", bounds.left);
        element.put("y1", bounds.top);
        element.put("x2", bounds.right);
        element.put("y2", bounds.bottom);
        element.put("cx", (bounds.left + bounds.right) / 2);
        element.put("cy", (bounds.top + bounds.bottom) / 2);
        
        elements.add(element);
        
        // 递归子节点
        for (int i = 0; i < node.getChildCount(); i++) {
            traverseNode(node.getChild(i), elements, depth + 1);
        }
    }
}
