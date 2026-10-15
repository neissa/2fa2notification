package com.neissa.twofa2notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class MailTimeParserTest {

    private val now: Long = Calendar.getInstance().apply {
        set(2025, Calendar.JANUARY, 15, 14, 40, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Test
    fun relativeFrenchMinutes() {
        assertEquals(180_000L, MailTimeParser.ageMs("Google il y a 3 min Votre code 123456", now))
    }

    @Test
    fun relativeEnglishMinutes() {
        assertEquals(120_000L, MailTimeParser.ageMs("Google 2 minutes ago Your code 123456", now))
    }

    @Test
    fun absolute24hTime() {
        assertEquals(300_000L, MailTimeParser.ageMs("Jean 14:35 Votre code 482913", now))
    }

    @Test
    fun absolute12hTime() {
        assertEquals(240_000L, MailTimeParser.ageMs("Bob 2:36 PM Your code 482913", now))
    }

    @Test
    fun yesterdayIsIndeterminate() {
        assertNull(MailTimeParser.ageMs("Hier Votre code 482913", now))
    }

    @Test
    fun noTimeIsIndeterminate() {
        assertNull(MailTimeParser.ageMs("Votre code 482913", now))
    }
}
