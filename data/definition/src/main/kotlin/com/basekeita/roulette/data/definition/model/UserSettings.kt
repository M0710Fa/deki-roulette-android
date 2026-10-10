package com.basekeita.roulette.data.definition.model

/** 設定画面で変える値。未設定のときは触覚・効果音とも ON、光彩は既定の色。 */
data class UserSettings(
    val hapticsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val glowStyle: GlowStyle = GlowStyle.DEFAULT,
)
