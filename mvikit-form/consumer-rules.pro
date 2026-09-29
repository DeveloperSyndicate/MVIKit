# MVIKit Form Proguard Rules
-keepattributes *Annotation*, RuntimeVisibleAnnotations

# Retain all Form model fields and validation annotations for reflection or code-gen safety
-keepclassmembers class * {
    @com.developersyndicate.mvikit.annotations.Form <fields>;
    @com.developersyndicate.mvikit.annotations.Form <methods>;
    @com.developersyndicate.mvikit.annotations.Required <fields>;
    @com.developersyndicate.mvikit.annotations.Email <fields>;
    @com.developersyndicate.mvikit.annotations.Password <fields>;
    @com.developersyndicate.mvikit.annotations.Min <fields>;
    @com.developersyndicate.mvikit.annotations.Max <fields>;
    @com.developersyndicate.mvikit.annotations.Pattern <fields>;
    @com.developersyndicate.mvikit.annotations.DependentValidation <fields>;
}

# Retain generated form validators
-keep class *FormValidator {
    public <init>(...);
    public <methods>;
}

-keep class com.developersyndicate.mvikit.form.** { *; }
