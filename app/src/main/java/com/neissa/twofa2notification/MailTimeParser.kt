package com.neissa.twofa2notification

import java.util.Calendar

/**
 * Déduit l'âge d'un e-mail à partir de l'heure affichée à l'écran par Gmail.
 * Gmail n'affiche que l'heure (« 14:32 », « 2:32 PM ») ou une durée relative (« il y a 3 min »)
 * pour les mails du jour ; les mails plus anciens n'ont pas d'heure exploitable.
 */
object MailTimeParser {
    private val relativeFr = Regex("il y a\\s+(\\d+)\\s*(min|minute|minutes|h|heure|heures|sec|seconde|secondes)\\b", RegexOption.IGNORE_CASE)
    private val relativeEn = Regex("(\\d+)\\s*(min|minute|minutes|hour|hours|h|sec|second|seconds)\\s+ago\\b", RegexOption.IGNORE_CASE)
    private val justNow = Regex("à l'instant|just now", RegexOption.IGNORE_CASE)
    private val yesterday = Regex("\\bhier\\b|\\byesterday\\b", RegexOption.IGNORE_CASE)
    private val time12 = Regex("(?<!\\d)(1[0-2]|0?[1-9]):([0-5]\\d)\\s*([AaPp])\\.?[Mm]\\b")
    private val time24 = Regex("(?<![\\d:])([01]?\\d|2[0-3]):([0-5]\\d)(?![\\d:])")

    /**
     * Retourne l'âge en millisecondes du premier indice temporel trouvé dans [text],
     * ou null si l'heure n'est pas déterminable.
     */
    fun ageMs(text: String, nowMs: Long): Long? {
        // Pair(position dans le texte, âge en ms ou null si indéterminé)
        val hits = mutableListOf<Pair<Int, Long?>>()

        relativeFr.find(text)?.let { m ->
            hits += m.range.first to m.groupValues[1].toLong() * unitMs(m.groupValues[2])
        }
        relativeEn.find(text)?.let { m ->
            hits += m.range.first to m.groupValues[1].toLong() * unitMs(m.groupValues[2])
        }
        justNow.find(text)?.let { hits += it.range.first to 0L }
        yesterday.find(text)?.let { hits += it.range.first to null }
        time12.find(text)?.let { m ->
            val hour12 = m.groupValues[1].toInt() % 12
            val pm = m.groupValues[3].equals("p", ignoreCase = true)
            val hour = if (pm) hour12 + 12 else hour12
            hits += m.range.first to absoluteAge(hour, m.groupValues[2].toInt(), nowMs)
        }
        time24.find(text)?.let { m ->
            hits += m.range.first to absoluteAge(m.groupValues[1].toInt(), m.groupValues[2].toInt(), nowMs)
        }

        // Le premier élément affiché dans le texte correspond à l'en-tête du mail
        return hits.minByOrNull { it.first }?.second
    }

    private fun unitMs(unit: String): Long = when (unit.lowercase()) {
        "h", "heure", "heures", "hour", "hours" -> 60L * 60_000L
        "sec", "seconde", "secondes", "second", "seconds" -> 1_000L
        else -> 60_000L
    }

    /** Âge d'une heure du jour ; si l'heure est dans le futur, elle date de la veille. */
    private fun absoluteAge(hour: Int, minute: Int, nowMs: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis > nowMs + 60_000L) cal.add(Calendar.DAY_OF_YEAR, -1)
        return nowMs - cal.timeInMillis
    }
}
