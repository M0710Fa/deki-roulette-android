plugins {
    alias(libs.plugins.deki.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.basekeita.roulette.navigation"
}

dependencies {
    api(libs.androidx.navigation3.runtime)
    implementation(libs.kotlinx.serialization.core)
}
