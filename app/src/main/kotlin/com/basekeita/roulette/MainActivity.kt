package com.basekeita.roulette

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.basekeita.roulette.ui.feedback.FeedbackHost
import com.basekeita.roulette.ui.theme.DekiLayout
import com.basekeita.roulette.ui.theme.DekiTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /** 触覚・共有・画面録画の検知に、この画面を渡す。 */
    @Inject
    lateinit var feedbackHost: FeedbackHost

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        lockPortraitOnSmallScreens()
        feedbackHost.attach(this)
        enableEdgeToEdge()
        setContent {
            DekiTheme {
                DekiApp()
            }
        }
    }

    /**
     * 横並びにできない小さな画面（スマートフォン）だけ縦向きに固定する（iOS 版の iPhone と同じ）。
     * タブレットなどは向きを固定しない。分割画面から全画面に戻ったときに固定を解くため、毎回どちらかを指定する。
     */
    private fun lockPortraitOnSmallScreens() {
        val smallScreen = resources.configuration.smallestScreenWidthDp < DekiLayout.expandedMinWidth.value
        requestedOrientation = if (smallScreen) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}
