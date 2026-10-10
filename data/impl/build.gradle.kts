plugins {
    alias(libs.plugins.deki.library)
    alias(libs.plugins.deki.hilt)
}

android {
    namespace = "com.basekeita.roulette.data.impl"
}

dependencies {
    implementation(projects.data.definition)
    implementation(projects.local)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)
    // 一時ファイルの DataStore を作って DekiLocalDataSource に渡すため
    testImplementation(libs.androidx.datastore.preferences)
}
