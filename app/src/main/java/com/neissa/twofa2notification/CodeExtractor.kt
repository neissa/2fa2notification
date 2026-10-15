package com.neissa.twofa2notification

import android.content.Context

object CodeExtractor {
    private val keywords = Regex(
        "code|otp|pin|v[ée]rification|verification|mot de passe|password|passcode|2fa|s[ée]curit[ée]|security|one[- ]time|usage unique|confirmation",
        RegexOption.IGNORE_CASE,
    )

    // Motifs de dates / heures / durées / téléphones / années retirés avant la recherche de code
    private val filters: List<Pair<String, Regex>> = listOf(
        "Date" to Regex("\\b\\d{1,4}[/.-]\\d{1,2}(?:[/.-]\\d{1,4})?\\b"),
        "Heure" to Regex("\\b\\d{1,2}\\s*[:hH]\\s*\\d{2}(?:\\s*[:sS]\\s*\\d{2})?\\b"),
        "Heure courte" to Regex("\\b\\d{1,2}\\s*[hH]\\b"),
        "Durée" to Regex(
            "\\b\\d{1,3}\\s*(?:mn|min|mins|minute|minutes|sec|seconde|secondes|s|h|heure|heures|j|jour|jours|day|days|hour|hours)\\b",
            RegexOption.IGNORE_CASE,
        ),
        "Téléphone" to Regex("\\+?\\b0[1-9](?:[ .-]?\\d{2}){4}\\b"),
        "Année" to Regex("\\b(19|20)\\d{2}\\b"),
    )

    // 4-8 chiffres, éventuellement groupés "123 456" ou "123-456"
    private val digits = Regex("(?<!\\d)(\\d{3,4}[ -]\\d{3,4}|\\d{4,8})(?!\\d)")
    private val alnum = Regex("(?<![A-Za-z0-9])(?=[A-Z0-9]*\\d)(?=[A-Z0-9]*[A-Z])[A-Z0-9]{6,8}(?![A-Za-z0-9])")

    private class Candidate(val code: String, val distance: Int)
    private class Analysis(val code: String?, val log: String)

    /** Retourne le code 2FA le plus probable dans [text], ou null. */
    fun extract(text: String?): String? = analyze(text).code

    /** Idem [extract], mais écrit chaque étape de l'analyse dans le journal de débogage. */
    fun extractWithDebug(context: Context, text: String?, source: String): String? {
        val analysis = analyze(text)
        DebugLogger.log(context, source, analysis.log)
        return analysis.code
    }

    private fun analyze(text: String?): Analysis {
        if (text.isNullOrBlank()) return Analysis(null, "Texte vide, rien à analyser.")

        val log = StringBuilder()
        var cleaned: String = text
        for ((label, pattern) in filters) {
            val removed = pattern.findAll(cleaned).map { it.value }.toList()
            if (removed.isNotEmpty()) {
                log.append("• Filtre [$label] ignoré: ${removed.joinToString(", ")}\n")
                cleaned = pattern.replace(cleaned, " ")
            }
        }

        val keywordMatches = keywords.findAll(cleaned).toList()
        if (keywordMatches.isEmpty()) {
            log.append("• Aucun mot-clé 2FA. Texte: \"${snippet(text)}\"")
            return Analysis(null, log.toString())
        }
        log.append("• Mots-clés: ${keywordMatches.map { it.value.lowercase() }.distinct().joinToString(", ")}\n")

        val digitCandidates = digits.findAll(cleaned).map { m ->
            Candidate(m.value.replace(Regex("[ -]"), ""), distanceTo(m.range.first, keywordMatches))
        }.toList()

        val alnumCandidates = if (digitCandidates.isEmpty()) {
            alnum.findAll(cleaned).map { m ->
                Candidate(m.value, distanceTo(m.range.first, keywordMatches))
            }.toList()
        } else {
            emptyList<Candidate>()
        }

        val kind = if (digitCandidates.isNotEmpty()) "numériques" else "alphanumériques"
        val candidates = digitCandidates.ifEmpty { alnumCandidates }
        val best = candidates.minByOrNull { it.distance }
            ?: return Analysis(null, log.append("• Aucun code valide après filtrage.").toString())

        log.append("• Candidats $kind: ${candidates.joinToString { "${it.code} (dist=${it.distance})" }}\n")
        log.append("==> Code retenu: ${best.code}")
        return Analysis(best.code, log.toString())
    }

    /** Distance entre la position du code et le mot-clé le plus proche (pénalise les codes avant le mot-clé). */
    private fun distanceTo(offset: Int, kws: List<MatchResult>): Int =
        kws.minOf { kw ->
            when {
                offset >= kw.range.last -> offset - kw.range.last
                offset < kw.range.first -> (kw.range.first - offset) * 2
                else -> 0
            }
        }

    private fun snippet(text: String): String = text.replace("\n", " ").take(100)
}
