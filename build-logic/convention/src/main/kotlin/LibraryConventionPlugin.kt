import com.android.build.api.dsl.LibraryExtension
import com.basekeita.roulette.buildlogic.configureAndroid
import com.basekeita.roulette.buildlogic.libs
import com.basekeita.roulette.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Android ライブラリのモジュールの設定。common を内部で適用する。各モジュールは namespace だけを書く。 */
class LibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("android-library"))
            pluginManager.apply(libs.pluginId("deki-common"))

            extensions.configure<LibraryExtension> {
                configureAndroid(this)
            }
        }
    }
}
