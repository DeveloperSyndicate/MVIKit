package io.mvikit.form

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class ValidatorsTest {

    @Test
    fun testRequiredValidator() {
        val validator = RequiredValidator("Error")

        assertNull(validator.validate("Valid string"))
        assertNull(validator.validate(123))
        assertNull(validator.validate(listOf(1)))

        assertEquals("Error", validator.validate(null))
        assertEquals("Error", validator.validate(""))
        assertEquals("Error", validator.validate("   "))
        assertEquals("Error", validator.validate(emptyList<Int>()))
        assertEquals("Error", validator.validate(emptyMap<String, String>()))
    }

    @Test
    fun testEmailValidator() {
        val validator = EmailValidator("Bad Email")

        assertNull(validator.validate("test@gmail.com"))
        assertNull(validator.validate("user.name+tag@sub.domain.co"))

        assertEquals("Bad Email", validator.validate("test"))
        assertEquals("Bad Email", validator.validate("test@"))
        assertEquals("Bad Email", validator.validate("test@gmail"))
        assertEquals("Bad Email", validator.validate("@gmail.com"))
        assertEquals("Bad Email", validator.validate("test@@gmail.com"))
    }

    @Test
    fun testPasswordValidator() {
        val validator = PasswordValidator(
            minLength = 8,
            requireUppercase = true,
            requireLowercase = true,
            requireDigit = true,
            requireSpecialCharacter = true,
            errorMessage = "Invalid password"
        )

        assertNull(validator.validate("P@ss1234"))
        assertNull(validator.validate("A#1bdefg"))

        assertEquals("Invalid password", validator.validate("short")) // Too short
        assertEquals("Invalid password", validator.validate("pass1234!")) // No uppercase
        assertEquals("Invalid password", validator.validate("PASS1234!")) // No lowercase
        assertEquals("Invalid password", validator.validate("Password!")) // No digit
        assertEquals("Invalid password", validator.validate("Password12")) // No special character
    }

    @Test
    fun testMinLengthValidator() {
        val validator = MinLengthValidator(5, "Too short")

        assertNull(validator.validate("12345"))
        assertNull(validator.validate("123456"))

        assertEquals("Too short", validator.validate("1234"))
    }

    @Test
    fun testMaxLengthValidator() {
        val validator = MaxLengthValidator(5, "Too long")

        assertNull(validator.validate("12345"))
        assertNull(validator.validate(""))

        assertEquals("Too long", validator.validate("123456"))
    }

    @Test
    fun testRegexValidator() {
        val validator = RegexValidator(Regex("^[0-9]+$"), "Numbers only")

        assertNull(validator.validate("1234"))

        assertEquals("Numbers only", validator.validate("123a"))
    }

    @Test
    fun testMatchValidator() {
        var targetValue = "password"
        val validator = MatchValidator({ targetValue }, "Passwords do not match")

        assertNull(validator.validate("password"))

        assertEquals("Passwords do not match", validator.validate("wrongpassword"))

        // Verify dynamic lookup
        targetValue = "newpassword"
        assertNull(validator.validate("newpassword"))
    }
}
