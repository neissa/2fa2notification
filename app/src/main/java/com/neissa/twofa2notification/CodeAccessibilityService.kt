package com.neissa.twofa2notification

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/** Lit l'écran de Gmail (e-mail ouvert ou liste) et déduit l'heure du mail depuis l'en-tête affiché. */
class CodeAccessibilityService : AccessibilityService() {

    private var lastScreenHash = 0

    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg != GMAIL_PACKAGE) return
        checkScreen(force = false)
    }

    /** Relit l'écran actif ; si [force] est vrai, le contenu est analysé même s'il n'a pas changé. */
    private fun checkScreen(force: Boolean) {
        val root = rootInActiveWindow
        if (root == null) {
            if (force) DebugLogger.log(this, "Gmail", "Vérification: aucune fenêtre active lisible")
            return
        }

        val pkg = root.packageName?.toString()
        if (pkg != GMAIL_PACKAGE) {
            if (force) DebugLogger.log(this, "Gmail", "Vérification: Gmail n'est pas au premier plan (actuel: $pkg)")
            return
        }

        val builder = StringBuilder()
        collectText(root, builder)

        val text = builder.toString()
        val hash = text.hashCode()
        if (!force && hash == lastScreenHash) return
        lastScreenHash = hash

        val now = System.currentTimeMillis()
        val ageMs = MailTimeParser.ageMs(text, now)
        DebugLogger.log(
            this,
            "Gmail",
            "Écran lu${if (force) " (vérification forcée)" else ""} (${text.length} caractères), âge du mail: ${ageMs?.let { "${it / 1000}s" } ?: "inconnu"}",
        )
        CodeProcessor.process(this, "Gmail", text, ageMs?.let { now - it })
    }

    private fun collectText(node: AccessibilityNodeInfo?, builder: StringBuilder) {
        if (node == null) return
        node.text?.let {
            if (it.isNotBlank()) builder.append(it).append("\n")
        }
        for (i in 0 until node.childCount) {
            collectText(node.getChild(i), builder)
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        private const val GMAIL_PACKAGE = "com.google.android.gm"

        @Volatile
        private var instance: CodeAccessibilityService? = null

        /** Déclenché par le bouton « Vérifier mails » de la notification. */
        fun forceCheck(context: Context) {
            val service = instance
            if (service == null) {
                DebugLogger.log(context, "Gmail", "Vérification impossible: service d'accessibilité inactif (à activer dans les réglages)")
                return
            }
            service.checkScreen(force = true)
        }
    }
}
