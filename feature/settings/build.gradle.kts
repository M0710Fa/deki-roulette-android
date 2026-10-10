plugins {
    alias(libs.plugins.deki.feature)
}

android {
    namespace = "com.basekeita.roulette.feature.settings"
}

dependencies {
    // シートの中で詳細設定から設定に戻る操作（BackHandler）。この feature だけで使う
    implementation(libs.androidx.activity.compose)
}
