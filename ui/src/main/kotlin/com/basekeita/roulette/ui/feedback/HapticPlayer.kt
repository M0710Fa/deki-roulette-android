package com.basekeita.roulette.ui.feedback

/** 触覚の種類。iOS の `SensoryFeedback` の使い分けに合わせる。 */
enum class HapticEffect {
    /** スピン中に境目を越えた、順番の結果が 1 件現れた。 */
    SELECTION,

    /** 長押しで指定が切り替わった。本人の指にだけ伝わる軽い手応え。 */
    LIGHT,

    /** スピンを始めた、指で動かしている盤面が境目を越えた。 */
    MEDIUM,

    /** 結果が出た。 */
    SUCCESS,
}

/** 触覚を鳴らす。設定の「触覚フィードバック」が OFF のときに呼ばないのは呼び出し側（ViewModel）の役目。 */
interface HapticPlayer {
    fun play(effect: HapticEffect)
}
