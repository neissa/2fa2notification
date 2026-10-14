package com.neissa.twofa2notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CodeExtractorTest {
    @Test fun frenchSms() = assertEquals("482913", CodeExtractor.extract("Votre code de vérification est 482913"))
    @Test fun grouped() = assertEquals("123456", CodeExtractor.extract("Your verification code: 123 456"))
    @Test fun alphanumeric() = assertEquals("A1B2C3", CodeExtractor.extract("Your security code is A1B2C3"))
    @Test fun noKeyword() = assertNull(CodeExtractor.extract("Rendez-vous à 1830 demain"))
    @Test fun nullText() = assertNull(CodeExtractor.extract(null))
}
