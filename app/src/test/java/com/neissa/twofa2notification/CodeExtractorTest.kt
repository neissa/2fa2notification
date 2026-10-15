package com.neissa.twofa2notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CodeExtractorTest {
    @Test
    fun frenchSms() = assertEquals("482913", CodeExtractor.extract("Votre code de vérification est 482913"))

    @Test
    fun frenchSmsWithDateAndTime() = assertEquals(
        "482913",
        CodeExtractor.extract("Votre code de vérification reçu le 12/03/2025 à 14h30 est 482913"),
    )

    @Test
    fun smsWithHourPrefix() = assertEquals(
        "892104",
        CodeExtractor.extract("18h30: Votre code de confirmation est 892104"),
    )

    @Test
    fun smsWithYearAndDuration() = assertEquals(
        "749201",
        CodeExtractor.extract("Code de sécurité (2025): 749201. Valable 10 minutes."),
    )

    @Test
    fun englishSmsWithDateTime() = assertEquals(
        "582910",
        CodeExtractor.extract("Your verification code is 582910. Sent at 09:45:00 on 2025-01-15"),
    )

    @Test
    fun grouped() = assertEquals("123456", CodeExtractor.extract("Your verification code: 123 456"))

    @Test
    fun alphanumeric() = assertEquals("A1B2C3", CodeExtractor.extract("Your security code is A1B2C3"))

    @Test
    fun dateOnlyNoCode() = assertNull(CodeExtractor.extract("Rendez-vous de confirmation le 12/03/2025 à 18h30"))

    @Test
    fun noKeyword() = assertNull(CodeExtractor.extract("Rendez-vous à 1830 demain"))

    @Test
    fun nullText() = assertNull(CodeExtractor.extract(null))
}
