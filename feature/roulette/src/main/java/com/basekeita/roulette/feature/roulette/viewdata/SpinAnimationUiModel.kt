package com.basekeita.roulette.feature.roulette.viewdata

import androidx.compose.runtime.Immutable

/**
 * 盤面の回転。Screen はスピンが始まるたびに（`spinCount` が変わるたびに）`targetRotation` へ、
 * `easing` の曲線と `durationMillis` の長さで回す。
 *
 * @property spinCount 始めたスピンの回数。同じ角度・同じ結果が続いても回し直すための識別子
 * @property targetRotation 止まる累積回転角（度、画面上の時計回りが正）。止まっている間はいまの角度
 * @property easing 回転の曲線（CSS の `cubic-bezier` と同じ意味の制御点）
 */
@Immutable
data class SpinAnimationUiModel(
    val spinCount: Int,
    val targetRotation: Float,
    val easing: SpinEasingUiModel,
    val durationMillis: Int,
)

/** 回転の曲線の制御点。端点は (0, 0) と (1, 1)。 */
@Immutable
data class SpinEasingUiModel(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
)
