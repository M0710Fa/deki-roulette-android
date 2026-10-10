package com.basekeita.roulette.ui.feedback

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 動きを減らすか（iOS の reduceMotion）。ユーザー補助の「アニメーションを削除」
 * （開発者オプションのアニメーター再生時間スケールが 0）で判定し、設定の変化を追う。
 */
interface ReduceMotionMonitor {
    val reducedMotion: StateFlow<Boolean>
}

@Singleton
internal class ReduceMotionMonitorImpl
@Inject
constructor(
    @ApplicationContext context: Context,
) : ReduceMotionMonitor {
    private val contentResolver = context.contentResolver
    private val reduced = MutableStateFlow(readReducedMotion())
    override val reducedMotion: StateFlow<Boolean> = reduced.asStateFlow()

    init {
        // アプリが動いている間ずっと追う（Singleton なので解除しない）
        contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    reduced.value = readReducedMotion()
                }
            },
        )
    }

    private fun readReducedMotion(): Boolean = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
}
