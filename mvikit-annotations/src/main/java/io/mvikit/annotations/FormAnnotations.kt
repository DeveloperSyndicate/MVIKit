package io.mvikit.annotations

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class Required(val message: String = "Field is required")

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class Email(val message: String = "Invalid email format")

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class Password(
    val minLength: Int = 8,
    val requireUppercase: Boolean = false,
    val requireLowercase: Boolean = false,
    val requireSpecialCharacter: Boolean = false,
    val requireDigit: Boolean = false,
    val message: String = "Password does not meet validation criteria"
)

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class MinLength(val length: Int, val message: String = "")

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class MaxLength(val length: Int, val message: String = "")

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class Regex(val pattern: String, val message: String = "Format is invalid")

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class MatchField(val fieldName: String, val message: String = "Fields do not match")
