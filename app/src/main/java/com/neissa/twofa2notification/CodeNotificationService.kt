package com.neissa.twofa2notification

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

/**
 * Service de premier plan : maintient la notification permanente du dernier code 2FA
 * et exécute les vérifications déclenchées par le bouton « Vérifier ».
 * START_STICKY : Android le relance s'il est arrêté par le système.
 */
class CodeNotificationService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = CodeNotificationManager.buildNotification(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                CodeNotificationManager.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(CodeNotificationManager.NOTIFICATION_ID, notification)
        }

        if (intent?.action == ACTION_CHECK) {
            val appContext = applicationContext
            Thread { CodeCheckRunner.run(appContext) }.start()
        }
        return START_STICKY
    }

    companion object {
        const val ACTION_CHECK = "com.neissa.twofa2notification.CHECK_MAIL"

        fun start(context: Context) {
            try {
                context.startForegroundService(Intent(context, CodeNotificationService::class.java))
            } catch (e: Exception) {
                DebugLogger.log(context, "Notification", "Service de premier plan non démarré: ${e.message}")
            }
        }

        /** Intent utilisé par le bouton « Vérifier » de la notification. */
        fun checkIntent(context: Context): Intent =
            Intent(context, CodeNotificationService::class.java).setAction(ACTION_CHECK)
    }
}
