package com.basekeita.roulette.ui.rule

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 盤面ラベルの省略と文字サイズ。iOS 版 `WheelLabel` と同じ。
 *
 * ラベルは半径方向に置くので、収まる長さは直径に比例する。基準より大きい盤面では増えた分を文字サイズと文字数に等分し
 * （それぞれ倍率の平方根）、最長のラベルが盤面に占める割合を基準と同じに保つ。基準より小さい盤面は文字サイズだけを縮める。
 * 直径と文字サイズの単位は dp（文字サイズも文字の大きさの設定に追従させないため dp で扱う）。
 */
object WheelLabel {
    /** 基準になる盤面の直径（dp）。この大きさのとき SPEC §5.1 の表どおりになる。 */
    const val REFERENCE_DIAMETER = 320.0

    /** ラベルを置く位置。スライスの中央の角度で、中心からスライスの半径のこの割合のところに置く。 */
    const val RADIUS_FRACTION = 0.62

    /** 基準の直径での最大表示文字数（SPEC §5.1 の表）。 */
    fun baseLimit(count: Int): Int {
        if (count > 8) return 5

        if (count > 5) return 7

        return 10
    }

    /** 基準の直径での文字サイズ（SPEC §5.1 の表）。1 件だけのときは中央に大きく出す。 */
    fun baseFontSize(count: Int): Double {
        if (count == 1) return 15.0

        if (count > 8) return 9.0

        if (count > 5) return 11.0

        return 13.0
    }

    /** 盤面の直径に応じた最大表示文字数。基準以下では表のまま、基準より大きいと平方根で増やす。 */
    fun limit(
        count: Int,
        diameter: Double,
    ): Int {
        val ratio = max(0.0, diameter) / REFERENCE_DIAMETER
        return floor(baseLimit(count) * sqrt(max(1.0, ratio))).toInt()
    }

    /** 盤面の直径に応じた文字サイズ。基準以下では直径に比例して縮め、基準より大きいと平方根で増やす。 */
    fun fontSize(
        count: Int,
        diameter: Double,
    ): Double {
        val ratio = max(0.0, diameter) / REFERENCE_DIAMETER
        return baseFontSize(count) * min(ratio, sqrt(ratio))
    }

    /**
     * スライスの中央の角度 `midAngle`（度、12 時が 0 度・時計回り）に置くラベルの回転（度、画面上の時計回りが正）。
     * 半径方向に沿わせ、左半分はそのまま回すと文字が上下逆さまになるため 180 度返す。
     */
    fun rotation(midAngle: Double): Double {
        if (midAngle > 180) return midAngle + 90

        return midAngle - 90
    }

    /** `limit` 文字を超えるラベルを切って省略記号を付ける。文字は見た目の 1 文字で数える。 */
    fun truncate(
        label: String,
        limit: Int,
    ): String {
        if (label.characterCount() <= limit) return label

        return label.takeCharacters(limit) + "…"
    }
}
