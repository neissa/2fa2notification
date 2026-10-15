package com.neissa.twofa2notification

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast

class CopyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val code = intent.getStringExtra(EXTRA_CODE) ?: return
        context.getSystemService(ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText("2FA", code))
        Toast.makeText(context, context.getString(R.string.copied, code), Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_CODE = "code"
    }
}
