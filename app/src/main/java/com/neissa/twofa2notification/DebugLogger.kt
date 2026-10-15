package com.neissa.twofa2notification

import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLogger {
    private const val TAG = "2FA_Debug"
    private const val PREFS_NAME = "2fa_prefs"
    const val KEY_LOGS = "debug_logs"

    fun log(context: Context, source: String, message: String) {
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val entry = "[$timeStr][$source] $message"
        Log.d(TAG, entry)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentLogs = prefs.getString(KEY_LOGS, "") ?: ""
        val updatedLogs = (entry + "\n" + currentLogs).take(4000)
        prefs.edit().putString(KEY_LOGS, updatedLogs).apply()
    }

    fun getLogs(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LOGS, "Aucun journal de débogage pour le moment.") ?: ""
    }

    fun clearLogs(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_LOGS).apply()
    }
}
