package com.alex.bridge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject
import java.io.IOException

/**
 * HTTP服务 - 暴露API供外部调用
 * 端口: 8080
 */
class BridgeHttpService : Service() {

    companion object {
        private const val TAG = "BridgeHttp"
        private const val PORT = 8080
        private const val CHANNEL_ID = "bridge_service_channel"
    }

    private var httpServer: HttpServer? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "HTTP服务创建")
        createNotificationChannel()
        startForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (httpServer == null) {
            try {
                httpServer = HttpServer(PORT)
                httpServer?.start()
                Log.i(TAG, "HTTP服务器启动在端口 $PORT")
            } catch (e: IOException) {
                Log.e(TAG, "HTTP服务器启动失败: ${e.message}")
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        httpServer?.stop()
        httpServer = null
        Log.i(TAG, "HTTP服务已销毁")
    }

    private fun startForeground() {
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Alex Bridge")
            .setContentText("无障碍HTTP服务运行中")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
        startForeground(1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bridge Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * NanoHTTPD HTTP服务器
     */
    inner class HttpServer(port: Int) : NanoHTTPD("localhost", port) {

        override fun serve(session: IHTTPSession): Response {
            val uri = session.uri
            val method = session.method
            val params = session.parameters

            Log.d(TAG, "请求: $method $uri")

            return when {
                // 健康检查
                uri == "/ping" -> newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    JSONObject().put("status", "ok").toString()
                )

                // 获取UI树
                uri == "/dump" -> handleDump()

                // 点击
                uri == "/tap" -> handleTap(params)

                // 滑动
                uri == "/swipe" -> handleSwipe(params)

                // 输入文本
                uri == "/input" -> handleInput(params)

                // 返回
                uri == "/back" -> handleBack()

                // 主页
                uri == "/home" -> handleHome()

                // 唤醒
                uri == "/wake" -> handleWake()

                else -> newFixedLengthResponse(
                    Response.Status.NOT_FOUND,
                    "application/json",
                    JSONObject().put("error", "未知端点").toString()
                )
            }
        }

        private fun handleDump(): Response {
            val service = BridgeAccessibilityService.instance
                ?: return errorResponse("无障碍服务未启动")

            return try {
                val json = service.getUiTree()
                jsonResponse(json)
            } catch (e: Exception) {
                errorResponse("获取UI树失败: ${e.message}")
            }
        }

        private fun handleTap(params: Map<String, List<String>>): Response {
            val service = BridgeAccessibilityService.instance
                ?: return errorResponse("无障碍服务未启动")

            val x = params["x"]?.firstOrNull()?.toIntOrNull()
            val y = params["y"]?.firstOrNull()?.toIntOrNull()

            if (x == null || y == null) {
                return errorResponse("缺少参数: x, y")
            }

            val success = service.tap(x, y)
            return jsonResponse(JSONObject().put("success", success))
        }

        private fun handleSwipe(params: Map<String, List<String>>): Response {
            val service = BridgeAccessibilityService.instance
                ?: return errorResponse("无障碍服务未启动")

            val x1 = params["x1"]?.firstOrNull()?.toIntOrNull()
            val y1 = params["y1"]?.firstOrNull()?.toIntOrNull()
            val x2 = params["x2"]?.firstOrNull()?.toIntOrNull()
            val y2 = params["y2"]?.firstOrNull()?.toIntOrNull()
            val duration = params["duration"]?.firstOrNull()?.toLongOrNull() ?: 300

            if (x1 == null || y1 == null || x2 == null || y2 == null) {
                return errorResponse("缺少参数: x1, y1, x2, y2")
            }

            val success = service.swipe(x1, y1, x2, y2, duration)
            return jsonResponse(JSONObject().put("success", success))
        }

        private fun handleInput(params: Map<String, List<String>>): Response {
            val service = BridgeAccessibilityService.instance
                ?: return errorResponse("无障碍服务未启动")

            val text = params["text"]?.firstOrNull()
                ?: return errorResponse("缺少参数: text")

            val success = service.inputText(text)
            return jsonResponse(JSONObject().put("success", success))
        }

        private fun handleBack(): Response {
            val service = BridgeAccessibilityService.instance
                ?: return errorResponse("无障碍服务未启动")
            val success = service.back()
            return jsonResponse(JSONObject().put("success", success))
        }

        private fun handleHome(): Response {
            val service = BridgeAccessibilityService.instance
                ?: return errorResponse("无障碍服务未启动")
            val success = service.home()
            return jsonResponse(JSONObject().put("success", success))
        }

        private fun handleWake(): Response {
            val service = BridgeAccessibilityService.instance
                ?: return errorResponse("无障碍服务未启动")
            val success = service.power()
            return jsonResponse(JSONObject().put("success", success))
        }

        private fun jsonResponse(json: JSONObject): Response {
            return newFixedLengthResponse(
                Response.Status.OK,
                "application/json",
                json.toString()
            )
        }

        private fun errorResponse(message: String): Response {
            return newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "application/json",
                JSONObject().put("error", message).toString()
            )
        }
    }
}
