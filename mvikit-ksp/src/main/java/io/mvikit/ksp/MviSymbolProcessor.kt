package io.mvikit.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.*
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.io.File

class MviSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private var processed = false
    private val generatedFiles = mutableSetOf<String>()

    private fun createNewFileSafe(
        dependencies: Dependencies,
        packageName: String,
        fileName: String
    ): java.io.OutputStream? {
        val fqName = "$packageName.$fileName"
        if (generatedFiles.contains(fqName)) return null
        generatedFiles.add(fqName)
        return try {
            codeGenerator.createNewFile(dependencies, packageName, fileName)
        } catch (e: java.io.IOException) {
            null
        }
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (processed) return emptyList()
        processed = true

        try {
            // Run diagnostics validation first
            runDiagnostics(resolver)

            // 1. Process @Form and generate validators
            processForms(resolver)

            // 2. Process @MviViewModel and generate wrappers, states, intents, etc.
            processViewModels(resolver)

            // 3. Process @MviScreen stubs
            processMviScreen(resolver)

            // 4. Process @Destination, @DeepLink, and @ComposableScreen
            processNavigation(resolver)

        } catch (e: Exception) {
            logger.error("MVIKit KSP processing failed: ${e.message}")
            e.printStackTrace()
        }

        return emptyList()
    }

    private fun runDiagnostics(resolver: Resolver) {
        // Validate @Form
        val formSymbols = resolver.getSymbolsWithAnnotation("io.mvikit.annotations.Form").toList()
        for (symbol in formSymbols) {
            if (symbol !is KSClassDeclaration || symbol.classKind != ClassKind.CLASS || !symbol.modifiers.contains(com.google.devtools.ksp.symbol.Modifier.DATA)) {
                logger.error("@Form annotation can only be applied to Kotlin data classes", symbol)
            }
        }

        // Validate @MviViewModel
        val vmSymbols = resolver.getSymbolsWithAnnotation("io.mvikit.annotations.MviViewModel").toList()
        for (symbol in vmSymbols) {
            if (symbol !is KSClassDeclaration || symbol.classKind != ClassKind.CLASS) {
                logger.error("@MviViewModel annotation can only be applied to Kotlin classes", symbol)
                continue
            }

            val handlers = symbol.getDeclaredFunctions().filter { fn ->
                fn.annotations.any { it.shortName.asString() == "IntentHandler" }
            }.toList()

            val methodNames = mutableMapOf<String, KSFunctionDeclaration>()
            for (handler in handlers) {
                val methodName = handler.simpleName.asString()
                val normalizedName = methodName.replaceFirstChar { it.uppercase() }
                if (methodNames.containsKey(normalizedName)) {
                    val duplicateHandler = methodNames[normalizedName]!!
                    logger.error(
                        "Duplicate @IntentHandler function name '$methodName' (normalized to '$normalizedName'). " +
                        "This conflicts with function '${duplicateHandler.simpleName.asString()}' as they both generate the same Intent class.",
                        handler
                    )
                } else {
                    methodNames[normalizedName] = handler
                }
            }
        }

        // Validate @IntentHandler
        val intentHandlerSymbols = resolver.getSymbolsWithAnnotation("io.mvikit.annotations.IntentHandler").toList()
        for (symbol in intentHandlerSymbols) {
            if (symbol !is KSFunctionDeclaration) continue
            val parent = symbol.parentDeclaration
            val isParentMviViewModel = parent is KSClassDeclaration && parent.annotations.any {
                val name = it.annotationType.resolve().declaration.qualifiedName?.asString()
                name == "io.mvikit.annotations.MviViewModel"
            }
            if (!isParentMviViewModel) {
                logger.error("@IntentHandler can only be declared inside @MviViewModel annotated classes", symbol)
            }
            if (symbol.modifiers.contains(Modifier.PRIVATE) || symbol.modifiers.contains(Modifier.PROTECTED)) {
                logger.error("@IntentHandler function '${symbol.simpleName.asString()}' cannot be private or protected.", symbol)
            }
            if (symbol.modifiers.contains(Modifier.SUSPEND)) {
                logger.error("@IntentHandler function '${symbol.simpleName.asString()}' cannot be suspend. Use @MviAction for asynchronous/suspending operations.", symbol)
            }
        }

        // Validate @MviAction
        val mviActionSymbols = resolver.getSymbolsWithAnnotation("io.mvikit.annotations.MviAction").toList()
        for (symbol in mviActionSymbols) {
            if (symbol !is KSFunctionDeclaration) continue
            val parent = symbol.parentDeclaration
            val isParentMviViewModel = parent is KSClassDeclaration && parent.annotations.any {
                val name = it.annotationType.resolve().declaration.qualifiedName?.asString()
                name == "io.mvikit.annotations.MviViewModel"
            }
            if (!isParentMviViewModel) {
                logger.error("@MviAction can only be declared inside @MviViewModel annotated classes", symbol)
            }
            if (symbol.modifiers.contains(Modifier.PRIVATE) || symbol.modifiers.contains(Modifier.PROTECTED)) {
                logger.error("@MviAction function '${symbol.simpleName.asString()}' cannot be private or protected.", symbol)
            }
        }

        // Validate @Persist types
        val persistSymbols = resolver.getSymbolsWithAnnotation("io.mvikit.annotations.Persist").toList()
        for (symbol in persistSymbols) {
            val type = when (symbol) {
                is KSPropertyDeclaration -> symbol.type.resolve()
                is KSValueParameter -> symbol.type.resolve()
                else -> null
            }
            if (type != null) {
                if (!isBundleSupportedType(type)) {
                    val name = when (symbol) {
                        is KSPropertyDeclaration -> symbol.simpleName.asString()
                        is KSValueParameter -> symbol.name?.asString() ?: "param"
                        else -> "element"
                    }
                    val typeName = type.declaration.qualifiedName?.asString() ?: type.toString()
                    logger.error(
                        "@Persist property '$name' has unsupported type '$typeName'. " +
                        "Persisted properties must be primitives, String, Parcelable, Serializable, or standard collections/arrays thereof.",
                        symbol
                    )
                }
            }
        }

        // Validate @DeepLink route uniqueness
        val deepLinkSymbols = resolver.getSymbolsWithAnnotation("io.mvikit.annotations.DeepLink").toList()
        val registeredRoutes = mutableMapOf<String, KSClassDeclaration>()
        for (symbol in deepLinkSymbols) {
            if (symbol !is KSClassDeclaration) continue
            val annotation = symbol.annotations.firstOrNull { it.shortName.asString() == "DeepLink" } ?: continue
            val route = annotation.arguments.firstOrNull { it.name?.asString() == "route" }?.value?.toString() ?: ""
            if (route.isNotBlank()) {
                if (registeredRoutes.containsKey(route)) {
                    val conflict = registeredRoutes[route]!!
                    logger.error(
                        "Duplicate @DeepLink route '$route' found on '${symbol.simpleName.asString()}'. " +
                        "This conflicts with route declared on '${conflict.simpleName.asString()}'.",
                        symbol
                    )
                } else {
                    registeredRoutes[route] = symbol
                }
            }
        }
    }

    private fun isBundleSupportedType(type: KSType): Boolean {
        val qName = type.declaration.qualifiedName?.asString() ?: return true
        val supportedPrimitivesAndStrings = setOf(
            "kotlin.String",
            "kotlin.CharSequence",
            "kotlin.Int",
            "kotlin.Long",
            "kotlin.Boolean",
            "kotlin.Float",
            "kotlin.Double",
            "kotlin.Byte",
            "kotlin.Short",
            "kotlin.Char",
            "kotlin.ByteArray",
            "kotlin.ShortArray",
            "kotlin.IntArray",
            "kotlin.LongArray",
            "kotlin.FloatArray",
            "kotlin.DoubleArray",
            "kotlin.BooleanArray",
            "kotlin.CharArray"
        )
        if (supportedPrimitivesAndStrings.contains(qName)) return true

        // Check common collections
        if (qName == "kotlin.collections.List" || qName == "kotlin.collections.ArrayList" || qName == "kotlin.collections.Set") {
            val typeArg = type.arguments.firstOrNull()?.type?.resolve()
            return if (typeArg != null) isBundleSupportedType(typeArg) else true
        }
        if (qName == "kotlin.collections.Map" || qName == "kotlin.collections.HashMap") {
            val valArg = type.arguments.getOrNull(1)?.type?.resolve()
            return if (valArg != null) isBundleSupportedType(valArg) else true
        }

        // Check supertypes for Parcelable or Serializable
        fun hasSupportedSupertype(decl: KSDeclaration): Boolean {
            if (decl !is KSClassDeclaration) return false
            for (superType in decl.superTypes) {
                val superDecl = superType.resolve().declaration
                val superName = superDecl.qualifiedName?.asString()
                if (superName == "android.os.Parcelable" || superName == "java.io.Serializable") {
                    return true
                }
                if (hasSupportedSupertype(superDecl)) return true
            }
            return false
        }

        return hasSupportedSupertype(type.declaration)
    }

    // --- Helper to get default values for state fields ---
    private fun getDefaultValueLiteral(typeName: TypeName): String {
        if (typeName.isNullable) return "null"
        return when (typeName.toString()) {
            "kotlin.String" -> "\"\""
            "kotlin.Boolean" -> "false"
            "kotlin.Int" -> "0"
            "kotlin.Long" -> "0L"
            "kotlin.Float" -> "0.0f"
            "kotlin.Double" -> "0.0"
            "kotlin.collections.Map<kotlin.String, kotlin.String>" -> "emptyMap()"
            "kotlin.collections.Map<kotlin.String,kotlin.String>" -> "emptyMap()"
            else -> {
                if (typeName.toString().startsWith("kotlin.collections.Map")) {
                    "emptyMap()"
                } else if (typeName.toString().startsWith("kotlin.collections.List")) {
                    "emptyList()"
                } else if (typeName.toString().startsWith("kotlin.collections.Set")) {
                    "emptySet()"
                } else {
                    "null"
                }
            }
        }
    }

    // --- 1. Form Generator ---
    private fun processForms(resolver: Resolver) {
        val formSymbols = resolver.getSymbolsWithAnnotation("io.mvikit.annotations.Form").toList()
        for (symbol in formSymbols) {
            if (symbol !is KSClassDeclaration || symbol.classKind != ClassKind.CLASS || !symbol.modifiers.contains(com.google.devtools.ksp.symbol.Modifier.DATA)) {
                continue
            }

            val packageName = symbol.packageName.asString()
            val className = symbol.simpleName.asString()
            val validatorName = "${className}Validator"

            val properties = symbol.getDeclaredProperties().toList()

            // Prepare validator class spec
            val validatorClassName = ClassName(packageName, validatorName)
            val validatorClassBuilder = TypeSpec.classBuilder(validatorClassName)
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addParameter(
                            ParameterSpec.builder("validationMode", ClassName("io.mvikit.form", "ValidationMode"))
                                .defaultValue("io.mvikit.form.ValidationMode.ON_CHANGE")
                                .build()
                        )
                        .build()
                )

            // formInstance property
            validatorClassBuilder.addProperty(
                PropertySpec.builder("formInstance", ClassName("io.mvikit.form", "Form"), KModifier.PRIVATE)
                    .mutable(true)
                    .addModifiers(KModifier.LATEINIT)
                    .build()
            )

            // Initializer block with Form DSL
            val initBlock = CodeBlock.builder()
            initBlock.addStatement("formInstance = io.mvikit.form.form(validationMode) {")

            for (prop in properties) {
                val propName = prop.simpleName.asString()
                val isString = prop.type.toTypeName().toString() == "kotlin.String"

                if (isString) {
                    initBlock.addStatement("    field(%S) {", propName)
                } else {
                    val def = getDefaultValueLiteral(prop.type.toTypeName())
                    initBlock.addStatement("    field(%S, %L) {", propName, def)
                }

                // Parse form constraints
                for (ann in prop.annotations) {
                    val name = ann.shortName.asString()
                    val args = ann.arguments.associateBy { it.name?.asString() }
                    when (name) {
                        "Required" -> {
                            val msg = args["message"]?.value ?: "Field is required"
                            initBlock.addStatement("        required(%S)", msg)
                        }
                        "Email" -> {
                            val msg = args["message"]?.value ?: "Invalid email format"
                            initBlock.addStatement("        email(%S)", msg)
                        }
                        "Password" -> {
                            val min = args["minLength"]?.value ?: 8
                            val upper = args["requireUppercase"]?.value ?: false
                            val lower = args["requireLowercase"]?.value ?: false
                            val spec = args["requireSpecialCharacter"]?.value ?: false
                            val digit = args["requireDigit"]?.value ?: false
                            val msg = args["message"]?.value ?: "Password does not meet validation criteria"
                            initBlock.addStatement("        password(minLength = %L, requireUppercase = %L, requireLowercase = %L, requireSpecialCharacter = %L, requireDigit = %L, message = %S)", min, upper, lower, spec, digit, msg)
                        }
                        "MinLength" -> {
                            val len = args["length"]?.value ?: 0
                            val msg = args["message"]?.value ?: "Minimum length is $len"
                            initBlock.addStatement("        minLength(%L, %S)", len, msg)
                        }
                        "MaxLength" -> {
                            val len = args["length"]?.value ?: 0
                            val msg = args["message"]?.value ?: "Maximum length is $len"
                            initBlock.addStatement("        maxLength(%L, %S)", len, msg)
                        }
                        "Regex" -> {
                            val pat = args["pattern"]?.value ?: ""
                            val msg = args["message"]?.value ?: "Format is invalid"
                            initBlock.addStatement("        regex(%S, %S)", pat, msg)
                        }
                        "MatchField" -> {
                            val ref = args["fieldName"]?.value ?: ""
                            val msg = args["message"]?.value ?: "Fields do not match"
                            val fallback = if (isString) "\"\"" else getDefaultValueLiteral(prop.type.toTypeName())
                            initBlock.addStatement("        match({ if (::formInstance.isInitialized) formInstance.get<%T>(%S).value else %L }, %S)", prop.type.toTypeName(), ref, fallback, msg)
                        }
                    }
                }
                initBlock.addStatement("    }")
            }
            initBlock.addStatement("}")

            validatorClassBuilder.addInitializerBlock(initBlock.build())

            // form helper property
            validatorClassBuilder.addProperty(
                PropertySpec.builder("form", ClassName("io.mvikit.form", "Form"))
                    .getter(FunSpec.getterBuilder().addStatement("return formInstance").build())
                    .build()
            )

            // validate function
            val validateMethod = FunSpec.builder("validate")
                .addParameter("data", ClassName(packageName, className))
                .returns(ClassName("io.mvikit.form", "ValidationResult"))
                .addCode(
                    CodeBlock.builder().apply {
                        for (prop in properties) {
                            val name = prop.simpleName.asString()
                            val type = prop.type.toTypeName()
                            addStatement("formInstance.get<%T>(%S).updateValue(data.%N)", type, name, name)
                        }
                        addStatement("return formInstance.validate()")
                    }.build()
                )
                .build()
            validatorClassBuilder.addFunction(validateMethod)

            // isValid and validationErrors properties
            validatorClassBuilder.addProperty(
                PropertySpec.builder("isValid", BOOLEAN)
                    .getter(FunSpec.getterBuilder().addStatement("return form.isValid").build())
                    .build()
            )

            validatorClassBuilder.addProperty(
                PropertySpec.builder("validationErrors", Map::class.asClassName().parameterizedBy(STRING, STRING))
                    .getter(
                        FunSpec.getterBuilder()
                            .addStatement("return form.fields.value.mapNotNull { entry -> entry.value.error?.let { err -> entry.key to err } }.toMap()")
                            .build()
                    )
                    .build()
            )

            // Write File
            val fileSpec = FileSpec.builder(packageName, validatorName)
                .addImport("io.mvikit.form", "required")
                .addImport("io.mvikit.form", "email")
                .addImport("io.mvikit.form", "password")
                .addImport("io.mvikit.form", "minLength")
                .addImport("io.mvikit.form", "maxLength")
                .addImport("io.mvikit.form", "regex")
                .addImport("io.mvikit.form", "match")
                .addType(validatorClassBuilder.build())
                .build()

            fileSpec.writeTo(codeGenerator, Dependencies(false, symbol.containingFile!!))
        }
    }

    // --- 2. ViewModel & State & Intent Processor ---
    private fun processViewModels(resolver: Resolver) {
        val vmSymbols = resolver.getSymbolsWithAnnotation("io.mvikit.annotations.MviViewModel").toList()
        val formSymbols = resolver.getSymbolsWithAnnotation("io.mvikit.annotations.Form").toList()

        for (symbol in vmSymbols) {
            if (symbol !is KSClassDeclaration) continue
            val packageName = symbol.packageName.asString()
            val className = symbol.simpleName.asString()
            val prefix = className.removeSuffix("ViewModel")
            val hasNoArgConstructor = symbol.primaryConstructor?.parameters?.isEmpty() ?: true

            val dependencies = Dependencies(false, symbol.containingFile!!)

            // Find matching form symbol if any
            val formSymbol = formSymbols.filterIsInstance<KSClassDeclaration>()
                .firstOrNull { it.simpleName.asString().removeSuffix("Form") == prefix }

            val functions = symbol.getDeclaredFunctions().toList()
            val handlers = functions.filter { fn ->
                fn.annotations.any { it.shortName.asString() == "IntentHandler" }
            }
            val actions = functions.filter { fn ->
                fn.annotations.any { it.shortName.asString() == "MviAction" }
            }

            // 1. Gather all state properties
            val stateProperties = mutableMapOf<String, TypeName>()

            // Add fields from form
            if (formSymbol != null) {
                for (prop in formSymbol.getDeclaredProperties()) {
                    stateProperties[prop.simpleName.asString()] = prop.type.toTypeName()
                }
            }

            // Add fields from intent handler parameters
            for (handler in handlers) {
                for (param in handler.parameters) {
                    val name = param.name!!.asString()
                    val type = param.type.toTypeName()
                    if (stateProperties.containsKey(name) && stateProperties[name] != type) {
                        logger.error("State property '$name' has conflicting types: ${stateProperties[name]} vs $type", handler)
                    }
                    stateProperties[name] = type
                }
            }

            // Add standard parameters
            stateProperties["isLoading"] = BOOLEAN
            stateProperties["validationErrors"] = Map::class.asClassName().parameterizedBy(STRING, STRING)
            stateProperties["errorMessage"] = STRING.copy(nullable = true)

            // Gather all persisted properties mapping: Property Name -> Bundle Key
            val persistedProperties = mutableMapOf<String, String>()
            if (formSymbol != null) {
                val declaredProps = formSymbol.getDeclaredProperties().toList()
                val hasAnyExplicitPersist = declaredProps.any { prop ->
                    prop.annotations.any { it.shortName.asString() == "Persist" }
                }

                for (prop in declaredProps) {
                    val name = prop.simpleName.asString()
                    val persistAnn = prop.annotations.firstOrNull { it.shortName.asString() == "Persist" }
                    if (persistAnn != null) {
                        val customKey = persistAnn.arguments.firstOrNull { it.name?.asString() == "key" }?.value?.toString()
                        val key = if (!customKey.isNullOrBlank()) customKey else name
                        persistedProperties[name] = key
                    } else if (!hasAnyExplicitPersist) {
                        // Backward compatibility: If no @Persist is explicitly declared on any field, persist all form properties
                        persistedProperties[name] = name
                    }
                }
            }
            for (handler in handlers) {
                for (param in handler.parameters) {
                    val name = param.name!!.asString()
                    val persistAnn = param.annotations.firstOrNull { it.shortName.asString() == "Persist" }
                    if (persistAnn != null) {
                        val customKey = persistAnn.arguments.firstOrNull { it.name?.asString() == "key" }?.value?.toString()
                        val key = if (!customKey.isNullOrBlank()) customKey else name
                        persistedProperties[name] = key
                    }
                }
            }

            val stateClassName = ClassName(packageName, "${prefix}State")
            val builderClassName = ClassName(packageName, "${prefix}StateBuilder")
            val intentClassName = ClassName(packageName, "${prefix}Intent")
            val effectClassName = ClassName(packageName, "${prefix}Effect")
            val wrapperClassName = ClassName(packageName, "${prefix}ViewModel_GeneratedWrapper")
            val vmClassName = ClassName(packageName, className)

            // --- 2.1 Generate State data class ---
            val stateClassBuilder = TypeSpec.classBuilder(stateClassName)
                .addModifiers(KModifier.DATA)
            val stateConstructor = FunSpec.constructorBuilder()

            for ((name, typeName) in stateProperties) {
                val defaultValue = getDefaultValueLiteral(typeName)
                stateConstructor.addParameter(
                    ParameterSpec.builder(name, typeName)
                        .defaultValue(defaultValue)
                        .build()
                )
                stateClassBuilder.addProperty(
                    PropertySpec.builder(name, typeName)
                        .initializer(name)
                        .build()
                )
            }
            stateClassBuilder.primaryConstructor(stateConstructor.build())

            FileSpec.builder(packageName, "${prefix}State")
                .addType(stateClassBuilder.build())
                .build()
                .writeTo(codeGenerator, dependencies)

            // --- 2.2 Generate StateBuilder class ---
            val builderClassBuilder = TypeSpec.classBuilder(builderClassName)
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addParameter("state", stateClassName)
                        .build()
                )

            for ((name, typeName) in stateProperties) {
                builderClassBuilder.addProperty(
                    PropertySpec.builder(name, typeName)
                        .mutable(true)
                        .initializer("state.%N", name)
                        .build()
                )
            }

            builderClassBuilder.addFunction(
                FunSpec.builder("build")
                    .returns(stateClassName)
                    .addStatement("return %T(%L)", stateClassName, stateProperties.keys.joinToString(", ") { "$it = $it" })
                    .build()
            )

            FileSpec.builder(packageName, "${prefix}StateBuilder")
                .addType(builderClassBuilder.build())
                .build()
                .writeTo(codeGenerator, dependencies)

            // --- 2.3 Generate Intent sealed interface & subclasses ---
            val intentInterfaceBuilder = TypeSpec.interfaceBuilder(intentClassName)
                .addModifiers(KModifier.SEALED)
                .addSuperinterface(ClassName("io.mvikit.core.intent", "Intent"))

            for (handler in handlers) {
                val methodName = handler.simpleName.asString()
                val subclassName = methodName.replaceFirstChar { it.uppercase() }
                val params = handler.parameters

                val subclassSpec = if (params.isEmpty()) {
                    TypeSpec.objectBuilder(subclassName)
                        .addSuperinterface(intentClassName)
                        .build()
                } else {
                    val classBuilder = TypeSpec.classBuilder(subclassName)
                        .addModifiers(KModifier.DATA)
                        .addSuperinterface(intentClassName)

                    val subclassConstructor = FunSpec.constructorBuilder()
                    for (param in params) {
                        val paramName = param.name!!.asString()
                        val paramType = param.type.toTypeName()
                        subclassConstructor.addParameter(paramName, paramType)
                        classBuilder.addProperty(
                            PropertySpec.builder(paramName, paramType)
                                .initializer(paramName)
                                .build()
                        )
                    }
                    classBuilder.primaryConstructor(subclassConstructor.build()).build()
                }
                intentInterfaceBuilder.addType(subclassSpec)
            }

            FileSpec.builder(packageName, "${prefix}Intent")
                .addType(intentInterfaceBuilder.build())
                .build()
                .writeTo(codeGenerator, dependencies)

            // --- 2.4 Generate Effect sealed interface ---
            val effectInterfaceSpec = TypeSpec.interfaceBuilder(effectClassName)
                .addModifiers(KModifier.SEALED)
                .build()

            FileSpec.builder(packageName, "${prefix}Effect")
                .addType(effectInterfaceSpec)
                .build()
                .writeTo(codeGenerator, dependencies)

            // --- 2.5 Generate ViewModel Wrapper class ---
            val savedStateHandleClassName = ClassName("androidx.lifecycle", "SavedStateHandle")
            val wrapperClassSpecBuilder = TypeSpec.classBuilder(wrapperClassName)
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addParameter("viewModel", vmClassName)
                        .addParameter(
                            ParameterSpec.builder("savedStateHandle", savedStateHandleClassName.copy(nullable = true))
                                .defaultValue("null")
                                .build()
                        )
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("viewModel", vmClassName)
                        .initializer("viewModel")
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("savedStateHandle", savedStateHandleClassName.copy(nullable = true))
                        .initializer("savedStateHandle")
                        .build()
                )

            val superclassInitCode = CodeBlock.builder()
            if (persistedProperties.isEmpty()) {
                superclassInitCode.add("%T()", stateClassName)
            } else {
                superclassInitCode.add("run {\n")
                superclassInitCode.add("    if (savedStateHandle != null) {\n")
                superclassInitCode.add("        %T(\n", stateClassName)
                for ((name, typeName) in stateProperties) {
                    val bundleKey = persistedProperties[name]
                    if (bundleKey != null) {
                        superclassInitCode.add("            %N = savedStateHandle.get<%T>(%S) ?: ", name, typeName, bundleKey)
                        val defaultValue = getDefaultValueLiteral(typeName)
                        superclassInitCode.add("%L,\n", defaultValue)
                    } else {
                        val defaultValue = getDefaultValueLiteral(typeName)
                        superclassInitCode.add("            %N = %L,\n", name, defaultValue)
                    }
                }
                superclassInitCode.add("        )\n")
                superclassInitCode.add("    } else {\n")
                superclassInitCode.add("        %T()\n", stateClassName)
                superclassInitCode.add("    }\n")
                superclassInitCode.add("}")
            }

            wrapperClassSpecBuilder.superclass(ClassName("io.mvikit.core", "ViewModel").parameterizedBy(stateClassName, ANY))
                .addSuperclassConstructorParameter(superclassInitCode.build())

            if (formSymbol != null) {
                val validatorClassName = ClassName(packageName, "${prefix}FormValidator")
                wrapperClassSpecBuilder.addProperty(
                    PropertySpec.builder("validator", validatorClassName)
                        .initializer("%T()", validatorClassName)
                        .build()
                )
            }

            val wrapperInitBlock = CodeBlock.builder()
            wrapperInitBlock.addStatement("io.mvikit.core.MviViewModelRegistry.register(viewModel, this)")

            val rateLimitPolicyClassName = ClassName("io.mvikit.core.ratelimit", "RateLimitPolicy")
            val durationMember = MemberName("kotlin.time.Duration.Companion", "milliseconds")

            for (handler in handlers) {
                val intentHandlerAnnotation = handler.annotations.firstOrNull { it.shortName.asString() == "IntentHandler" }
                val rateLimitArg = intentHandlerAnnotation?.arguments?.firstOrNull { it.name?.asString() == "rateLimit" }?.value?.toString()
                val durationMsArg = (intentHandlerAnnotation?.arguments?.firstOrNull { it.name?.asString() == "durationMs" }?.value as? Long)
                    ?: (intentHandlerAnnotation?.arguments?.firstOrNull { it.name?.asString() == "durationMs" }?.value as? Number)?.toLong()
                    ?: 0L

                val rateLimitType = rateLimitArg?.substringAfterLast('.') ?: "NONE"
                if (rateLimitType != "NONE" && durationMsArg > 0L) {
                    val methodName = handler.simpleName.asString()
                    val subclassName = methodName.replaceFirstChar { it.uppercase() }
                    val subclassFq = ClassName(packageName, "${prefix}Intent", subclassName)

                    val policyExpr = when (rateLimitType) {
                        "DEBOUNCE" -> CodeBlock.of("%T.Debounce(%L.%M)", rateLimitPolicyClassName, durationMsArg, durationMember)
                        "THROTTLE_FIRST" -> CodeBlock.of("%T.ThrottleFirst(%L.%M)", rateLimitPolicyClassName, durationMsArg, durationMember)
                        "THROTTLE_LATEST" -> CodeBlock.of("%T.ThrottleLatest(%L.%M)", rateLimitPolicyClassName, durationMsArg, durationMember)
                        else -> null
                    }

                    if (policyExpr != null) {
                        val params = handler.parameters
                        if (params.isEmpty()) {
                            wrapperInitBlock.add(
                                "registerRateLimitedIntent(%T::class, %L) { viewModel.%N() }\n",
                                subclassFq, policyExpr, methodName
                            )
                        } else {
                            val paramAccess = params.joinToString(", ") { "it.${it.name!!.asString()}" }
                            wrapperInitBlock.add(
                                "registerRateLimitedIntent(%T::class, %L) { viewModel.%N(%L) }\n",
                                subclassFq, policyExpr, methodName, paramAccess
                            )
                        }
                    }
                }
            }

            val viewModelScopeMember = MemberName("androidx.lifecycle", "viewModelScope")
            val launchMember = MemberName("kotlinx.coroutines", "launch")
            if (persistedProperties.isNotEmpty()) {
                wrapperInitBlock.add("if (savedStateHandle != null) {\n")
                wrapperInitBlock.add("    this.%M.%M {\n", viewModelScopeMember, launchMember)
                wrapperInitBlock.add("        state.collect { currentState ->\n")
                for ((propName, bundleKey) in persistedProperties) {
                    wrapperInitBlock.add("            savedStateHandle[%S] = currentState.%N\n", bundleKey, propName)
                }
                wrapperInitBlock.add("        }\n")
                wrapperInitBlock.add("    }\n")
                wrapperInitBlock.add("}\n")
            }

            wrapperClassSpecBuilder.addInitializerBlock(wrapperInitBlock.build())

            // Add override suspend fun executeHandler(intent: Intent)
            val executeHandlerFunSpecBuilder = FunSpec.builder("executeHandler")
                .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
                .addParameter("intent", ClassName("io.mvikit.core.intent", "Intent"))

            val whenCode = CodeBlock.builder()
            whenCode.add("when (intent) {\n")
            for (handler in handlers) {
                val methodName = handler.simpleName.asString()
                val subclassName = methodName.replaceFirstChar { it.uppercase() }
                val subclassFq = ClassName(packageName, "${prefix}Intent", subclassName)

                val params = handler.parameters
                if (params.isEmpty()) {
                    whenCode.add("    is %T -> viewModel.%N()\n", subclassFq, methodName)
                } else {
                    val paramAccess = params.joinToString(", ") { "intent.${it.name!!.asString()}" }
                    whenCode.add("    is %T -> viewModel.%N(%L)\n", subclassFq, methodName, paramAccess)
                }
            }
            whenCode.add("    else -> super.executeHandler(intent)\n")
            whenCode.add("}\n")

            executeHandlerFunSpecBuilder.addCode(whenCode.build())
            wrapperClassSpecBuilder.addFunction(executeHandlerFunSpecBuilder.build())

            // Add updateStateInternal
            wrapperClassSpecBuilder.addFunction(
                FunSpec.builder("updateStateInternal")
                    .addParameter("block", LambdaTypeName.get(builderClassName, emptyList(), UNIT))
                    .addCode(
                        CodeBlock.builder()
                            .addStatement("reduce {")
                            .addStatement("    val builder = %T(this)", builderClassName)
                            .addStatement("    builder.block()")
                            .addStatement("    builder.build()")
                            .addStatement("}")
                            .build()
                    )
                    .build()
            )

            FileSpec.builder(packageName, "${prefix}ViewModel_GeneratedWrapper")
                .addImport("kotlinx.coroutines.flow", "collect")
                .addType(wrapperClassSpecBuilder.build())
                .build()
                .writeTo(codeGenerator, dependencies)

            // --- 2.6 Generate Extensions File ---
            val fileSpecBuilder = FileSpec.builder(packageName, "${prefix}ViewModel_Extensions")
                .addImport("androidx.compose.runtime", "collectAsState")
                .addImport("androidx.lifecycle", "createSavedStateHandle")

            // val LoginViewModel.state: LoginState
            fileSpecBuilder.addProperty(
                PropertySpec.builder("state", stateClassName)
                    .receiver(vmClassName)
                    .getter(
                        FunSpec.getterBuilder()
                            .addStatement("return (io.mvikit.core.MviViewModelRegistry.getWrapper(this) as %T).state.value", wrapperClassName)
                            .build()
                    )
                    .build()
            )

            // fun LoginViewModel.updateState(...)
            fileSpecBuilder.addFunction(
                FunSpec.builder("updateState")
                    .receiver(vmClassName)
                    .addParameter("block", LambdaTypeName.get(builderClassName, emptyList(), UNIT))
                    .addStatement("val wrapper = io.mvikit.core.MviViewModelRegistry.getWrapper(this) as %T", wrapperClassName)
                    .addStatement("wrapper.updateStateInternal(block)")
                    .build()
            )

            // fun <T> LoginViewModel.execute(...)
            val executeMethod = FunSpec.builder("execute")
                .receiver(vmClassName)
                .addTypeVariable(TypeVariableName("T"))
                .addParameter(
                    ParameterSpec.builder("key", STRING)
                        .defaultValue("java.util.UUID.randomUUID().toString()")
                        .build()
                )
                .addParameter(
                    ParameterSpec.builder("policy", ClassName("io.mvikit.core.action", "DuplicateActionPolicy"))
                        .defaultValue("io.mvikit.core.action.DuplicateActionPolicy.ALLOW")
                        .build()
                )
                .addParameter(
                    ParameterSpec.builder("retries", INT)
                        .defaultValue("0")
                        .build()
                )
                .addParameter(
                    ParameterSpec.builder("retryDelayMillis", LONG)
                        .defaultValue("0L")
                        .build()
                )
                .addParameter(
                    ParameterSpec.builder("exponentialBackoff", BOOLEAN)
                        .defaultValue("false")
                        .build()
                )
                .addParameter(
                    ParameterSpec.builder("loadingKey", STRING.copy(nullable = true))
                        .defaultValue("null")
                        .build()
                )
                .addParameter(
                    ParameterSpec.builder("request", LambdaTypeName.get(receiver = null, returnType = TypeVariableName("T")).copy(suspending = true))
                        .build()
                )
                .returns(ClassName("io.mvikit.core.action", "ActionBuilder").parameterizedBy(TypeVariableName("T")))
                .addCode(
                    CodeBlock.builder()
                        .addStatement("val wrapper = io.mvikit.core.MviViewModelRegistry.getWrapper(this)")
                        .addStatement("return io.mvikit.core.action.ActionBuilder(wrapper, key, policy, retries, retryDelayMillis, exponentialBackoff, loadingKey, request)")
                        .build()
                )
                .build()
            fileSpecBuilder.addFunction(executeMethod)

            // Composable collectState()
            fileSpecBuilder.addFunction(
                FunSpec.builder("collectState")
                    .receiver(vmClassName)
                    .addAnnotation(ClassName("androidx.compose.runtime", "Composable"))
                    .returns(ClassName("androidx.compose.runtime", "State").parameterizedBy(stateClassName))
                    .addStatement("val wrapper = io.mvikit.core.MviViewModelRegistry.getWrapper(this) as %T", wrapperClassName)
                    .addStatement("return wrapper.state.collectAsState()")
                    .build()
            )

            // Composable collectEffects()
            fileSpecBuilder.addFunction(
                FunSpec.builder("collectEffects")
                    .receiver(vmClassName)
                    .addAnnotation(ClassName("androidx.compose.runtime", "Composable"))
                    .addParameter("handler", LambdaTypeName.get(receiver = null, ANY, returnType = UNIT).copy(suspending = true))
                    .addStatement("val wrapper = io.mvikit.core.MviViewModelRegistry.getWrapper(this)")
                    .addStatement("io.mvikit.compose.CollectEffects(wrapper.effects, handler)")
                    .build()
            )

            // Composable rememberLoginViewModel()
            fileSpecBuilder.addFunction(
                FunSpec.builder("remember${prefix}ViewModel")
                    .addAnnotation(ClassName("androidx.compose.runtime", "Composable"))
                    .addParameter(
                        ParameterSpec.builder("factory", LambdaTypeName.get(receiver = null, returnType = vmClassName)).apply {
                            if (hasNoArgConstructor) {
                                defaultValue("{ %T() }", vmClassName)
                            }
                        }.build()
                    )
                    .returns(vmClassName)
                    .addCode(
                        CodeBlock.builder()
                            .addStatement("val wrapper = androidx.lifecycle.viewmodel.compose.viewModel<%T>(", wrapperClassName)
                            .addStatement("    initializer = {")
                            .addStatement("        val savedStateHandle = this.createSavedStateHandle()")
                            .addStatement("        %T(factory(), savedStateHandle)", wrapperClassName)
                            .addStatement("    }")
                            .addStatement(")")
                            .addStatement("return wrapper.viewModel")
                            .build()
                    )
                    .build()
            )

            // If form exists, generate the validate() extension function
            if (formSymbol != null) {
                val formClassName = ClassName(packageName, formSymbol.simpleName.asString())
                val validateFuncBuilder = FunSpec.builder("validate")
                    .receiver(vmClassName)
                    .returns(BOOLEAN)

                val formArgs = formSymbol.getDeclaredProperties().map { prop ->
                    val name = prop.simpleName.asString()
                    "$name = current.$name"
                }.joinToString(", ")

                validateFuncBuilder.addCode(
                    CodeBlock.builder()
                        .addStatement("val wrapper = io.mvikit.core.MviViewModelRegistry.getWrapper(this) as %T", wrapperClassName)
                        .addStatement("val current = this.state")
                        .addStatement("val form = %T(%L)", formClassName, formArgs)
                        .addStatement("val result = wrapper.validator.validate(form)")
                        .addStatement("this.updateState {")
                        .addStatement("    validationErrors = result.errors")
                        .addStatement("}")
                        .addStatement("return result.isValid")
                        .build()
                )
                fileSpecBuilder.addFunction(validateFuncBuilder.build())
            }

            // Generate @MviAction wrappers
            for (action in actions) {
                val actionName = action.simpleName.asString()
                val wrapperName = if (actionName.startsWith("perform")) {
                    actionName.removePrefix("perform").replaceFirstChar { it.lowercase() }
                } else {
                    "${actionName}Async"
                }

                val actionWrapperBuilder = FunSpec.builder(wrapperName)
                    .receiver(vmClassName)

                val params = action.parameters
                val callArgs = params.joinToString(", ") { p -> p.name!!.asString() }

                for (p in params) {
                    actionWrapperBuilder.addParameter(p.name!!.asString(), p.type.toTypeName())
                }

                actionWrapperBuilder.addCode(
                    CodeBlock.builder()
                        .addStatement("val wrapper = io.mvikit.core.MviViewModelRegistry.getWrapper(this)")
                        .addStatement("wrapper.viewModelScope.launch {")
                        .addStatement("    this@%N.%N(%L)", wrapperName, actionName, callArgs)
                        .addStatement("}")
                        .build()
                )

                fileSpecBuilder.addImport("androidx.lifecycle", "viewModelScope")
                fileSpecBuilder.addImport("kotlinx.coroutines", "launch")
                fileSpecBuilder.addFunction(actionWrapperBuilder.build())
            }

            fileSpecBuilder.build().writeTo(codeGenerator, dependencies)
        }
    }

    // --- 3. Screen Generator ---
    private fun processMviScreen(resolver: Resolver) {
        val symbols = resolver.getSymbolsWithAnnotation("io.mvikit.annotations.MviScreen").toList()
        for (symbol in symbols) {
            if (symbol !is KSClassDeclaration) continue
            val annotation = symbol.annotations.first { it.shortName.asString() == "MviScreen" }
            val name = annotation.arguments.first { it.name?.asString() == "name" }.value.toString()
            val packageName = symbol.packageName.asString()

            // Generate Contract if not present
            val contractName = "${name}Contract"
            if (resolver.getClassDeclarationByName(resolver.getKSNameFromString("$packageName.$contractName")) == null) {
                val file = createNewFileSafe(Dependencies(false, symbol.containingFile!!), packageName, contractName)
                if (file != null) {
                    PrintWriter(OutputStreamWriter(file, "UTF-8")).use { out ->
                        out.println("package $packageName")
                        out.println()
                        out.println("interface $contractName")
                    }
                }
            }
        }
    }

    // --- 4. Navigation Generator ---
    private fun processNavigation(resolver: Resolver) {
        val destSymbols = resolver.getSymbolsWithAnnotation("io.mvikit.annotations.Destination").toList()
        val dlSymbols = resolver.getSymbolsWithAnnotation("io.mvikit.annotations.DeepLink").toList()
        val csSymbols = resolver.getSymbolsWithAnnotation("io.mvikit.annotations.ComposableScreen").toList()

        val allScreens = mutableMapOf<String, ScreenMeta>()

        fun getOrPutScreen(symbol: KSClassDeclaration): ScreenMeta {
            val fqName = symbol.qualifiedName!!.asString()
            return allScreens.getOrPut(fqName) {
                val isObject = symbol.classKind == ClassKind.OBJECT
                val params = symbol.primaryConstructor?.parameters?.map { p ->
                    ScreenParam(p.name!!.asString(), p.type.toTypeName().toString())
                } ?: emptyList()
                ScreenMeta(
                    simpleName = symbol.simpleName.asString(),
                    fqName = fqName,
                    isObject = isObject,
                    params = params,
                    symbol = symbol
                )
            }
        }

        for (sym in destSymbols) {
            if (sym is KSClassDeclaration) getOrPutScreen(sym)
        }

        for (sym in dlSymbols) {
            if (sym is KSClassDeclaration) {
                val meta = getOrPutScreen(sym)
                val annotation = sym.annotations.first { it.shortName.asString() == "DeepLink" }
                val route = annotation.arguments.first { it.name?.asString() == "route" }.value.toString()
                meta.deepLinkRoute = route
            }
        }

        for (sym in csSymbols) {
            if (sym is KSClassDeclaration) {
                val meta = getOrPutScreen(sym)
                val annotation = sym.annotations.first { it.shortName.asString() == "ComposableScreen" }
                val viewVal = annotation.arguments.firstOrNull { it.name?.asString() == "viewName" }?.value?.toString()
                meta.viewName = viewVal?.takeIf { it.isNotBlank() } ?: "${meta.simpleName}View"
            }
        }

        if (allScreens.isEmpty()) return

        // Generate Navigation Registry
        val sourceFiles = allScreens.values
            .mapNotNull { it.symbol.containingFile }
            .toSet()
            .toTypedArray()

        // Generate Module-specific NavigationModule class
        val firstScreen = allScreens.values.first()
        val screenPackage = firstScreen.fqName.substringBeforeLast('.')
        val moduleSegment = screenPackage.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        val moduleClassName = "${moduleSegment}NavigationModule"

        val file = createNewFileSafe(
            Dependencies(aggregating = true, *sourceFiles),
            "io.mvikit.generated.navigation",
            moduleClassName
        ) ?: return

        PrintWriter(OutputStreamWriter(file, "UTF-8")).use { out ->
            out.println("package io.mvikit.generated.navigation")
            out.println()
            out.println("import io.mvikit.compose.NavigationModule")
            out.println("import io.mvikit.compose.NavigationGraphBuilder")
            out.println("import io.mvikit.navigation.DeepLinkSystem")
            out.println("import io.mvikit.navigation.Navigator")
            out.println("import io.mvikit.navigation.Screen")
            out.println()
            out.println("class $moduleClassName : NavigationModule {")
            out.println("    override fun registerDeepLinks() {")
            for (meta in allScreens.values) {
                val route = meta.deepLinkRoute ?: continue
                out.println("        DeepLinkSystem.register(\"$route\") { params ->")
                if (meta.isObject) {
                    out.println("            ${meta.fqName}")
                } else {
                    out.print("            ${meta.fqName}(")
                    val paramInits = meta.params.map { p ->
                        val cast = when (p.type) {
                            "Int" -> "?.toIntOrNull()"
                            "Long" -> "?.toLongOrNull()"
                            "Boolean" -> "?.toBooleanStrictOrNull()"
                            "Float" -> "?.toFloatOrNull()"
                            "Double" -> "?.toDoubleOrNull()"
                            else -> ""
                        }
                        val assertion = if (p.type.endsWith("?")) "" else " ?: throw IllegalArgumentException(\"Missing parameter ${p.name}\")"
                        "${p.name} = params[\"${p.name}\"]$cast$assertion"
                    }
                    out.print(paramInits.joinToString(", "))
                    out.println(")")
                }
                out.println("        }")
            }
            out.println("    }")
            out.println()
            out.println("    override fun registerScreens(builder: NavigationGraphBuilder, navigator: Navigator) {")
            out.println("        builder.run {")
            for (meta in allScreens.values) {
                val viewName = meta.viewName ?: "${meta.simpleName}View"
                val fqViewName = if (viewName.contains(".")) viewName else "${meta.fqName.substringBeforeLast(".")}.$viewName"
                if (meta.isObject) {
                    out.println("            screen<${meta.fqName}> {")
                    out.println("                $fqViewName(navigator)")
                    out.println("            }")
                } else {
                    out.println("            screen<${meta.fqName}> { screen ->")
                    out.println("                $fqViewName(navigator, screen)")
                    out.println("            }")
                }
            }
            out.println("        }")
            out.println("    }")
            out.println("}")
        }

        // Generate Service Provider Configuration
        try {
            val serviceFile = codeGenerator.createNewFile(
                dependencies = Dependencies(aggregating = true, *sourceFiles),
                packageName = "",
                fileName = "META-INF/services/io.mvikit.compose.NavigationModule",
                extensionName = ""
            )
            PrintWriter(OutputStreamWriter(serviceFile, "UTF-8")).use { out ->
                out.println("io.mvikit.generated.navigation.${moduleClassName}")
            }
        } catch (e: Exception) {
            logger.warn("Could not write ServiceLoader configuration: ${e.message}")
        }
    }

    private fun getTypeNameString(typeRef: KSTypeReference): String {
        return typeRef.resolve().declaration.qualifiedName?.asString() ?: typeRef.toString()
    }

    private data class ScreenParam(val name: String, val type: String)
    private data class ScreenMeta(
        val simpleName: String,
        val fqName: String,
        val isObject: Boolean,
        val params: List<ScreenParam>,
        val symbol: KSClassDeclaration,
        var deepLinkRoute: String? = null,
        var viewName: String? = null
    )
}
