package com.neissa.twofa2notification

import android.content.Context
import android.provider.Telephony
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Lit la boîte de réception et analyse uniquement les SMS des 10 dernières minutes. */
object SmsInboxScanner {
    fun scanRecent(context: Context): Int {
        val since = System.currentTimeMillis() - CodeProcessor.MAX_AGE_MS
        val sinceStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(since))
        DebugLogger.log(context, "SMS", "Scan de la boîte de réception depuis $sinceStr")

        val rows = mutableListOf<Triple<String, String, Long>>()
        try {
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                "${Telephony.Sms.DATE} >= ?",
                arrayOf(since.toString()),
                "${Telephony.Sms.DATE} ASC",
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    rows += Triple(
                        cursor.getString(0).orEmpty(),
                        cursor.getString(1).orEmpty(),
                        cursor.getLong(2),
                    )
                }
            }
        } catch (e: SecurityException) {
            DebugLogger.log(context, "SMS", "Permission READ_SMS manquante: ${e.message}")
            return 0
        }

        DebugLogger.log(context, "SMS", "${rows.size} SMS trouvé(s) dans les 10 dernières minutes")
        // Traités du plus ancien au plus récent : le plus récent code gagne
        rows.forEach { (sender, body, time) ->
            CodeProcessor.process(context, "SMS inbox ($sender)", body, time, sender)
        }
        return rows.size
    }
}
