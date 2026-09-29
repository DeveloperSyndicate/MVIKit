plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.dokka) apply false
}

allprojects {
    group = "com.developersyndicate.mvikit"
    version = "0.0.1-alpha"
}

subprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    val publishableModules = setOf(
        "mvikit-core",
        "mvikit-compose",
        "mvikit-form",
        "mvikit-navigation",
        "mvikit-ksp",
        "mvikit-test"
    )

    if (name in publishableModules) {
        apply(plugin = "com.vanniktech.maven.publish")

        configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
            publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
            
            val isReleaseOrCi = hasProperty("signing.keyId") || System.getenv("SIGNING_SECRET_KEY") != null
            if (isReleaseOrCi) {
                signAllPublications()
            }

            coordinates(group.toString(), project.name, version.toString())

            pom {
                name.set("MVIKit ${project.name.replace("mvikit-", "").replaceFirstChar { it.uppercase() }}")
                description.set("Production-ready, lightweight Model-View-Intent (MVI) framework for Kotlin & Jetpack Compose - ${project.name}")
                url.set("https://github.com/developersyndicate/mvikit")
                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("sanjays")
                        name.set("Sanjay S")
                        email.set("dev.sanjayofficial@outlook.com")
                        organization.set("Developer Syndicate")
                        organizationUrl.set("https://developersyndicate.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/developersyndicate/mvikit.git")
                    developerConnection.set("scm:git:ssh://github.com/developersyndicate/mvikit.git")
                    url.set("https://github.com/developersyndicate/mvikit")
                }
            }
        }
    }
}