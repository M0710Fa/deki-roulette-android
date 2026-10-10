package com.basekeita.roulette.data.definition.model

/** 止まったスライスに付ける外側への光彩の色。設定の「ルーレットの詳細設定」で選ぶ。 */
enum class GlowStyle {
    /** 白系。縁取りと同じ色。 */
    WHITE,

    /** 止まったスライスの塗りと同じ色。 */
    SLICE,

    /** スライスの 10 色を色相順に並べた虹色。 */
    RAINBOW,
    ;

    companion object {
        /** 未設定のときの値。 */
        val DEFAULT = WHITE
    }
}
