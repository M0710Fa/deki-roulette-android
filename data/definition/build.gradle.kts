plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.deki.common)
}

dependencies {
    // Repository の interface が Flow を公開するため、使う側にも見せる
    api(libs.kotlinx.coroutines.core)
}
