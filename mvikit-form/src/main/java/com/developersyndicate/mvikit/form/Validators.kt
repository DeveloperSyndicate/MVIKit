package com.developersyndicate.mvikit.form

/**
 * Validates that a value is provided (i.e. is not null, not blank for strings, and not empty for collections).
 */
class RequiredValidator(
    private val errorMessage: String = "Field is required"
) : Validator<Any?> {
    override fun validate(value: Any?): String? {
        return when (value) {
            null -> errorMessage
            is CharSequence -> if (value.trim().isEmpty()) errorMessage else null
            is Collection<*> -> if (value.isEmpty()) errorMessage else null
            is Map<*, *> -> if (value.isEmpty()) errorMessage else null
            else -> null
        }
    }
}

/**
 * Validates that a string matches an RFC-compliant email address pattern.
 */
class EmailValidator(
    private val errorMessage: String = "Invalid email address"
) : Validator<String> {
    private val emailRegex = Regex(
        "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
        "\\@" +
        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
        "(" +
        "\\." +
        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
        ")+"
    )

    override fun validate(value: String): String? {
        return if (emailRegex.matches(value)) null else errorMessage
    }
}

/**
 * Validates password rules.
 *
 * @property minLength The minimum password length.
 * @property requireUppercase True to require at least one uppercase character.
 * @property requireLowercase True to require at least one lowercase character.
 * @property requireSpecialCharacter True to require at least one symbol.
 * @property requireDigit True to require at least one digit.
 */
class PasswordValidator(
    private val minLength: Int = 0,
    private val requireUppercase: Boolean = false,
    private val requireLowercase: Boolean = false,
    private val requireSpecialCharacter: Boolean = false,
    private val requireDigit: Boolean = false,
    private val errorMessage: String = "Password does not meet validation criteria"
) : Validator<String> {
    override fun validate(value: String): String? {
        if (value.length < minLength) return errorMessage
        if (requireUppercase && !value.any { it.isUpperCase() }) return errorMessage
        if (requireLowercase && !value.any { it.isLowerCase() }) return errorMessage
        if (requireDigit && !value.any { it.isDigit() }) return errorMessage
        if (requireSpecialCharacter && !value.any { !it.isLetterOrDigit() }) return errorMessage
        return null
    }
}

/**
 * Validates that a string contains at least [min] characters.
 */
class MinLengthValidator(
    private val min: Int,
    private val errorMessage: String = "Minimum length is $min"
) : Validator<String> {
    override fun validate(value: String): String? {
        return if (value.length >= min) null else errorMessage
    }
}

/**
 * Validates that a string does not exceed [max] characters.
 */
class MaxLengthValidator(
    private val max: Int,
    private val errorMessage: String = "Maximum length is $max"
) : Validator<String> {
    override fun validate(value: String): String? {
        return if (value.length <= max) null else errorMessage
    }
}

/**
 * Validates that a string matches a given [regex] pattern.
 */
class RegexValidator(
    private val regex: Regex,
    private val errorMessage: String = "Field format is invalid"
) : Validator<String> {
    override fun validate(value: String): String? {
        return if (regex.matches(value)) null else errorMessage
    }
}

/**
 * Validates that a value matches the value retrieved by a [reference] provider lambda.
 * Useful for fields like "Confirm Password".
 */
class MatchValidator<T>(
    private val reference: () -> T,
    private val errorMessage: String = "Values do not match"
) : Validator<T> {
    override fun validate(value: T): String? {
        return if (value == reference()) null else errorMessage
    }
}

/**
 * Validates that a value matches the value of another field identified by [otherFieldName].
 */
class MatchFieldValidator<T>(
    val otherFieldName: String,
    private val errorMessage: String = "Values do not match"
) : CrossFieldValidator<T> {
    override fun validate(value: T, context: FormValidationContext): String? {
        val otherValue = context.getFieldValue<T>(otherFieldName)
        return if (value == otherValue) null else errorMessage
    }
}

/**
 * Validates that a value is provided (not null/blank) conditionally when [predicate] matches
 * the value of [otherFieldName].
 */
class RequiredIfValidator<T>(
    val otherFieldName: String,
    private val predicate: (Any?) -> Boolean,
    private val errorMessage: String = "Field is required"
) : CrossFieldValidator<T> {
    private val requiredDelegate = RequiredValidator(errorMessage)

    override fun validate(value: T, context: FormValidationContext): String? {
        val otherValue = context.getFieldValue<Any?>(otherFieldName)
        return if (predicate(otherValue)) {
            requiredDelegate.validate(value)
        } else {
            null
        }
    }
}
