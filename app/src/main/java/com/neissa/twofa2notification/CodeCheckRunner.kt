package com.neissa.twofa2notification

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Vérification à la demande (SMS puis mails récents) : jusqu'à [MAX_ATTEMPTS] tentatives
 * espacées de [RETRY_DELAY_MS], arrêt dès qu'un nouveau code est trouvé.
 * Appelée depuis le service de premier plan, jamais depuis le thread principal.
 */
object CodeCheckRunner {
    const val MAX_ATTEMPTS = 5
    const val RETRY_DELAY_MS = 5_000L

    private val running = AtomicBoolean(false)

    /** Bloquant : à appeler hors thread principal. Ignore l'appel si une vérification est déjà en cours. */
    fun run(context: Context) {
        if (!running.compareAndSet(false, true)) return
        try {
            val before = CodeNotificationManager.lastCodeTime(context)
            var found = false
            for (attempt in 1..MAX_ATTEMPTS) {
                CodeNotificationManager.updateNotification(
                    context,
                    null,
                    context.getString(R.string.checking_attempt, attempt, MAX_ATTEMPTS),
                )

                val smsCount = SmsInboxScanner.scanRecent(context)
                DebugLogger.log(context, "Vérification", "Tentative $attempt/$MAX_ATTEMPTS: $smsCount SMS analysé(s)")
                found = isNewCode(context, before)
                if (found) break

                if (GmailApiClient.isAuthorized(context)) {
                    GmailApiClient.checkRecentMails(context)
                    found = isNewCode(context, before)
                    if (found) break
                } else {
                    DebugLogger.log(context, "Gmail API", "Non autorisé: mails ignorés")
                }

                if (attempt < MAX_ATTEMPTS) Thread.sleep(RETRY_DELAY_MS)
            }

            val message = if (found) {
                context.getString(R.string.check_found)
            } else {
                context.getString(R.string.check_not_found, MAX_ATTEMPTS)
            }
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        } finally {
            // Rétablit la notification normale (code ou attente)
            CodeNotificationManager.updateNotification(context, null)
            running.set(false)
        }
    }

    /** Vrai si un code plus récent que celui d'avant la vérification a été enregistré. */
    private fun isNewCode(context: Context, before: Long): Boolean =
        CodeNotificationManager.lastCodeTime(context) != before
}
