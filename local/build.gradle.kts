plugins {
    alias(libs.plugins.deki.library)
    alias(libs.plugins.deki.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.basekeita.roulette.local"
}

dependencies {
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlinx.coroutines.test)
}
