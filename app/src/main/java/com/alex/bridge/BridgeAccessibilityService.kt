package com.alex.bridge

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.json.JSONArray
import org.json.JSONObject

/**
 * 无障碍服务 - 提供UI树获取和自动化操作
 */
class BridgeAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "BridgeAccessibility"
        var instance: BridgeAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "无障碍服务已连接")

        // 配置服务
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }

        // 启动HTTP服务
        startService(Intent(this, BridgeHttpService::class.java))
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 不处理事件，仅保持服务运行
    }

    override fun onInterrupt() {
        Log.w(TAG, "无障碍服务被中断")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.i(TAG, "无障碍服务已销毁")
    }

    /**
     * 获取当前UI树（JSON格式）
     */
    fun getUiTree(): JSONObject {
        val root = rootInActiveWindow
            ?: return JSONObject().put("error", "无法获取根窗口")

        return JSONObject().apply {
            put("package", root.packageName?.toString() ?: "")
            put("class", root.className?.toString() ?: "")
            put("elements", traverseNode(root))
        }
    }

    /**
     * 遍历节点，返回元素列表
     */
    private fun traverseNode(node: AccessibilityNodeInfo?): JSONArray {
        val result = JSONArray()
        if (node == null) return result

        // 当前节点
        val element = JSONObject().apply {
            put("text", node.text?.toString() ?: "")
            put("desc", node.contentDescription?.toString() ?: "")
            put("id", node.viewIdResourceName ?: "")
            put("class", node.className?.toString() ?: "")
            put("package", node.packageName?.toString() ?: "")
            put("clickable", node.isClickable)
            put("scrollable", node.isScrollable)
            put("bounds", getBoundsString(node))
            
            // 计算中心点坐标
            val rect = android.graphics.Rect()
            node.getBoundsInScreen(rect)
            put("cx", (rect.left + rect.right) / 2)
            put("cy", (rect.top + rect.bottom) / 2)
        }
        result.put(element)

        // 递归子节点
        for (i in 0 until node.childCount) {
            val childArray = traverseNode(node.getChild(i))
            for (j in 0 until childArray.length()) {
                result.put(childArray.get(j))
            }
        }

        return result
    }

    private fun getBoundsString(node: AccessibilityNodeInfo): String {
        val rect = android.graphics.Rect()
        node.getBoundsInScreen(rect)
        return "[${rect.left},${rect.top}][${rect.right},${rect.bottom}]"
    }

    /**
     * 点击坐标
     */
    fun tap(x: Int, y: Int): Boolean {
        val path = android.graphics.Path().apply {
            moveTo(x.toFloat(), y.toFloat())
        }
        val gesture = android.accessibilityservice.GestureDescription.Builder()
            .addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    /**
     * 滑动
     */
    fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, duration: Long = 300): Boolean {
        val path = android.graphics.Path().apply {
            moveTo(x1.toFloat(), y1.toFloat())
            lineTo(x2.toFloat(), y2.toFloat())
        }
        val gesture = android.accessibilityservice.GestureDescription.Builder()
            .addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, duration))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    /**
     * 输入文本（需先聚焦输入框）
     */
    fun inputText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focusNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: return false
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return focusNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    /**
     * 返回
     */
    fun back(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /**
     * 主页
     */
    fun home(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    /**
     * 电源键（唤醒/息屏）
     */
    fun power(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
    }
}
