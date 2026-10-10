package com.basekeita.roulette.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/** SDK とバイトコードのバージョン。全モジュールでここだけを見る。 */
object DekiBuildConfig {
    const val COMPILE_SDK = 37
    const val MIN_SDK = 30
    const val TARGET_SDK = 37
    val javaVersion: JavaVersion = JavaVersion.VERSION_17
}

val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun VersionCatalog.library(alias: String) = findLibrary(alias).get()

fun VersionCatalog.pluginId(alias: String): String = findPlugin(alias).get().get().pluginId

/** application と library に共通する Android の設定。 */
internal fun configureAndroid(commonExtension: CommonExtension) {
    commonExtension.apply {
        compileSdk = DekiBuildConfig.COMPILE_SDK
        defaultConfig.minSdk = DekiBuildConfig.MIN_SDK
        defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        compileOptions.sourceCompatibility = DekiBuildConfig.javaVersion
        compileOptions.targetCompatibility = DekiBuildConfig.javaVersion
    }
}
