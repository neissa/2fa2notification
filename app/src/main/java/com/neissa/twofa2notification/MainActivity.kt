package com.neissa.twofa2notification

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var logView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logView = TextView(this).apply { textSize = 11f; setTextIsSelectable(true) }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }
        root.addView(TextView(this).apply { setText(R.string.description) })
        root.addView(button(R.string.btn_grant_permissions) { requestAllPermissions() })
        root.addView(button(R.string.btn_enable_accessibility) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
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
            val count = SmsInboxScanner.scanRecent(this)
            Toast.makeText(this, getString(R.string.scan_done, count), Toast.LENGTH_SHORT).show()
            refreshLogs()
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

    private fun refreshLogs() {
        logView.text = DebugLogger.getLogs(this)
    }

    private companion object {
        const val RC_SIGN_IN = 2001
    }
}
