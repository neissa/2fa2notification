package com.neissa.twofa2notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/** Reçoit le clic sur le bouton « Vérifier mails » de la notification. */
class CheckMailReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CHECK_MAIL) return
        Toast.makeText(context, R.string.checking_mail, Toast.LENGTH_SHORT).show()

        if (GmailApiClient.isAuthorized(context)) {
            val pending = goAsync()
            Thread {
                try {
                    GmailApiClient.checkRecentMails(context)
                } finally {
                    pending.finish()
                }
            }.start()
        } else {
            CodeAccessibilityService.forceCheck(context)
        }
    }

    companion object {
        const val ACTION_CHECK_MAIL = "com.neissa.twofa2notification.CHECK_MAIL"
    }
}
