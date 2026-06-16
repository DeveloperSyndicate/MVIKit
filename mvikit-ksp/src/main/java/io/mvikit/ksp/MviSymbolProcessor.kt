package io.mvikit.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
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
            // 1. Process @GenerateState
            processGenerateState(resolver)

            // 2. Process @MviScreen
            processMviScreen(resolver)

            // 3. Process Form annotations and generate validators
            processForms(resolver)

            // 4. Process @MviViewModel and @IntentHandler / @Reducer
            processViewModels(resolver)

            // 5. Process @Destination, @DeepLink, and @ComposableScreen
            processNavigation(resolver)

        } catch (e: Exception) {
            logger.error("MVIKit KSP processing failed: ${e.message}")
            e.printStackTrace()
        }

        return emptyList()
    }

    private fun getTypeName(typeRef: KSTypeReference): String {
        val type = typeRef.resolve()
        val baseName = type.declaration.qualifiedName?.asString() ?: typeRef.toString()
        val cleanName = when (baseName) {
            "kotlin.String" -> "String"
            "kotlin.Boolean" -> "Boolean"
            "kotlin.Int" -> "Int"
            "kotlin.Long" -> "Long"
            "kotlin.Float" -> "Float"
            "kotlin.Double" -> "Double"
            "kotlin.collections.List" -> {
                val arg = type.arguments.firstOrNull()?.type?.let { getTypeName(it) } ?: "Any"
                "List<$arg>"
            }
            "kotlin.collections.Set" -> {
                val arg = type.arguments.firstOrNull()?.type?.let { getTypeName(it) } ?: "Any"
                "Set<$arg>"
            }
            "kotlin.collections.Map" -> {
                val arg0 = type.arguments.getOrNull(0)?.type?.let { getTypeName(it) } ?: "Any"
                val arg1 = type.arguments.getOrNull(1)?.type?.let { getTypeName(it) } ?: "Any"
                "Map<$arg0, $arg1>"
            }
            else -> baseName
        }
        return if (type.isMarkedNullable) "$cleanName?" else cleanName
    }

    private fun getDefaultValue(typeRef: KSTypeReference): String {
        val type = typeRef.resolve()
        if (type.isMarkedNullable) return "null"
        val baseName = type.declaration.qualifiedName?.asString() ?: typeRef.toString()
        return when (baseName) {
            "kotlin.String" -> "\"\""
            "kotlin.Boolean" -> "false"
            "kotlin.Int" -> "0"
            "kotlin.Long" -> "0L"
            "kotlin.Float" -> "0.0f"
            "kotlin.Double" -> "0.0"
            "kotlin.collections.List" -> "emptyList()"
            "kotlin.collections.Set" -> "emptySet()"
            "kotlin.collections.Map" -> "emptyMap()"
            else -> "null"
        }
    }

    // --- 1. State Generator (@GenerateState) ---
    private fun processGenerateState(resolver: Resolver) {
        val symbols = resolver.getSymbolsWithAnnotation("io.mvikit.annotations.GenerateState").toList()
        for (symbol in symbols) {
            if (symbol !is KSClassDeclaration || symbol.classKind != ClassKind.INTERFACE) {
                logger.error("@GenerateState can only be applied to interfaces", symbol)
                continue
            }

            val packageName = symbol.packageName.asString()
            val interfaceName = symbol.simpleName.asString()
            val stateName = if (interfaceName.endsWith("Contract")) {
                interfaceName.replace("Contract", "State")
            } else {
                "${interfaceName}State"
            }

            val file = createNewFileSafe(
                dependencies = Dependencies(false, symbol.containingFile!!),
                packageName = packageName,
                fileName = stateName
            ) ?: continue

            PrintWriter(OutputStreamWriter(file, "UTF-8")).use { out ->
                out.println("package $packageName")
                out.println()
                out.println("data class $stateName(")
                val properties = symbol.getDeclaredProperties()
                val propLines = properties.map { prop ->
                    val type = getTypeName(prop.type)
                    val default = getDefaultValue(prop.type)
                    "    override val ${prop.simpleName.asString()}: $type = $default"
                }
                out.println(propLines.joinToString(",\n"))
                out.println(") : $interfaceName")
            }
        }
    }

    // --- 2. Screen Generator (@MviScreen) ---
    private fun processMviScreen(resolver: Resolver) {
        val symbols = resolver.getSymbolsWithAnnotation("io.mvikit.annotations.MviScreen").toList()
        for (symbol in symbols) {
            if (symbol !is KSClassDeclaration) continue
            val annotation = symbol.annotations.first { it.shortName.asString() == "MviScreen" }
            val name = annotation.arguments.first { it.name?.asString() == "name" }.value.toString()
            val packageName = symbol.packageName.asString()

            // 1. Generate State if not present
            val stateName = "${name}State"
            if (resolver.getClassDeclarationByName(resolver.getKSNameFromString("$packageName.$stateName")) == null) {
                val file = createNewFileSafe(Dependencies(false, symbol.containingFile!!), packageName, stateName)
                if (file != null) {
                    PrintWriter(OutputStreamWriter(file, "UTF-8")).use { out ->
                        out.println("package $packageName")
                        out.println()
                        out.println("data class $stateName(val unused: Boolean = false)")
                    }
                }
            }

            // 2. Generate Intent if not present
            val intentName = "${name}Intent"
            if (resolver.getClassDeclarationByName(resolver.getKSNameFromString("$packageName.$intentName")) == null) {
                val file = createNewFileSafe(Dependencies(false, symbol.containingFile!!), packageName, intentName)
                if (file != null) {
                    PrintWriter(OutputStreamWriter(file, "UTF-8")).use { out ->
                        out.println("package $packageName")
                        out.println()
                        out.println("sealed interface $intentName : io.mvikit.core.intent.Intent")
                    }
                }
            }

            // 3. Generate Effect if not present
            val effectName = "${name}Effect"
            if (resolver.getClassDeclarationByName(resolver.getKSNameFromString("$packageName.$effectName")) == null) {
                val file = createNewFileSafe(Dependencies(false, symbol.containingFile!!), packageName, effectName)
                if (file != null) {
                    PrintWriter(OutputStreamWriter(file, "UTF-8")).use { out ->
                        out.println("package $packageName")
                        out.println()
                        out.println("sealed interface $effectName")
                    }
                }
            }

            // 4. Generate Contract if not present
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

    // --- 3. Form Generator ---
    private fun processForms(resolver: Resolver) {
        val formAnnotations = listOf(
            "io.mvikit.annotations.Required",
            "io.mvikit.annotations.Email",
            "io.mvikit.annotations.Password",
            "io.mvikit.annotations.MinLength",
            "io.mvikit.annotations.MaxLength",
            "io.mvikit.annotations.Regex",
            "io.mvikit.annotations.MatchField"
        )

        // Find all properties having any of these annotations
        val annotatedClasses = mutableSetOf<KSClassDeclaration>()
        for (ann in formAnnotations) {
            val symbols = resolver.getSymbolsWithAnnotation(ann).toList()
            for (symbol in symbols) {
                if (symbol is KSPropertyDeclaration) {
                    val parent = symbol.parentDeclaration
                    if (parent is KSClassDeclaration) {
                        annotatedClasses.add(parent)
                    }
                }
            }
        }

        // Generate schemas
        val schemas = mutableListOf<String>()

        for (clazz in annotatedClasses) {
            val packageName = clazz.packageName.asString()
            val className = clazz.simpleName.asString()
            val validatorName = "${className}Validator"

            val properties = clazz.getDeclaredProperties().toList()

            // Validate @MatchField reference matching
            for (prop in properties) {
                val matchFieldAnn = prop.annotations.firstOrNull { it.shortName.asString() == "MatchField" }
                if (matchFieldAnn != null) {
                    val targetField = matchFieldAnn.arguments.first { it.name?.asString() == "fieldName" }.value.toString()
                    val targetProp = properties.firstOrNull { it.simpleName.asString() == targetField }
                    if (targetProp == null) {
                        logger.error("MatchField references unknown field '$targetField'", prop)
                    }
                }
            }

            val file = createNewFileSafe(Dependencies(false, clazz.containingFile!!), packageName, validatorName) ?: continue
            PrintWriter(OutputStreamWriter(file, "UTF-8")).use { out ->
                out.println("package $packageName")
                out.println()
                out.println("import io.mvikit.form.form")
                out.println("import io.mvikit.form.Form")
                out.println("import io.mvikit.form.ValidationMode")
                out.println("import io.mvikit.form.ValidationResult")
                out.println("import io.mvikit.form.BoundField")
                out.println("import io.mvikit.form.required")
                out.println("import io.mvikit.form.match")
                out.println("import io.mvikit.form.email")
                out.println("import io.mvikit.form.minLength")
                out.println("import io.mvikit.form.maxLength")
                out.println("import io.mvikit.form.regex")
                out.println("import io.mvikit.form.password")
                out.println()
                out.println("class $validatorName(")
                out.println("    validationMode: io.mvikit.form.ValidationMode = io.mvikit.form.ValidationMode.ON_CHANGE")
                out.println(") {")
                out.println("    private lateinit var formInstance: io.mvikit.form.Form")
                out.println()
                out.println("    init {")
                out.println("        formInstance = io.mvikit.form.form(validationMode) {")

                for (prop in properties) {
                    val propName = prop.simpleName.asString()
                    val propType = getTypeName(prop.type)
                    val isString = propType == "String"

                    if (isString) {
                        out.println("            field(\"$propName\") {")
                    } else {
                        val defVal = getDefaultValue(prop.type)
                        out.println("            field(\"$propName\", $defVal) {")
                    }

                    // Write validation rules
                    for (ann in prop.annotations.toList()) {
                        val name = ann.shortName.asString()
                        val args = ann.arguments.associateBy { it.name?.asString() }
                        when (name) {
                            "Required" -> {
                                val msg = args["message"]?.value ?: "Field is required"
                                out.println("                required(\"$msg\")")
                            }
                            "Email" -> {
                                val msg = args["message"]?.value ?: "Invalid email format"
                                out.println("                email(\"$msg\")")
                            }
                            "Password" -> {
                                val min = args["minLength"]?.value ?: 8
                                val upper = args["requireUppercase"]?.value ?: false
                                val lower = args["requireLowercase"]?.value ?: false
                                val spec = args["requireSpecialCharacter"]?.value ?: false
                                val digit = args["requireDigit"]?.value ?: false
                                val msg = args["message"]?.value ?: "Password does not meet validation criteria"
                                out.println("                password(minLength = $min, requireUppercase = $upper, requireLowercase = $lower, requireSpecialCharacter = $spec, requireDigit = $digit, message = \"$msg\")")
                            }
                            "MinLength" -> {
                                val len = args["length"]?.value ?: 0
                                val msg = args["message"]?.value ?: "Minimum length is $len"
                                out.println("                minLength($len, \"$msg\")")
                            }
                            "MaxLength" -> {
                                val len = args["length"]?.value ?: 0
                                val msg = args["message"]?.value ?: "Maximum length is $len"
                                out.println("                maxLength($len, \"$msg\")")
                            }
                            "Regex" -> {
                                val pat = args["pattern"]?.value ?: ""
                                val msg = args["message"]?.value ?: "Format is invalid"
                                out.println("                regex(\"$pat\", \"$msg\")")
                            }
                            "MatchField" -> {
                                val ref = args["fieldName"]?.value ?: ""
                                val msg = args["message"]?.value ?: "Fields do not match"
                                val fallback = if (isString) "\"\"" else getDefaultValue(prop.type)
                                out.println("                match({ if (::formInstance.isInitialized) formInstance.get<$propType>(\"$ref\").value else $fallback }, \"$msg\")")
                            }
                        }
                    }
                    out.println("            }")
                }
                out.println("        }")
                out.println("    }")
                out.println()
                out.println("    val form: io.mvikit.form.Form get() = formInstance")
                out.println()
                out.println("    fun validate(data: $packageName.$className): io.mvikit.form.ValidationResult {")
                for (prop in properties) {
                    val name = prop.simpleName.asString()
                    val type = getTypeName(prop.type)
                    out.println("        formInstance.get<$type>(\"$name\").updateValue(data.$name)")
                }
                out.println("        return formInstance.validate()")
                out.println("    }")
                out.println()
                out.println("    val isValid: Boolean get() = form.isValid")
                out.println("    val validationErrors: Map<String, String> get() = form.fields.value.mapNotNull { entry -> entry.value.error?.let { err -> entry.key to err } }.toMap()")
                out.println("}")
            }

            // Schema documentation line
            val fieldsSchema = properties.joinToString(", ") { prop ->
                "${prop.simpleName.asString()}: ${getTypeName(prop.type)}"
            }
            schemas.add("- **$className**: `$fieldsSchema`")
        }

        if (schemas.isNotEmpty()) {
            writeDocFile("form_schema.md", "# Form Schema Documentation\n\n" + schemas.joinToString("\n"))
        }
    }

    // --- 4. Intent & Reducer Generator (@MviViewModel) ---
    private fun processViewModels(resolver: Resolver) {
        val vmSymbols = resolver.getSymbolsWithAnnotation("io.mvikit.annotations.MviViewModel").toList()
        val features = mutableListOf<String>()

        for (symbol in vmSymbols) {
            if (symbol !is KSClassDeclaration) continue
            val packageName = symbol.packageName.asString()
            val className = symbol.simpleName.asString()

            // Find intent handlers and reducers
            val functions = symbol.getDeclaredFunctions().toList()
            val handlers = functions.filter { fn ->
                fn.annotations.any { it.shortName.asString() == "IntentHandler" || it.shortName.asString() == "Reducer" }
            }

            val actions = functions.filter { fn ->
                fn.annotations.any { it.shortName.asString() == "MviAction" }
            }

            val generatedName = "${className}_Generated"
            val file = createNewFileSafe(Dependencies(false, symbol.containingFile!!), packageName, generatedName) ?: continue

            val uniqueIntents = mutableSetOf<String>()

            PrintWriter(OutputStreamWriter(file, "UTF-8")).use { out ->
                out.println("package $packageName")
                out.println()
                out.println("import io.mvikit.core.intent.Intent")
                out.println()

                // Write registerGeneratedIntents extension
                out.println("fun $packageName.$className.registerGeneratedIntents() {")

                for (fn in handlers) {
                    if (fn.parameters.size != 1) {
                        logger.error("IntentHandler/Reducer function must have exactly one parameter", fn)
                        continue
                    }
                    val param = fn.parameters[0]
                    val intentType = getTypeName(param.type)

                    if (uniqueIntents.contains(intentType)) {
                        logger.error("Duplicate intent registration for $intentType", fn)
                    }
                    uniqueIntents.add(intentType)

                    val isReducer = fn.annotations.any { it.shortName.asString() == "Reducer" }
                    val fnName = fn.simpleName.asString()

                    out.println("    registerIntent($intentType::class) { intent ->")
                    if (isReducer) {
                        // reducer: checks if the function takes state receiver or ViewModel returns state
                        val returnsState = fn.returnType?.resolve()?.declaration?.simpleName?.asString() ?: "Unit"
                        if (returnsState == "Unit") {
                            logger.error("Reducer function must return a state object", fn)
                        }
                        out.println("        reduce {")
                        out.println("            this@registerGeneratedIntents.$fnName(intent)")
                        out.println("        }")
                    } else {
                        // handler: normal method call
                        out.println("        this@registerGeneratedIntents.$fnName(intent)")
                    }
                    out.println("    }")
                }
                out.println("}")
                out.println()

                // Generate @MviAction execute wrappers
                for (action in actions) {
                    val annotation = action.annotations.first { it.shortName.asString() == "MviAction" }
                    val args = annotation.arguments.associateBy { it.name?.asString() }

                    val actionFnName = action.simpleName.asString()
                    val keyVal = args["key"]?.value?.toString()?.takeIf { it.isNotBlank() } ?: actionFnName
                    val policyVal = args["policy"]?.value?.toString() ?: "ALLOW"
                    val retriesVal = args["retries"]?.value ?: 0
                    val delayVal = args["retryDelayMillis"]?.value ?: 0L
                    val backoffVal = args["exponentialBackoff"]?.value ?: false
                    val loadingKeyVal = args["loadingKey"]?.value?.toString()?.takeIf { it.isNotBlank() }

                    val policyEnum = "io.mvikit.core.action.DuplicateActionPolicy.$policyVal"

                    // Extract action input params and return type
                    val inputs = action.parameters.joinToString(", ") { p ->
                        "${p.name!!.asString()}: ${getTypeName(p.type)}"
                    }
                    val callArgs = action.parameters.joinToString(", ") { p -> p.name!!.asString() }

                    val returnType = action.returnType?.let { getTypeName(it) } ?: "Unit"

                    val wrapperName = if (actionFnName.startsWith("perform")) {
                        actionFnName.removePrefix("perform").replaceFirstChar { it.lowercase() }
                    } else {
                        "${actionFnName}Async"
                    }

                    val wrapperInputs = if (inputs.isEmpty()) "" else "$inputs, "

                    out.println("fun $packageName.$className.$wrapperName(")
                    out.println("    ${wrapperInputs}onSuccess: suspend ($returnType) -> Unit,")
                    out.println("    onFailure: (suspend (Throwable) -> Unit)? = null,")
                    out.println("    onFinally: (suspend () -> Unit)? = null")
                    out.println(") {")
                    out.println("    execute(")
                    out.println("        key = \"$keyVal\",")
                    out.println("        request = { this@$wrapperName.$actionFnName($callArgs) },")
                    out.println("        onSuccess = onSuccess,")
                    out.println("        onFailure = onFailure,")
                    out.println("        onFinally = onFinally,")
                    out.println("        policy = $policyEnum,")
                    out.println("        retries = $retriesVal,")
                    out.println("        retryDelayMillis = ${delayVal}L,")
                    out.println("        exponentialBackoff = $backoffVal,")
                    if (loadingKeyVal != null) {
                        out.println("        loadingKey = \"$loadingKeyVal\"")
                    } else {
                        out.println("        loadingKey = null")
                    }
                    out.println("    )")
                    out.println("}")
                    out.println()
                }
            }

            features.add("- **$className**: registers intents `${uniqueIntents.joinToString()}`")
        }

        if (features.isNotEmpty()) {
            writeDocFile("feature_graph.md", "# Feature Graph Documentation\n\n" + features.joinToString("\n"))
        }
    }

    // --- 5. Navigation Generator (@Destination, @DeepLink, @ComposableScreen) ---
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
                    ScreenParam(p.name!!.asString(), getTypeName(p.type))
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

        // Validate duplicates and deep link placeholders
        val checkedDestinations = mutableSetOf<String>()

        for (meta in allScreens.values) {
            if (checkedDestinations.contains(meta.simpleName)) {
                logger.error("Duplicate Destination found for simple name: ${meta.simpleName}", meta.symbol)
            }
            checkedDestinations.add(meta.simpleName)

            val route = meta.deepLinkRoute
            if (route != null) {
                val pathParams = Regex("\\{([^}]+)\\}").findAll(route).map { it.groupValues[1] }.toList()
                for (param in pathParams) {
                    val hasProp = meta.params.any { it.name == param }
                    if (!hasProp) {
                        logger.error("DeepLink path parameter '{$param}' does not match any property in the screen ${meta.simpleName}", meta.symbol)
                    }
                }
            }
        }

        // Generate Navigation Registry
        val file = createNewFileSafe(Dependencies(false), "io.mvikit.generated.navigation", "GeneratedNavigationRegistry") ?: return
        PrintWriter(OutputStreamWriter(file, "UTF-8")).use { out ->
            out.println("package io.mvikit.generated.navigation")
            out.println()
            out.println("import io.mvikit.navigation.DeepLinkSystem")
            out.println("import io.mvikit.navigation.Navigator")
            out.println("import io.mvikit.navigation.Screen")
            out.println("import io.mvikit.compose.NavigationGraphBuilder")
            out.println()
            out.println("fun registerAllGeneratedDeepLinks() {")
            for (meta in allScreens.values) {
                val route = meta.deepLinkRoute ?: continue
                out.println("    DeepLinkSystem.register(\"$route\") { params ->")
                if (meta.isObject) {
                    out.println("        ${meta.fqName}")
                } else {
                    out.print("        ${meta.fqName}(")
                    val paramInits = meta.params.map { p ->
                        val cast = when (p.type) {
                            "Int" -> "?.toIntOrNull()"
                            "Long" -> "?.toLongOrNull()"
                            "Boolean" -> "?.toBooleanStrictOrNull()"
                            "Float" -> "?.toFloatOrNull()"
                            "Double" -> "?.toDoubleOrNull()"
                            else -> "" // Already String
                        }
                        val assertion = if (p.type.endsWith("?")) "" else " ?: throw IllegalArgumentException(\"Missing parameter ${p.name}\")"
                        "${p.name} = params[\"${p.name}\"]$cast$assertion"
                    }
                    out.print(paramInits.joinToString(", "))
                    out.println(")")
                }
                out.println("    }")
            }
            out.println("}")
            out.println()

            out.println("fun NavigationGraphBuilder.registerAllGeneratedScreens(navigator: io.mvikit.navigation.Navigator) {")
            for (meta in allScreens.values) {
                val viewName = meta.viewName ?: "${meta.simpleName}View"
                val fqViewName = if (viewName.contains(".")) viewName else "${meta.fqName.substringBeforeLast(".")}.$viewName"
                if (meta.isObject) {
                    out.println("    screen<${meta.fqName}> {")
                    out.println("        $fqViewName(navigator)")
                    out.println("    }")
                } else {
                    out.println("    screen<${meta.fqName}> { screen ->")
                    out.println("        $fqViewName(navigator, screen)")
                    out.println("    }")
                }
            }
            out.println("}")
        }

        // Generate Navigation Graph doc
        val graphLines = mutableListOf<String>()
        for (meta in allScreens.values) {
            val dl = meta.deepLinkRoute?.let { " [DeepLink: `$it`]" } ?: ""
            val view = meta.viewName?.let { " -> Rendered by: `$it`" } ?: ""
            graphLines.add("- **${meta.simpleName}** (${meta.fqName})$dl$view")
        }
        writeDocFile("navigation_graph.md", "# Navigation Graph Documentation\n\n" + graphLines.joinToString("\n"))
    }

    private fun writeDocFile(fileName: String, content: String) {
        try {
            val docDir = File("build/mvikit")
            if (!docDir.exists()) {
                docDir.mkdirs()
            }
            File(docDir, fileName).writeText(content)
        } catch (e: Exception) {
            // Ignore doc writing failures during compilation if directories are locked
        }
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
