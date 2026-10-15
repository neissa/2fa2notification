package com.neissa.twofa2notification

import android.content.Context

/** Point d'entrée commun pour SMS et Gmail : extrait le code puis applique le filtre des 10 minutes. */
object CodeProcessor {
    const val MAX_AGE_MS = 10 * 60 * 1000L

    /** [timeMs] = moment de réception du message, ou null si inconnu (le code est alors ignoré). */
    fun process(context: Context, source: String, text: String?, timeMs: Long?) {
        val code = CodeExtractor.extractWithDebug(context, text, source) ?: return

        if (timeMs == null) {
            DebugLogger.log(context, source, "Code $code ignoré: heure du message introuvable")
            return
        }

        val ageSec = (System.currentTimeMillis() - timeMs) / 1000
        if (ageSec > MAX_AGE_MS / 1000) {
            DebugLogger.log(context, source, "Code $code ignoré: reçu il y a ${ageSec / 60} min ${ageSec % 60} s (limite 10 min)")
            return
        }

        DebugLogger.log(context, source, "✅ Code $code envoyé à la notification (âge ${ageSec}s)")
        CodeNotificationManager.updateNotification(context, code)
    }
}
