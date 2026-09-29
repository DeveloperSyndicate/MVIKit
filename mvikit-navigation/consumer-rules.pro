# MVIKit Navigation Proguard Rules
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature

# Retain Screens and Destinations
-keep interface io.mvikit.navigation.Screen { *; }
-keep class * implements io.mvikit.navigation.Screen { *; }

# Retain all Destination, DeepLink and ComposableScreen annotated classes
-keep @io.mvikit.annotations.Destination class * { *; }
-keep @io.mvikit.annotations.DeepLink class * { *; }
-keep @io.mvikit.annotations.ComposableScreen class * { *; }

# Retain Navigation classes, routes, and generated registries
-keep class io.mvikit.navigation.** { *; }
-keep class *NavigationRegistry* { *; }
-keep class *GeneratedScreen* { *; }
