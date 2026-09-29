# MVIKit Form Proguard Rules
-keepattributes *Annotation*, RuntimeVisibleAnnotations

# Retain all Form model fields and validation annotations for reflection or code-gen safety
-keepclassmembers class * {
    @io.mvikit.annotations.Form <fields>;
    @io.mvikit.annotations.Form <methods>;
    @io.mvikit.annotations.Required <fields>;
    @io.mvikit.annotations.Email <fields>;
    @io.mvikit.annotations.Password <fields>;
    @io.mvikit.annotations.Min <fields>;
    @io.mvikit.annotations.Max <fields>;
    @io.mvikit.annotations.Pattern <fields>;
    @io.mvikit.annotations.DependentValidation <fields>;
}

# Retain generated form validators
-keep class *FormValidator {
    public <init>(...);
    public <methods>;
}

-keep class io.mvikit.form.** { *; }
