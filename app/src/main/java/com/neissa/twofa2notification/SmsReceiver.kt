package com.neissa.twofa2notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

/** Reçoit les SMS entrants directement (sans passer par les notifications). */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val sender = messages.first().originatingAddress ?: "inconnu"
        val body = messages.joinToString("") { it.messageBody.orEmpty() }
        val time = messages.first().timestampMillis

        DebugLogger.log(context, "SMS", "Reçu de $sender (${body.length} caractères)")
        CodeProcessor.process(context, "SMS", body, time)
    }
}
