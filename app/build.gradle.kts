plugins {
    alias(libs.plugins.deki.application)
    alias(libs.plugins.deki.hilt)
}

android {
    namespace = "com.basekeita.roulette"

    // 設定アプリでアプリ別に言語（日本語・英語）を選べるようにする。既定の言語は res/resources.properties
    androidResources {
        generateLocaleConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
}

dependencies {
    implementation(projects.feature.roulette)
    implementation(projects.feature.order)
    implementation(projects.feature.settings)
    implementation(projects.feature.savedLists)
    implementation(projects.ui)
    implementation(projects.navigation)
    implementation(projects.domain)
    implementation(projects.data.definition)
    implementation(projects.data.impl)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
