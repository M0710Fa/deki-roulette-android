import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.basekeita.roulette.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.compose.compiler.gradle.plugin)
    compileOnly(libs.ksp.gradle.plugin)
    compileOnly(libs.hilt.gradle.plugin)
    compileOnly(libs.ktlint.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("common") {
            id = libs.plugins.deki.common.get().pluginId
            implementationClass = "CommonConventionPlugin"
        }
        register("application") {
            id = libs.plugins.deki.application.get().pluginId
            implementationClass = "ApplicationConventionPlugin"
        }
        register("library") {
            id = libs.plugins.deki.library.get().pluginId
            implementationClass = "LibraryConventionPlugin"
        }
        register("hilt") {
            id = libs.plugins.deki.hilt.get().pluginId
            implementationClass = "HiltConventionPlugin"
        }
        register("feature") {
            id = libs.plugins.deki.feature.get().pluginId
            implementationClass = "FeatureConventionPlugin"
        }
    }
}
