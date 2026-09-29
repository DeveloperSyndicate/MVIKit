# MVIKit Compose Proguard Rules
-keepattributes *Annotation*

# Retain Compose UI helpers and effect collection extensions
-keep class com.developersyndicate.mvikit.compose.** { *; }
