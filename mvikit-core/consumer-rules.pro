# MVIKit Core Proguard Rules
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature

# Retain State data class members and properties
-keepclassmembers class * implements com.developersyndicate.mvikit.core.state.State {
    <fields>;
    <methods>;
}

# Retain Intent and Effect interfaces and their implementations
-keep interface com.developersyndicate.mvikit.core.intent.Intent { *; }
-keep interface com.developersyndicate.mvikit.core.intent.Effect { *; }
-keep class * implements com.developersyndicate.mvikit.core.intent.Intent { *; }
-keep class * implements com.developersyndicate.mvikit.core.intent.Effect { *; }

# Retain RateLimit and DuplicateAction policies
-keep class com.developersyndicate.mvikit.core.action.** { *; }
-keep class com.developersyndicate.mvikit.core.ratelimit.** { *; }

# Retain SavedStateHandle persistence utilities
-keep class com.developersyndicate.mvikit.core.persistence.** { *; }

# Retain annotations
-keep class com.developersyndicate.mvikit.annotations.** { *; }
