package io.mvikit.sample

import io.mvikit.core.MviViewModelRegistry
import io.mvikit.form.ValidationMode
import io.mvikit.navigation.DeepLinkSystem
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KspGeneratedCodeTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testGeneratedFormValidator() {
        val validator = LoginFormValidator(ValidationMode.MANUAL)
        
        // 1. Invalid input
        val invalidForm = LoginForm(
            email = "invalid-email",
            password = "123"
        )
        val result = validator.validate(invalidForm)
        assertFalse(result.isValid)
        assertEquals("Invalid email format", result.errors["email"])
        assertEquals("Password must be at least 6 characters", result.errors["password"])

        // 2. Valid input
        val validForm = LoginForm(
            email = "test@example.com",
            password = "securepassword"
        )
        val result2 = validator.validate(validForm)
        assertTrue(result2.isValid)
        assertTrue(result2.errors.isEmpty())
    }

    @Test
    fun testGeneratedViewModelStateAndUpdates() = runTest {
        val viewModel = LoginViewModel(AuthRepository())
        // Initialize wrapper to link ViewModel to registry
        val wrapper = LoginViewModel_GeneratedWrapper(viewModel)

        // Initial state assertions
        assertEquals("", viewModel.state.email)
        assertEquals("", viewModel.state.password)
        assertFalse(viewModel.state.isLoading)
        assertNull(viewModel.state.errorMessage)

        // 1. Test updating state directly via method calls
        viewModel.updateEmail("user@example.com")
        assertEquals("user@example.com", viewModel.state.email)

        viewModel.updatePassword("mypassword")
        assertEquals("mypassword", viewModel.state.password)

        // 2. Test KSP intent routing via wrapper dispatch
        wrapper.dispatch(LoginIntent.UpdateEmail("routed@example.com"))
        assertEquals("routed@example.com", viewModel.state.email)
    }

    @Test
    fun testGeneratedViewModelValidationAndAction() = runTest {
        val navigator = io.mvikit.navigation.NavigatorImpl()
        val viewModel = LoginViewModel(AuthRepository())
        val wrapper = LoginViewModel_GeneratedWrapper(viewModel)

        // 1. Invalid state validation
        viewModel.updateEmail("invalid")
        viewModel.updatePassword("12")
        assertFalse(viewModel.validate())
        assertNotNull(viewModel.state.validationErrors["email"])
        assertNotNull(viewModel.state.validationErrors["password"])

        // 2. Valid state validation & action execution
        viewModel.updateEmail("test@example.com")
        viewModel.updatePassword("securepassword")
        assertTrue(viewModel.validate())
        assertTrue(viewModel.state.validationErrors.isEmpty())

        // Trigger action via async wrapper
        viewModel.loginAsync()
        
        // Wait for coroutine simulation
        delay(2000)
        
        assertFalse(viewModel.state.isLoading)
        assertNull(viewModel.state.errorMessage)
    }

    @Test
    fun testGeneratedNavigationModule() {
        DeepLinkSystem.clear()

        val loader = java.util.ServiceLoader.load(
            io.mvikit.compose.NavigationModule::class.java,
            io.mvikit.compose.NavigationModule::class.java.classLoader
        )
        val modules = loader.toList()
        assertFalse(modules.isEmpty())

        val module = modules.first()
        module.registerDeepLinks()

        val matchedScreen = DeepLinkSystem.handle("mvikit://login")
        assertNotNull(matchedScreen)
        assertTrue(matchedScreen is LoginScreen)
    }

    @Test
    fun testSavedStateHandleRestorationAndUpdates() = runTest {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "email" to "saved@mvikit.io",
                "password" to "savedpassword"
            )
        )
        val viewModel = LoginViewModel(AuthRepository())
        val wrapper = LoginViewModel_GeneratedWrapper(viewModel, savedStateHandle)

        // 1. Assert initial state is successfully restored from SavedStateHandle
        assertEquals("saved@mvikit.io", viewModel.state.email)
        assertEquals("savedpassword", viewModel.state.password)
        assertFalse(viewModel.state.isLoading)

        // 2. Assert state update is immediately persisted back into the SavedStateHandle
        viewModel.updateEmail("updated@mvikit.io")
        assertEquals("updated@mvikit.io", viewModel.state.email)
        
        // Wait for state collection to propagate changes to savedStateHandle
        delay(100)
        assertEquals("updated@mvikit.io", savedStateHandle.get<String>("email"))
    }

    @Test
    fun testCompileDiagnostics() {
        class TestLogger : com.google.devtools.ksp.processing.KSPLogger {
            val errors = mutableListOf<String>()
            override fun error(message: String, symbol: com.google.devtools.ksp.symbol.KSNode?) {
                errors.add(message)
            }
            override fun warn(message: String, symbol: com.google.devtools.ksp.symbol.KSNode?) {}
            override fun info(message: String, symbol: com.google.devtools.ksp.symbol.KSNode?) {}
            override fun logging(message: String, symbol: com.google.devtools.ksp.symbol.KSNode?) {}
            override fun exception(e: Throwable) {}
        }
        val testLogger = TestLogger()

        fun createKSName(name: String): com.google.devtools.ksp.symbol.KSName {
            val handler = java.lang.reflect.InvocationHandler { _, method, _ ->
                if (method.name == "asString") name else null
            }
            return java.lang.reflect.Proxy.newProxyInstance(
                com.google.devtools.ksp.symbol.KSName::class.java.classLoader,
                arrayOf(com.google.devtools.ksp.symbol.KSName::class.java),
                handler
            ) as com.google.devtools.ksp.symbol.KSName
        }

        fun createAnnotation(
            fqName: String,
            args: Map<String, Any?> = emptyMap()
        ): com.google.devtools.ksp.symbol.KSAnnotation {
            val nameProxy = createKSName(fqName)
            val shortNameVal = fqName.substringAfterLast('.')
            val shortNameProxy = createKSName(shortNameVal)
            val declHandler = java.lang.reflect.InvocationHandler { _, method, _ ->
                if (method.name == "getQualifiedName") nameProxy else null
            }
            val declProxy = java.lang.reflect.Proxy.newProxyInstance(
                com.google.devtools.ksp.symbol.KSDeclaration::class.java.classLoader,
                arrayOf(com.google.devtools.ksp.symbol.KSDeclaration::class.java),
                declHandler
            )
            val typeHandler = java.lang.reflect.InvocationHandler { _, method, _ ->
                if (method.name == "getDeclaration") declProxy else null
            }
            val typeProxy = java.lang.reflect.Proxy.newProxyInstance(
                com.google.devtools.ksp.symbol.KSType::class.java.classLoader,
                arrayOf(com.google.devtools.ksp.symbol.KSType::class.java),
                typeHandler
            )
            val typeRefHandler = java.lang.reflect.InvocationHandler { _, method, _ ->
                if (method.name == "resolve") typeProxy else null
            }
            val typeRefProxy = java.lang.reflect.Proxy.newProxyInstance(
                com.google.devtools.ksp.symbol.KSTypeReference::class.java.classLoader,
                arrayOf(com.google.devtools.ksp.symbol.KSTypeReference::class.java),
                typeRefHandler
            )

            val ksValueArgs = args.map { (k, v) ->
                val argHandler = java.lang.reflect.InvocationHandler { _, method, _ ->
                    when (method.name) {
                        "getName" -> createKSName(k)
                        "getValue" -> v
                        else -> null
                    }
                }
                java.lang.reflect.Proxy.newProxyInstance(
                    com.google.devtools.ksp.symbol.KSValueArgument::class.java.classLoader,
                    arrayOf(com.google.devtools.ksp.symbol.KSValueArgument::class.java),
                    argHandler
                ) as com.google.devtools.ksp.symbol.KSValueArgument
            }

            val annHandler = java.lang.reflect.InvocationHandler { _, method, _ ->
                when (method.name) {
                    "getAnnotationType" -> typeRefProxy
                    "getShortName" -> shortNameProxy
                    "getArguments" -> ksValueArgs
                    else -> null
                }
            }
            return java.lang.reflect.Proxy.newProxyInstance(
                com.google.devtools.ksp.symbol.KSAnnotation::class.java.classLoader,
                arrayOf(com.google.devtools.ksp.symbol.KSAnnotation::class.java),
                annHandler
            ) as com.google.devtools.ksp.symbol.KSAnnotation
        }

        val vmDeclarations = mutableListOf<com.google.devtools.ksp.symbol.KSDeclaration>()

        fun createClassDeclaration(
            isClass: Boolean,
            isDataClass: Boolean,
            annotations: List<Any> = emptyList(),
            decls: List<com.google.devtools.ksp.symbol.KSDeclaration>? = null,
            simpleName: String = "SomeClass"
        ): com.google.devtools.ksp.symbol.KSClassDeclaration {
            val classHandler = java.lang.reflect.InvocationHandler { _, method, _ ->
                when (method.name) {
                    "getClassKind" -> if (isClass) com.google.devtools.ksp.symbol.ClassKind.CLASS else com.google.devtools.ksp.symbol.ClassKind.INTERFACE
                    "getModifiers" -> if (isDataClass) setOf(com.google.devtools.ksp.symbol.Modifier.DATA) else emptySet()
                    "getSimpleName" -> createKSName(simpleName)
                    "getAnnotations" -> annotations.map {
                        if (it is com.google.devtools.ksp.symbol.KSAnnotation) it else createAnnotation(it.toString())
                    }.asSequence()
                    "getQualifiedName" -> createKSName("io.mvikit.sample.$simpleName")
                    "getDeclarations" -> (decls ?: vmDeclarations).asSequence()
                    else -> null
                }
            }
            return java.lang.reflect.Proxy.newProxyInstance(
                com.google.devtools.ksp.symbol.KSClassDeclaration::class.java.classLoader,
                arrayOf(com.google.devtools.ksp.symbol.KSClassDeclaration::class.java),
                classHandler
            ) as com.google.devtools.ksp.symbol.KSClassDeclaration
        }

        fun createFunctionDeclaration(
            name: String,
            parent: com.google.devtools.ksp.symbol.KSDeclaration?,
            annotations: List<String> = emptyList(),
            parameters: List<com.google.devtools.ksp.symbol.KSValueParameter> = emptyList(),
            modifiers: Set<com.google.devtools.ksp.symbol.Modifier> = emptySet()
        ): com.google.devtools.ksp.symbol.KSFunctionDeclaration {
            val funcHandler = java.lang.reflect.InvocationHandler { _, method, _ ->
                when (method.name) {
                    "getParentDeclaration" -> parent
                    "getSimpleName" -> createKSName(name)
                    "getAnnotations" -> annotations.map { createAnnotation(it) }.asSequence()
                    "getParameters" -> parameters
                    "getFunctionKind" -> com.google.devtools.ksp.symbol.FunctionKind.MEMBER
                    "getModifiers" -> modifiers
                    "getTypeParameters" -> emptyList<com.google.devtools.ksp.symbol.KSTypeParameter>()
                    else -> null
                }
            }
            return java.lang.reflect.Proxy.newProxyInstance(
                com.google.devtools.ksp.symbol.KSFunctionDeclaration::class.java.classLoader,
                arrayOf(com.google.devtools.ksp.symbol.KSFunctionDeclaration::class.java),
                funcHandler
            ) as com.google.devtools.ksp.symbol.KSFunctionDeclaration
        }

        fun createValueParameter(typeName: String): com.google.devtools.ksp.symbol.KSValueParameter {
            val nameProxy = createKSName(typeName)
            val declProxy = java.lang.reflect.Proxy.newProxyInstance(
                com.google.devtools.ksp.symbol.KSDeclaration::class.java.classLoader,
                arrayOf(com.google.devtools.ksp.symbol.KSDeclaration::class.java)
            ) { _, method, _ ->
                if (method.name == "getQualifiedName") nameProxy else null
            }
            val typeProxy = java.lang.reflect.Proxy.newProxyInstance(
                com.google.devtools.ksp.symbol.KSType::class.java.classLoader,
                arrayOf(com.google.devtools.ksp.symbol.KSType::class.java)
            ) { _, method, _ ->
                if (method.name == "getDeclaration") declProxy else null
            }
            val typeRefProxy = java.lang.reflect.Proxy.newProxyInstance(
                com.google.devtools.ksp.symbol.KSTypeReference::class.java.classLoader,
                arrayOf(com.google.devtools.ksp.symbol.KSTypeReference::class.java)
            ) { _, method, _ ->
                if (method.name == "resolve") typeProxy else null
            }
            val paramHandler = java.lang.reflect.InvocationHandler { _, method, _ ->
                if (method.name == "getType") typeRefProxy else null
            }
            return java.lang.reflect.Proxy.newProxyInstance(
                com.google.devtools.ksp.symbol.KSValueParameter::class.java.classLoader,
                arrayOf(com.google.devtools.ksp.symbol.KSValueParameter::class.java),
                paramHandler
            ) as com.google.devtools.ksp.symbol.KSValueParameter
        }

        // 1. Invalid Form class (not data class)
        val invalidFormSymbol = createClassDeclaration(isClass = true, isDataClass = false)
        // 2. Invalid ViewModel class (is interface, not class)
        val invalidVmSymbol = createClassDeclaration(isClass = false, isDataClass = false)
        // 3. Invalid IntentHandler (parent is not a @MviViewModel annotated class)
        val plainParentClass = createClassDeclaration(isClass = true, isDataClass = false, annotations = emptyList())
        val invalidHandlerSymbol = createFunctionDeclaration("invalidHandler", plainParentClass)
        // 4. Invalid MviAction (parent is not @MviViewModel annotated class)
        val invalidActionSymbol = createFunctionDeclaration("invalidAction", plainParentClass)

        // 5. Duplicate @IntentHandler validation
        val vmWithDuplicateHandlers = createClassDeclaration(
            isClass = true,
            isDataClass = false,
            annotations = listOf("io.mvikit.annotations.MviViewModel")
        )
        val param1 = createValueParameter("io.mvikit.sample.UpdateEmail")
        val handler1 = createFunctionDeclaration("updateEmail", vmWithDuplicateHandlers, listOf("io.mvikit.annotations.IntentHandler"), listOf(param1))
        val param2 = createValueParameter("io.mvikit.sample.UpdateEmail")
        val handler2 = createFunctionDeclaration("UpdateEmail", vmWithDuplicateHandlers, listOf("io.mvikit.annotations.IntentHandler"), listOf(param2))

        vmDeclarations.add(handler1)
        vmDeclarations.add(handler2)

        // 6. Test private and suspend @IntentHandler
        val privateHandler = createFunctionDeclaration("privateHandler", vmWithDuplicateHandlers, listOf("io.mvikit.annotations.IntentHandler"), emptyList(), setOf(com.google.devtools.ksp.symbol.Modifier.PRIVATE))
        val suspendHandler = createFunctionDeclaration("suspendHandler", vmWithDuplicateHandlers, listOf("io.mvikit.annotations.IntentHandler"), emptyList(), setOf(com.google.devtools.ksp.symbol.Modifier.SUSPEND))

        // 7. Duplicate @DeepLink route detection
        val dlAnnotation = createAnnotation("io.mvikit.annotations.DeepLink", mapOf("route" to "mvikit://duplicate"))
        val dlScreen1 = createClassDeclaration(
            isClass = true,
            isDataClass = false,
            annotations = listOf(dlAnnotation),
            simpleName = "ScreenOne"
        )
        val dlScreen2 = createClassDeclaration(
            isClass = true,
            isDataClass = false,
            annotations = listOf(dlAnnotation),
            simpleName = "ScreenTwo"
        )

        // Mock Resolver
        val resolverHandler = java.lang.reflect.InvocationHandler { _, method, args ->
            if (method.name == "getSymbolsWithAnnotation") {
                val annotation = args?.get(0) as? String
                when (annotation) {
                    "io.mvikit.annotations.Form" -> sequenceOf(invalidFormSymbol)
                    "io.mvikit.annotations.MviViewModel" -> sequenceOf(invalidVmSymbol, vmWithDuplicateHandlers)
                    "io.mvikit.annotations.IntentHandler" -> sequenceOf(invalidHandlerSymbol, handler1, handler2, privateHandler, suspendHandler)
                    "io.mvikit.annotations.MviAction" -> sequenceOf(invalidActionSymbol)
                    "io.mvikit.annotations.DeepLink" -> sequenceOf(dlScreen1, dlScreen2)
                    "io.mvikit.annotations.Persist" -> emptySequence()
                    else -> emptySequence()
                }
            } else {
                null
            }
        }
        val testResolver = java.lang.reflect.Proxy.newProxyInstance(
            com.google.devtools.ksp.processing.Resolver::class.java.classLoader,
            arrayOf(com.google.devtools.ksp.processing.Resolver::class.java),
            resolverHandler
        ) as com.google.devtools.ksp.processing.Resolver

        // Instantiate symbol processor
        val genHandler = java.lang.reflect.InvocationHandler { _, _, _ -> null }
        val dummyCodeGenerator = java.lang.reflect.Proxy.newProxyInstance(
            com.google.devtools.ksp.processing.CodeGenerator::class.java.classLoader,
            arrayOf(com.google.devtools.ksp.processing.CodeGenerator::class.java),
            genHandler
        ) as com.google.devtools.ksp.processing.CodeGenerator

        val processor = io.mvikit.ksp.MviSymbolProcessor(dummyCodeGenerator, testLogger)

        // Invoke runDiagnostics via reflection
        val runDiagnosticsMethod = io.mvikit.ksp.MviSymbolProcessor::class.java.getDeclaredMethod(
            "runDiagnostics",
            com.google.devtools.ksp.processing.Resolver::class.java
        )
        runDiagnosticsMethod.isAccessible = true
        runDiagnosticsMethod.invoke(processor, testResolver)

        // Assert that the diagnostics error messages were correctly logged
        assertTrue(testLogger.errors.any { it.contains("@Form annotation can only be applied to Kotlin data classes") })
        assertTrue(testLogger.errors.any { it.contains("@MviViewModel annotation can only be applied to Kotlin classes") })
        assertTrue(testLogger.errors.any { it.contains("@IntentHandler can only be declared inside @MviViewModel annotated classes") })
        assertTrue(testLogger.errors.any { it.contains("@MviAction can only be declared inside @MviViewModel annotated classes") })
        assertTrue(testLogger.errors.any { it.contains("Duplicate @IntentHandler function name 'UpdateEmail' (normalized to 'UpdateEmail')") })
        assertTrue(testLogger.errors.any { it.contains("@IntentHandler function 'privateHandler' cannot be private or protected") })
        assertTrue(testLogger.errors.any { it.contains("@IntentHandler function 'suspendHandler' cannot be suspend") })
        assertTrue(testLogger.errors.any { it.contains("Duplicate @DeepLink route") })
    }
}
