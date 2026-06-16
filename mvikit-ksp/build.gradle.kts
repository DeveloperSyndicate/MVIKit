plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":mvikit-annotations"))
    implementation(libs.org.jetbrains.kotlin.ksp)
}
