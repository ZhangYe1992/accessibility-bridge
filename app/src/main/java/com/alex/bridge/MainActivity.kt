package com.alex.bridge

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * 主界面 - 用于启动和配置服务
 */
class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnEnableAccessibility: Button
    private lateinit var btnStartService: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        btnEnableAccessibility = findViewById(R.id.btnEnableAccessibility)
        btnStartService = findViewById(R.id.btnStartService)

        btnEnableAccessibility.setOnClickListener {
            // 打开无障碍设置
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        btnStartService.setOnClickListener {
            // 启动HTTP服务
            startService(Intent(this, BridgeHttpService::class.java))
            updateStatus()
        }

        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val accessibilityEnabled = BridgeAccessibilityService.instance != null
        val httpRunning = BridgeHttpService.instance != null

        val status = when {
            accessibilityEnabled && httpRunning -> "✅ 服务运行正常\nHTTP端口: 8080"
            accessibilityEnabled -> "⚠️ HTTP服务未启动"
            else -> "❌ 无障碍服务未启动\n请先开启无障碍权限"
        }
        tvStatus.text = status
    }
}
