package com.basekeita.roulette.feature.roulette.viewdata

/** 止まったスライスに付ける光彩の色。 */
enum class GlowStyleUiModel {
    /** 白系。縁取りと同じ色。 */
    WHITE,

    /** 止まったスライスの塗りと同じ色。 */
    SLICE,

    /** スライスの 10 色を色相順に並べた虹色。 */
    RAINBOW,
}
