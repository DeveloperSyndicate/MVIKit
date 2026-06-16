plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(path = ":mvikit-core", configuration = "default"))
    implementation(libs.org.jetbrains.kotlin.ksp)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
}
