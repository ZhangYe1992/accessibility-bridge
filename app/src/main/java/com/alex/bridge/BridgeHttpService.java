package com.alex.bridge;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 极简HTTP服务 - 只提供 /ping 和 /dump
 * 其他操作通过 ADB 桥接执行
 */
public class BridgeHttpService extends Service {
    private static final String TAG = "AlexBridge";
    private static final int PORT = 8080;
    private static final String CHANNEL_ID = "bridge_service";
    
    private HttpServer httpServer;
    
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        
        try {
            httpServer = new HttpServer(PORT);
            httpServer.start();
            Log.i(TAG, "HTTP服务器已启动，端口: " + PORT);
        } catch (IOException e) {
            Log.e(TAG, "HTTP服务器启动失败: " + e.getMessage());
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 启动为前台服务保活
        Notification notification = createNotification();
        startForeground(1, notification);
        Log.i(TAG, "HTTP服务已转为前台服务");
        
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (httpServer != null) {
            httpServer.stop();
            Log.i(TAG, "HTTP服务器已停止");
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Alex Bridge Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("UI Tree HTTP Service");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        );
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Alex Bridge")
                .setContentText("UI Tree服务运行中")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentIntent(pendingIntent)
                .build();
        } else {
            return new Notification.Builder(this)
                .setContentTitle("Alex Bridge")
                .setContentText("UI Tree服务运行中")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentIntent(pendingIntent)
                .build();
        }
    }
    
    private class HttpServer extends NanoHTTPD {
        
        public HttpServer(int port) {
            super(port);
        }
        
        @Override
        public Response serve(IHTTPSession session) {
            String uri = session.getUri();
            Log.d(TAG, "请求: " + uri);
            
            String responseBody;
            Status status = Status.OK;
            
            switch (uri) {
                case "/ping":
                    responseBody = "{\"status\":\"ok\",\"service\":\"Alex UI Bridge\"}";
                    break;
                    
                case "/dump":
                    responseBody = handleDump();
                    break;
                    
                default:
                    responseBody = "{\"error\":\"Unknown endpoint\",\"available\":[\"/ping\",\"/dump\"]}";
                    status = Status.NOT_FOUND;
            }
            
            Response response = Response.newFixedLengthResponse(status, "application/json", responseBody);
            response.addHeader("Access-Control-Allow-Origin", "*");
            return response;
        }
        
        private String handleDump() {
            BridgeAccessibilityService service = BridgeAccessibilityService.getInstance();
            if (service == null) {
                return "{\"error\":\"Accessibility service not connected\"}";
            }
            
            List<Map<String, Object>> elements = service.getUiTree();
            return toJson(elements);
        }
        
        private String toJson(List<Map<String, Object>> elements) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"count\":");
            sb.append(elements.size());
            sb.append(",\"elements\":[");
            
            for (int i = 0; i < elements.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(toJsonObject(elements.get(i)));
            }
            
            sb.append("]}");
            return sb.toString();
        }
        
        private String toJsonObject(Map<String, Object> map) {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
                Object value = entry.getValue();
                if (value instanceof String) {
                    sb.append("\"").append(escapeJson((String) value)).append("\"");
                } else if (value instanceof Boolean) {
                    sb.append(value);
                } else {
                    sb.append(value);
                }
            }
            sb.append("}");
            return sb.toString();
        }
        
        private String escapeJson(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
        }
    }
}
