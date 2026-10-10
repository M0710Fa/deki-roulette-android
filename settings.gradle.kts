pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "deki-roulette-android"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")
include(":feature:roulette")
include(":feature:order")
include(":feature:settings")
include(":feature:saved-lists")
include(":ui")
include(":navigation")
include(":domain")
include(":data:definition")
include(":data:impl")
include(":local")
