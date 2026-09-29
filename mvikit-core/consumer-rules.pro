# MVIKit Core Proguard Rules
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature

# Retain State data class members and properties
-keepclassmembers class * implements io.mvikit.core.state.State {
    <fields>;
    <methods>;
}

# Retain Intent and Effect interfaces and their implementations
-keep interface io.mvikit.core.intent.Intent { *; }
-keep interface io.mvikit.core.intent.Effect { *; }
-keep class * implements io.mvikit.core.intent.Intent { *; }
-keep class * implements io.mvikit.core.intent.Effect { *; }

# Retain RateLimit and DuplicateAction policies
-keep class io.mvikit.core.action.** { *; }
-keep class io.mvikit.core.ratelimit.** { *; }

# Retain SavedStateHandle persistence utilities
-keep class io.mvikit.core.persistence.** { *; }

# Retain annotations
-keep class io.mvikit.annotations.** { *; }
