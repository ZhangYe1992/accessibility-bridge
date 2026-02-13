package com.alex.bridge;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 极简主界面 - UI Tree服务控制
 */
public class MainActivity extends Activity {
    private static final String TAG = "AlexBridge";
    
    private TextView tvStatus;
    private Button btnAccessibility;
    private Button btnStartService;
    private Button btnStopService;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createUI();
        updateStatus();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }
    
    private void createUI() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        
        // 标题
        TextView tvTitle = new TextView(this);
        tvTitle.setText("Alex UI Bridge");
        tvTitle.setTextSize(28);
        tvTitle.setPadding(0, 0, 0, 10);
        layout.addView(tvTitle);
        
        // 副标题
        TextView tvSubtitle = new TextView(this);
        tvSubtitle.setText("只获取UI树，操作走ADB桥接\n低延迟 ~50ms");
        tvSubtitle.setTextSize(14);
        tvSubtitle.setPadding(0, 0, 0, 40);
        layout.addView(tvSubtitle);
        
        // 状态显示
        tvStatus = new TextView(this);
        tvStatus.setTextSize(16);
        tvStatus.setPadding(20, 20, 20, 20);
        tvStatus.setBackgroundColor(0xFFEEEEEE);
        layout.addView(tvStatus);
        
        // 按钮容器
        LinearLayout btnLayout = new LinearLayout(this);
        btnLayout.setOrientation(LinearLayout.VERTICAL);
        btnLayout.setPadding(0, 30, 0, 0);
        
        btnAccessibility = new Button(this);
        btnAccessibility.setText("1. 开启无障碍权限");
        btnAccessibility.setOnClickListener(v -> openAccessibilitySettings());
        btnLayout.addView(btnAccessibility);
        
        btnStartService = new Button(this);
        btnStartService.setText("2. 启动UI Tree服务");
        btnStartService.setOnClickListener(v -> startHttpService());
        btnLayout.addView(btnStartService);
        
        btnStopService = new Button(this);
        btnStopService.setText("停止服务");
        btnStopService.setOnClickListener(v -> stopHttpService());
        btnLayout.addView(btnStopService);
        
        layout.addView(btnLayout);
        
        // 使用说明
        TextView tvDoc = new TextView(this);
        tvDoc.setText("\n\n使用方式:\n\n"
            + "1. Python获取UI树:\n"
            + "   requests.get('http://localhost:8080/dump')\n\n"
            + "2. ADB执行操作:\n"
            + "   su -c 'input tap x y'\n"
            + "   su -c 'input swipe x1 y1 x2 y2'\n"
            + "   su -c 'input text hello'\n\n"
            + "骚方案 = HTTP看 + ADB干");
        tvDoc.setTextSize(12);
        tvDoc.setPadding(0, 40, 0, 0);
        layout.addView(tvDoc);
        
        scrollView.addView(layout);
        setContentView(scrollView);
    }
    
    private void updateStatus() {
        boolean hasAccessibility = BridgeAccessibilityService.getInstance() != null;
        
        StringBuilder status = new StringBuilder();
        status.append("📊 状态\n");
        status.append("━━━━━━━━━━━━━━\n");
        status.append("无障碍: ").append(hasAccessibility ? "✅" : "❌").append("\n");
        status.append("HTTP: http://localhost:8080\n");
        status.append("\n端点:\n");
        status.append("  GET /ping - 检查\n");
        status.append("  GET /dump - UI树\n");
        
        tvStatus.setText(status.toString());
        
        btnAccessibility.setEnabled(!hasAccessibility);
        btnStartService.setEnabled(hasAccessibility);
    }
    
    private void openAccessibilitySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            Toast.makeText(this, "找到 Alex Bridge 并开启", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "打开设置失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void startHttpService() {
        try {
            Intent intent = new Intent(this, BridgeHttpService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            Toast.makeText(this, "服务已启动", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "启动失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void stopHttpService() {
        try {
            stopService(new Intent(this, BridgeHttpService.class));
            Toast.makeText(this, "服务已停止", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "停止失败", Toast.LENGTH_SHORT).show();
        }
    }
}
