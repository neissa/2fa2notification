package com.neissa.twofa2notification

object CodeExtractor {
    private val keywords = Regex(
        "code|otp|pin|v[ée]rification|verification|mot de passe|password|passcode|2fa|s[ée]curit[ée]|security|one[- ]time|usage unique|confirmation",
        RegexOption.IGNORE_CASE
    )
    // 4-8 digits, optionally grouped like "123 456" or "123-456"
    private val digits = Regex("(?<![\\d])(\\d{3,4}[ -]\\d{3,4}|\\d{4,8})(?![\\d])")
    private val alnum = Regex("(?<![A-Za-z0-9])(?=[A-Z0-9]*\\d)(?=[A-Z0-9]*[A-Z])[A-Z0-9]{6,8}(?![A-Za-z0-9])")

    /** Returns the first probable 2FA code found in [text], or null. */
    fun extract(text: String?): String? {
        if (text.isNullOrBlank() || !keywords.containsMatchIn(text)) return null
        digits.find(text)?.let { return it.value.replace(Regex("[ -]"), "") }
        return alnum.find(text)?.value
    }
}
