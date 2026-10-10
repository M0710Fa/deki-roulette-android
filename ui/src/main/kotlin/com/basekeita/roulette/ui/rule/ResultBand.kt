package com.basekeita.roulette.ui.rule

import kotlin.math.max

/**
 * 結果が出ている間、盤面の中心より下に重ねる帯の寸法。iOS 版 `ResultBand` と同じ。
 * 盤面と同じく文字の大きさの設定には追従させず、直径に比例させる。単位は dp。
 *
 * 帯の中心は盤面の中心から半径 × `CENTER_OFFSET_RATIO` だけ下に置く。中心のハブ（直径 `BASE_HUB_DIAMETER`）とは重ねず、
 * 帯（カプセル）は盤面のスライスの円（半径は外枠から `BASE_SLICE_INSET` 内側）に収める。
 */
object ResultBand {
    /** 基準の直径での文字サイズ。盤面のラベル（最大 15）より大きくして結果を読ませる。 */
    const val BASE_FONT_SIZE = 28.0

    /** 収まらないときに縮める下限の倍率。それでも収まらなければ末尾を省略する。 */
    const val MINIMUM_SCALE_FACTOR = 0.5

    /** 帯の幅の上限（直径に対する割合）。 */
    const val MAX_WIDTH_RATIO = 0.72

    /** 帯の中心を盤面の中心から下げる距離（半径に対する割合）。 */
    const val CENTER_OFFSET_RATIO = 0.4

    /** 文字の左右の余白（基準の直径での dp）。 */
    const val BASE_HORIZONTAL_PADDING = 14.0

    /** 文字の上下の余白（基準の直径での dp）。 */
    const val BASE_VERTICAL_PADDING = 5.0

    /** 1 行の高さの見積もり（文字サイズに対する割合）。ハブ・円との重なりの確認に使う。 */
    const val LINE_HEIGHT_RATIO = 1.2

    /** 中心のハブの直径（基準の直径での dp）。盤面のハブと同じ。 */
    const val BASE_HUB_DIAMETER = 38.0

    /** 盤面の外枠からスライスの円までの距離（基準の直径での dp）。盤面と同じ。 */
    const val BASE_SLICE_INSET = 16.0

    private fun ratio(diameter: Double): Double = max(0.0, diameter) / WheelLabel.REFERENCE_DIAMETER

    /** 文字サイズ。 */
    fun fontSize(diameter: Double): Double = BASE_FONT_SIZE * ratio(diameter)

    /** 帯の幅の上限。 */
    fun maxWidth(diameter: Double): Double = max(0.0, diameter) * MAX_WIDTH_RATIO

    /** 文字の左右の余白。 */
    fun horizontalPadding(diameter: Double): Double = BASE_HORIZONTAL_PADDING * ratio(diameter)

    /** 文字の上下の余白。 */
    fun verticalPadding(diameter: Double): Double = BASE_VERTICAL_PADDING * ratio(diameter)

    /** 帯の高さの見積もり。1 行の文字の高さ + 上下の余白。 */
    fun height(diameter: Double): Double = fontSize(diameter) * LINE_HEIGHT_RATIO + verticalPadding(diameter) * 2

    /** 盤面の中心から帯の中心までの距離（下向き）。 */
    fun centerOffset(diameter: Double): Double = max(0.0, diameter) / 2 * CENTER_OFFSET_RATIO

    /** 中心のハブの半径。 */
    fun hubRadius(diameter: Double): Double = BASE_HUB_DIAMETER / 2 * ratio(diameter)

    /** スライスの円の半径。 */
    fun sliceRadius(diameter: Double): Double = max(0.0, max(0.0, diameter) / 2 - BASE_SLICE_INSET * ratio(diameter))
}
