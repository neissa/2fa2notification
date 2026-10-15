package com.neissa.twofa2notification

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var logView: TextView
    private lateinit var gmailStatusView: TextView
    private lateinit var permissionsStatusView: TextView
    private lateinit var batteryStatusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logView = TextView(this).apply { textSize = 11f; setTextIsSelectable(true) }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }
        root.addView(TextView(this).apply { setText(R.string.description) })
        gmailStatusView = TextView(this).apply {
            textSize = 16f
            setPadding(0, 24, 0, 24)
        }
        root.addView(gmailStatusView)
        permissionsStatusView = TextView(this).apply {
            textSize = 16f
            setPadding(0, 0, 0, 24)
        }
        root.addView(permissionsStatusView)
        batteryStatusView = TextView(this).apply {
            textSize = 16f
            setPadding(0, 0, 0, 24)
        }
        root.addView(batteryStatusView)
        root.addView(button(R.string.btn_battery) { requestIgnoreBatteryOptimizations() })
        root.addView(button(R.string.btn_grant_permissions) { requestAllPermissions() })
        root.addView(button(R.string.btn_sign_in_gmail) {
            GmailApiClient.authorize(this, RC_SIGN_IN)
        })
        root.addView(button(R.string.btn_check_gmail) {
            Thread {
                GmailApiClient.checkRecentMails(this)
                runOnUiThread { refreshLogs() }
            }.start()
        })
        root.addView(button(R.string.btn_scan_sms) {
            Thread {
                val count = SmsInboxScanner.scanRecent(this)
                runOnUiThread {
                    Toast.makeText(this, getString(R.string.scan_done, count), Toast.LENGTH_SHORT).show()
                    refreshLogs()
                }
            }.start()
        })
        root.addView(button(R.string.btn_clear_logs) {
            DebugLogger.clearLogs(this)
            refreshLogs()
        })
        root.addView(TextView(this).apply {
            setText(R.string.debug_title)
            setPadding(0, 32, 0, 8)
        })
        root.addView(logView)

        setContentView(ScrollView(this).apply { addView(root) })
        refreshLogs()
    }

    @Deprecated("Deprecated in Activity, kept for simple Google Sign-In result handling")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != RC_SIGN_IN) return
        val ok = GmailApiClient.handleAuthorizationResult(this, data)
        Toast.makeText(
            this,
            if (ok) R.string.gmail_signed_in else R.string.gmail_sign_in_failed,
            Toast.LENGTH_LONG,
        ).show()
        refreshLogs()
    }

    override fun onResume() {
        super.onResume()
        refreshLogs()
        CodeNotificationService.start(this)
        CodeNotificationManager.updateNotification(this, null)
    }

    private fun button(textRes: Int, onClick: () -> Unit) = Button(this).apply {
        setText(textRes)
        setOnClickListener { onClick() }
    }

    private fun requestAllPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        requestPermissions(permissions.toTypedArray(), 1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        refreshLogs()
    }

    private fun permissionsStatusText(): String {
        val required = mutableListOf(
            Manifest.permission.RECEIVE_SMS to R.string.perm_sms_receive,
            Manifest.permission.READ_SMS to R.string.perm_sms_read,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            required += Manifest.permission.POST_NOTIFICATIONS to R.string.perm_notifications
        }
        val missing = required
            .filter { checkSelfPermission(it.first) != PackageManager.PERMISSION_GRANTED }
            .map { getString(it.second) }
        return if (missing.isEmpty()) {
            getString(R.string.permissions_ok)
        } else {
            getString(R.string.permissions_missing, missing.joinToString(", "))
        }
    }

    private fun isIgnoringBatteryOptimizations(): Boolean =
        getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)

    private fun requestIgnoreBatteryOptimizations() {
        if (isIgnoringBatteryOptimizations()) {
            Toast.makeText(this, R.string.battery_ok, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")),
            )
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    private fun refreshLogs() {
        permissionsStatusView.text = permissionsStatusText()
        batteryStatusView.setText(
            if (isIgnoringBatteryOptimizations()) R.string.battery_ok else R.string.battery_active,
        )
        gmailStatusView.setText(
            if (GmailApiClient.isAuthorized(this)) R.string.gmail_status_connected
            else R.string.gmail_status_disconnected,
        )
        logView.text = DebugLogger.getLogs(this)
    }

    private companion object {
        const val RC_SIGN_IN = 2001
    }
}
