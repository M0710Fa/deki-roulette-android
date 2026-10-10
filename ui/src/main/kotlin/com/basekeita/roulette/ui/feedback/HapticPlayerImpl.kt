package com.basekeita.roulette.ui.feedback

import android.view.HapticFeedbackConstants
import javax.inject.Inject

/**
 * `View.performHapticFeedback` で鳴らす。端末の「タップ時のバイブレーション」の設定に従う。
 * 画面が見えていなければ鳴らさない。
 */
internal class HapticPlayerImpl
@Inject
constructor(
    private val feedbackHost: FeedbackHost,
) : HapticPlayer {
    override fun play(effect: HapticEffect) {
        val view = feedbackHost.view ?: return
        view.performHapticFeedback(effect.feedbackConstant())
    }

    private fun HapticEffect.feedbackConstant(): Int = when (this) {
        HapticEffect.SELECTION -> HapticFeedbackConstants.CLOCK_TICK
        HapticEffect.LIGHT -> HapticFeedbackConstants.KEYBOARD_TAP
        HapticEffect.MEDIUM -> HapticFeedbackConstants.VIRTUAL_KEY
        HapticEffect.SUCCESS -> HapticFeedbackConstants.CONFIRM
    }
}
