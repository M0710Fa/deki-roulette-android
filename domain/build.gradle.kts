plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.deki.common)
}

dependencies {
    // UseCase の引数と戻り値に data:definition のモデルを使うため、使う側にも見せる
    api(projects.data.definition)
    implementation(libs.javax.inject)

    testImplementation(libs.kotlinx.coroutines.test)
}
