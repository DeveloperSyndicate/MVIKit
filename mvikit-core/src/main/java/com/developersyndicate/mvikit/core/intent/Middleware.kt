package com.developersyndicate.mvikit.core.intent

/**
 * Interface defining a middleware interceptor for the Intent dispatching pipeline.
 *
 * Middlewares allow processing, logging, analytics, and interception of actions
 * before they are executed by their registered handlers.
 */
interface IntentMiddleware {
    /**
     * Intercepts a dispatched [intent], executing custom logic before and/or after
     * invoking [next] to continue the middleware pipeline.
     *
     * @param intent The dispatched intent.
     * @param next The suspending continuation block to execute the next middleware or final handler.
     */
    suspend fun intercept(
        intent: Intent,
        next: suspend () -> Unit
    )
}

/**
 * Built-in middleware for logging intent receptions, execution durations, and failures.
 *
 * @property isEnabled Whether logging is enabled. Defaults to false.
 * @property tag The log tag used. Defaults to "MVIKitLogger".
 */
class LoggingMiddleware(
    var isEnabled: Boolean = false,
    private val tag: String = "MVIKitLogger"
) : IntentMiddleware {

    override suspend fun intercept(intent: Intent, next: suspend () -> Unit) {
        if (!isEnabled) {
            next()
            return
        }

        val startTime = System.currentTimeMillis()
        MviLogger.log(tag, "Intent received: ${intent::class.java.simpleName} -> $intent", null)
        try {
            next()
            val duration = System.currentTimeMillis() - startTime
            MviLogger.log(tag, "Intent completed: ${intent::class.java.simpleName} in $duration ms", null)
        } catch (t: Throwable) {
            val duration = System.currentTimeMillis() - startTime
            MviLogger.log(tag, "Intent failed: ${intent::class.java.simpleName} in $duration ms", t)
            throw t
        }
    }
}

/**
 * Safe logger abstraction to prevent Android SDK stub exceptions inside JVM unit tests.
 */
internal object MviLogger {
    var log: (String, String, Throwable?) -> Unit = { tag, message, throwable ->
        try {
            // Dynamically load Android Log class to avoid crash on plain JVM
            val logClass = Class.forName("android.util.Log")
            if (throwable != null) {
                logClass.getMethod("e", String::class.java, String::class.java, Throwable::class.java)
                    .invoke(null, tag, message, throwable)
            } else {
                logClass.getMethod("d", String::class.java, String::class.java)
                    .invoke(null, tag, message)
            }
        } catch (e: Exception) {
            // Fallback to standard console output for local JVM unit testing
            if (throwable != null) {
                println("[$tag] ERROR: $message - ${throwable.stackTraceToString()}")
            } else {
                println("[$tag] DEBUG: $message")
            }
        }
    }
}
