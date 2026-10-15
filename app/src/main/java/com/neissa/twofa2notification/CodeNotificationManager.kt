package com.neissa.twofa2notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object CodeNotificationManager {
    const val CHANNEL_ID = "2fa_codes"
    const val NOTIFICATION_ID = 1001
    private const val REQUEST_COPY = 1001
    private const val REQUEST_CHECK_MAIL = 1002
    private const val PREFS_NAME = "2fa_prefs"
    private const val KEY_LAST_CODE = "last_code"
    private const val KEY_LAST_CODE_TIME = "last_code_time"

    fun updateNotification(context: Context, code: String?) {
        val now = System.currentTimeMillis()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        if (code != null) {
            prefs.edit()
                .putString(KEY_LAST_CODE, code)
                .putLong(KEY_LAST_CODE_TIME, now)
                .apply()
        }

        val lastCode = prefs.getString(KEY_LAST_CODE, null)
        val lastCodeTime = prefs.getLong(KEY_LAST_CODE_TIME, 0L)
        val ageMs = now - lastCodeTime
        val tenMinutesMs = CodeProcessor.MAX_AGE_MS

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ),
        )

        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setOngoing(true)
            .setAutoCancel(false)

        if (lastCode != null && ageMs <= tenMinutesMs) {
            val copyIntent = Intent(context, CopyReceiver::class.java).putExtra(CopyReceiver.EXTRA_CODE, lastCode)
            val copyPendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_COPY,
                copyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            builder.setContentTitle(lastCode)
                .setContentIntent(copyPendingIntent)
                .addAction(
                    Notification.Action.Builder(null, context.getString(R.string.copy_code, lastCode), copyPendingIntent).build(),
                )
        } else {
            builder.setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(R.string.waiting_for_code))
        }

        val checkIntent = Intent(context, CheckMailReceiver::class.java)
            .setAction(CheckMailReceiver.ACTION_CHECK_MAIL)
        val checkPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CHECK_MAIL,
            checkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        builder.addAction(
            Notification.Action.Builder(null, context.getString(R.string.action_check_mail), checkPendingIntent).build(),
        )

        manager.notify(NOTIFICATION_ID, builder.build())
    }
}
