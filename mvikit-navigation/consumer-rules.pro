# MVIKit Navigation Proguard Rules
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature

# Retain Screens and Destinations
-keep interface com.developersyndicate.mvikit.navigation.Screen { *; }
-keep class * implements com.developersyndicate.mvikit.navigation.Screen { *; }

# Retain all Destination, DeepLink and ComposableScreen annotated classes
-keep @com.developersyndicate.mvikit.annotations.Destination class * { *; }
-keep @com.developersyndicate.mvikit.annotations.DeepLink class * { *; }
-keep @com.developersyndicate.mvikit.annotations.ComposableScreen class * { *; }

# Retain Navigation classes, routes, and generated registries
-keep class com.developersyndicate.mvikit.navigation.** { *; }
-keep class *NavigationRegistry* { *; }
-keep class *GeneratedScreen* { *; }
