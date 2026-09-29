plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}


dependencies {
    implementation(project(path = ":mvikit-core", configuration = "default"))
    implementation(libs.org.jetbrains.kotlin.ksp)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
}
