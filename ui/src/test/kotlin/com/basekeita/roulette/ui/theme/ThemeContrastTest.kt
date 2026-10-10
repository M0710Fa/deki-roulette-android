package com.basekeita.roulette.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * 配色がライトとダークの両方で読めることを確かめる。
 * 比率は WCAG 2.1 の相対輝度とコントラスト比の定義に従う。本文相当は 4.5:1、
 * アイコンなど大きめの要素は 3:1 を下限にする。
 */
class ThemeContrastTest {
    private val schemes = mapOf("ライト" to lightDekiColorScheme, "ダーク" to darkDekiColorScheme)

    // ── 文字 ────────────────────────────────────────────────────────────────

    @Test
    fun `本文と補助テキストは地に対して4_5対1以上`() {
        schemes.forEach { (name, scheme) ->
            for (background in listOf(scheme.ink900, scheme.ink800)) {
                assertAtLeast(4.5, ratio(scheme.ivory, background), "$name ivory")
                assertAtLeast(4.5, ratio(scheme.muted, background), "$name muted")
            }
        }
    }

    @Test
    fun `結果と警告の色は地に対して4_5対1以上`() {
        schemes.forEach { (name, scheme) ->
            for (background in listOf(scheme.ink900, scheme.ink800)) {
                assertAtLeast(4.5, ratio(scheme.gold, background), "$name gold")
                assertAtLeast(4.5, ratio(scheme.flareText, background), "$name flareText")
                scheme.sliceAccents.forEachIndexed { index, accent ->
                    assertAtLeast(4.5, ratio(accent, background), "$name sliceAccents[$index]")
                }
            }
        }
    }

    @Test
    fun `削除ボタンは地に対して3対1以上`() {
        schemes.forEach { (name, scheme) ->
            assertAtLeast(3.0, ratio(scheme.ink400, scheme.ink800), "$name ink400")
        }
    }

    // ── 塗りの上の文字 ──────────────────────────────────────────────────────

    @Test
    fun `スライスのラベルは塗りに対して4_5対1以上`() {
        DekiFixedColors.sliceColors.forEachIndexed { index, slice ->
            assertAtLeast(4.5, ratio(DekiFixedColors.onSlice, slice), "sliceColors[$index]")
        }
    }

    @Test
    fun `開始ボタンの文字は塗りに対して4_5対1以上`() {
        assertAtLeast(4.5, ratio(DekiFixedColors.onFlare, DekiFixedColors.flare), "onFlare")
    }

    @Test
    fun `スワイプの削除ボタンの文字は塗りに対して4_5対1以上`() {
        assertAtLeast(4.5, ratio(DekiFixedColors.onDestructive, DekiFixedColors.destructive), "onDestructive")
    }

    /** 沈めたスライスのラベルは読ませる対象ではないが、どの項目かは見分けられるようにする。 */
    @Test
    fun `沈めたスライスでもラベルは2_5対1以上`() {
        val ink = luminance(DekiFixedColors.onSlice)
        DekiFixedColors.sliceColors.forEachIndexed { index, slice ->
            val dimmed = luminance(composite(DekiFixedColors.sliceDim, slice))
            assertAtLeast(2.5, (dimmed + 0.05) / (ink + 0.05), "sliceColors[$index]")
        }
    }

    // ── 停止の強調 ──────────────────────────────────────────────────────────

    @Test
    fun `止まったスライスの縁取りは沈めたスライスに対して3対1以上`() {
        val outline = luminance(DekiFixedColors.stopOutline)
        DekiFixedColors.sliceColors.forEachIndexed { index, slice ->
            val dimmed = luminance(composite(DekiFixedColors.sliceDim, slice))
            assertAtLeast(3.0, (outline + 0.05) / (dimmed + 0.05), "sliceColors[$index]")
        }
    }

    // ── 結果の帯 ────────────────────────────────────────────────────────────

    @Test
    fun `結果の帯の文字は地に対して4_5対1以上`() {
        assertAtLeast(4.5, ratio(DekiFixedColors.resultBandInk, DekiFixedColors.resultBandFill), "resultBandInk")
    }

    /** 帯は止まったスライスの上に重なる。白系の地が沈めた盤面から浮くこと。 */
    @Test
    fun `結果の帯の地は沈めたスライスに対して3対1以上`() {
        val fill = luminance(DekiFixedColors.resultBandFill)
        DekiFixedColors.sliceColors.forEachIndexed { index, slice ->
            val dimmed = luminance(composite(DekiFixedColors.sliceDim, slice))
            assertAtLeast(3.0, (fill + 0.05) / (dimmed + 0.05), "sliceColors[$index]")
        }
    }

    // ── 外観による切り替え ──────────────────────────────────────────────────

    @Test
    fun `地と本文は外観設定で入れ替わる`() {
        assertTrue(luminance(lightDekiColorScheme.ink900) > luminance(darkDekiColorScheme.ink900))
        assertTrue(luminance(lightDekiColorScheme.ivory) < luminance(darkDekiColorScheme.ivory))
    }

    @Test
    fun `色見本はライトとダークで盤面の塗りと同じ数を持つ`() {
        assertEquals(DekiFixedColors.sliceColors.size, lightDekiColorScheme.sliceAccents.size)
        assertEquals(DekiFixedColors.sliceColors.size, darkDekiColorScheme.sliceAccents.size)
    }

    // ── 計算 ────────────────────────────────────────────────────────────────

    private fun assertAtLeast(
        minimum: Double,
        actual: Double,
        label: String,
    ) {
        assertTrue("$label: $actual < $minimum", actual >= minimum)
    }

    private fun ratio(
        foreground: Color,
        background: Color,
    ): Double {
        val front = luminance(foreground)
        val back = luminance(background)
        val high = maxOf(front, back)
        val low = minOf(front, back)
        return (high + 0.05) / (low + 0.05)
    }

    /** 半透明の `top` を不透明な `bottom` に重ねたときの見た目の色（sRGB の線形補間）。 */
    private fun composite(
        top: Color,
        bottom: Color,
    ): Color {
        val alpha = top.alpha
        return Color(
            red = top.red * alpha + bottom.red * (1 - alpha),
            green = top.green * alpha + bottom.green * (1 - alpha),
            blue = top.blue * alpha + bottom.blue * (1 - alpha),
            alpha = 1f,
        )
    }

    private fun luminance(color: Color): Double {
        fun channel(value: Float): Double {
            val component = value.toDouble()
            if (component <= 0.04045) return component / 12.92

            return ((component + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }
}
