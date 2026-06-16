package io.mvikit.form

/**
 * Definition of a field structure.
 */
class FieldDefinition<T>(
    val name: String,
    val initialValue: T,
    val validators: List<Validator<T>>
)

/**
 * Fluent builder for creating and configuring individual form fields.
 */
class FieldBuilder<T>(val name: String, val defaultValue: T) {
    /**
     * The initial value of the field. Defaults to [defaultValue].
     */
    var initialValue: T = defaultValue

    /**
     * Internal list of validators registered for this field.
     */
    val validators = mutableListOf<Validator<T>>()

    /**
     * Registers a custom validator.
     */
    fun validateWith(validator: Validator<T>) {
        validators.add(validator)
    }

    /**
     * Builds the final immutable [FieldDefinition].
     */
    fun build(): FieldDefinition<T> = FieldDefinition(name, initialValue, validators)
}

/**
 * Generic validator extensions.
 */

/**
 * Adds a required validation rule checking that the value is not blank, null, or empty.
 */
fun <T> FieldBuilder<T>.required(message: String = "Field is required") {
    @Suppress("UNCHECKED_CAST")
    validators.add(RequiredValidator(message) as Validator<T>)
}

/**
 * Adds a match validation rule checking that the field value equals the value of a [reference] provider.
 */
fun <T> FieldBuilder<T>.match(reference: () -> T, message: String = "Fields do not match") {
    validators.add(MatchValidator(reference, message))
}

/**
 * String-specific validator extensions.
 */

/**
 * Adds an email validation rule checking for RFC-compliant format.
 */
fun FieldBuilder<String>.email(message: String = "Invalid email address") {
    validators.add(EmailValidator(message))
}

/**
 * Adds a minimum length validation rule.
 */
fun FieldBuilder<String>.minLength(min: Int, message: String = "Minimum length is $min") {
    validators.add(MinLengthValidator(min, message))
}

/**
 * Adds a maximum length validation rule.
 */
fun FieldBuilder<String>.maxLength(max: Int, message: String = "Maximum length is $max") {
    validators.add(MaxLengthValidator(max, message))
}

/**
 * Adds a regex pattern validation rule.
 */
fun FieldBuilder<String>.regex(pattern: String, message: String = "Format is invalid") {
    validators.add(RegexValidator(Regex(pattern), message))
}

/**
 * Adds password strength validation rules.
 */
fun FieldBuilder<String>.password(
    minLength: Int = 8,
    requireUppercase: Boolean = false,
    requireLowercase: Boolean = false,
    requireSpecialCharacter: Boolean = false,
    requireDigit: Boolean = false,
    message: String = "Password does not meet validation criteria"
) {
    validators.add(
        PasswordValidator(
            minLength = minLength,
            requireUppercase = requireUppercase,
            requireLowercase = requireLowercase,
            requireSpecialCharacter = requireSpecialCharacter,
            requireDigit = requireDigit,
            errorMessage = message
        )
    )
}

/**
 * DSL builder class to construct a complete form model.
 */
class FormBuilder {
    private val _fields = mutableMapOf<String, FieldDefinition<*>>()

    /**
     * Map of registered field definitions.
     */
    val fields: Map<String, FieldDefinition<*>> get() = _fields

    /**
     * Registers a String field with default initial value `""`.
     */
    fun field(name: String, init: FieldBuilder<String>.() -> Unit) {
        val builder = FieldBuilder(name, "")
        builder.init()
        _fields[name] = builder.build()
    }

    /**
     * Registers a typed field with an explicit [initialValue].
     */
    fun <T> field(name: String, initialValue: T, init: FieldBuilder<T>.() -> Unit) {
        val builder = FieldBuilder(name, initialValue)
        builder.init()
        _fields[name] = builder.build()
    }
}

/**
 * Main DSL entrypoint to instantiate a [Form].
 *
 * @param validationMode The validation mode of the form. Defaults to [ValidationMode.ON_CHANGE].
 * @param builder DSL configuration lambda.
 * @return An initialized [Form] instance.
 */
fun form(
    validationMode: ValidationMode = ValidationMode.ON_CHANGE,
    builder: FormBuilder.() -> Unit
): Form {
    val formBuilder = FormBuilder()
    formBuilder.builder()
    return FormImpl(formBuilder.fields, validationMode)
}
