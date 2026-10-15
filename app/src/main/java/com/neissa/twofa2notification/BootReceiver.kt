package com.neissa.twofa2notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Relance la notification permanente après un redémarrage du téléphone. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CodeNotificationService.start(context)
        }
    }
}
