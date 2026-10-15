package com.neissa.twofa2notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CodeNotificationManager {
    const val CHANNEL_ID = "2fa_codes"
    const val NOTIFICATION_ID = 1001
    private const val REQUEST_COPY = 1001
    private const val REQUEST_CHECK_MAIL = 1002
    private const val PREFS_NAME = "2fa_prefs"
    private const val KEY_LAST_CODE = "last_code"
    private const val KEY_LAST_CODE_TIME = "last_code_time"
    private const val KEY_LAST_CODE_SENDER = "last_code_sender"

    /** Heure (ms) de réception du dernier code trouvé, 0 si aucun. */
    fun lastCodeTime(context: Context): Long =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getLong(KEY_LAST_CODE_TIME, 0L)

    /** Enregistre le code (si fourni, avec son expéditeur et sa date de réception) et met à jour la notification. */
    fun updateNotification(
        context: Context,
        code: String?,
        status: String? = null,
        codeTime: Long = System.currentTimeMillis(),
        sender: String? = null,
    ) {
        if (code != null) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_LAST_CODE, code)
                .putLong(KEY_LAST_CODE_TIME, codeTime)
                .putString(KEY_LAST_CODE_SENDER, sender ?: "inconnu")
                .apply()
        }
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.notify(NOTIFICATION_ID, buildNotification(context, status))
    }

    /** Construit la notification permanente (utilisée aussi par le service de premier plan). */
    fun buildNotification(context: Context, status: String? = null): Notification {
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ),
        )

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCode = prefs.getString(KEY_LAST_CODE, null)
        val lastTime = prefs.getLong(KEY_LAST_CODE_TIME, 0L)
        val ageMs = System.currentTimeMillis() - lastTime

        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)

        if (lastCode != null && ageMs <= CodeProcessor.MAX_AGE_MS) {
            val sender = prefs.getString(KEY_LAST_CODE_SENDER, null) ?: "inconnu"
            val date = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(lastTime))
            val copyIntent = Intent(context, CopyReceiver::class.java).putExtra(CopyReceiver.EXTRA_CODE, lastCode)
            val copyPendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_COPY,
                copyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            builder.setContentTitle(lastCode)
                .setContentText(context.getString(R.string.notif_info, sender, date))
                .setContentIntent(copyPendingIntent)
                .addAction(
                    Notification.Action.Builder(null, context.getString(R.string.copy_code, lastCode), copyPendingIntent).build(),
                )
        } else {
            builder.setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(R.string.waiting_for_code))
        }

        val checkPendingIntent = PendingIntent.getForegroundService(
            context,
            REQUEST_CHECK_MAIL,
            CodeNotificationService.checkIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        builder.addAction(
            Notification.Action.Builder(null, context.getString(R.string.action_check_mail), checkPendingIntent).build(),
        )

        status?.let { builder.setContentText(it) }

        return builder.build()
    }
}
