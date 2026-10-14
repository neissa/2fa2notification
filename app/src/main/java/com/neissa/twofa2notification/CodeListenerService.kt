package com.neissa.twofa2notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class CodeListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val extras = sbn.notification.extras
        val text = listOfNotNull(
            extras.getCharSequence(Notification.EXTRA_TITLE),
            extras.getCharSequence(Notification.EXTRA_TEXT),
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
        ).joinToString("\n")
        val code = CodeExtractor.extract(text) ?: return

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.channel_name), NotificationManager.IMPORTANCE_HIGH)
        )
        val copyIntent = Intent(this, CopyReceiver::class.java).putExtra(CopyReceiver.EXTRA_CODE, code)
        val pending = PendingIntent.getBroadcast(
            this, sbn.key.hashCode(), copyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle(getString(R.string.code_detected, code))
            .setContentText(getString(R.string.tap_to_copy))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .addAction(Notification.Action.Builder(null, getString(R.string.copy), pending).build())
            .build()
        manager.notify(sbn.key.hashCode(), notification)
    }

    private companion object {
        const val CHANNEL_ID = "2fa_codes"
    }
}
