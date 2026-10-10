plugins {
    alias(libs.plugins.deki.library)
    alias(libs.plugins.deki.hilt)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.basekeita.roulette.ui"
    buildFeatures {
        compose = true
    }
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
