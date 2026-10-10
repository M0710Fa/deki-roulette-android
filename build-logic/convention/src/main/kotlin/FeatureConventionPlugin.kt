import com.android.build.api.dsl.LibraryExtension
import com.basekeita.roulette.buildlogic.libs
import com.basekeita.roulette.buildlogic.library
import com.basekeita.roulette.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * 画面（feature 配下のモジュール）の設定。library + hilt + Compose と、どの feature も持つ依存をまとめる。
 * feature は他の feature と data:impl / local に依存しない。
 */
class FeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("deki-library"))
            pluginManager.apply(libs.pluginId("deki-hilt"))
            pluginManager.apply(libs.pluginId("kotlin-compose"))

            extensions.configure<LibraryExtension> {
                buildFeatures.compose = true
            }

            dependencies {
                add("implementation", project(":ui"))
                add("implementation", project(":navigation"))
                add("implementation", project(":domain"))
                add("implementation", project(":data:definition"))

                add("implementation", platform(libs.library("androidx-compose-bom")))
                add("implementation", libs.library("androidx-compose-material3"))
                add("implementation", libs.library("androidx-compose-ui"))
                add("implementation", libs.library("androidx-compose-ui-tooling-preview"))
                add("debugImplementation", libs.library("androidx-compose-ui-tooling"))
                add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.library("androidx-hilt-lifecycle-viewmodel-compose"))
                add("implementation", libs.library("androidx-navigation3-runtime"))

                add("testImplementation", libs.library("kotlinx-coroutines-test"))
                add("testImplementation", libs.library("turbine"))
            }
        }
    }
}
