package com.developersyndicate.mvikit.navigation

/**
 * Singleton coordinator to register and handle deep link URLs.
 */
object DeepLinkSystem {
    private val routes = mutableMapOf<String, (Map<String, String>) -> Screen>()

    /**
     * Registers a deep link pattern (e.g. "mvikit://profile/{userId}") associated with a screen generator.
     */
    fun register(pattern: String, factory: (Map<String, String>) -> Screen) {
        routes[pattern] = factory
    }

    /**
     * Matches the given [uri] against registered patterns and returns the generated [Screen] if matching.
     */
    fun handle(uri: String): Screen? {
        for ((pattern, factory) in routes) {
            val params = matchUri(pattern, uri)
            if (params != null) {
                return factory(params)
            }
        }
        return null
    }

    /**
     * Clears all registered deep link routes. Used primarily in test cycles.
     */
    fun clear() {
        routes.clear()
    }

    private fun matchUri(pattern: String, uri: String): Map<String, String>? {
        // Extract parameter placeholders, e.g. {userId} -> parameterNames: ["userId"]
        val parameterNames = Regex("\\{([^}]+)\\}").findAll(pattern).map { it.groupValues[1] }.toList()
        
        // Convert the pattern to a regex pattern by replacing {name} placeholders with wildcard matchers ([^/]+)
        val regexString = pattern.replace(Regex("\\{[^}]+\\}"), "([^/]+)")
        val regex = Regex("^$regexString$")
        
        val match = regex.matchEntire(uri) ?: return null
        
        val params = mutableMapOf<String, String>()
        parameterNames.forEachIndexed { index, name ->
            val value = match.groupValues.getOrNull(index + 1)
            if (value != null) {
                params[name] = value
            }
        }
        return params
    }
}
