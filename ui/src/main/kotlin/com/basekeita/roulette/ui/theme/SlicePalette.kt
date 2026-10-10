package com.basekeita.roulette.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * スライスの色をパレットのどの添字で塗るか。盤面の塗り・行の色見本・結果表示の枠で同じ規則を使う。
 *
 * Web 版は `SLICE_COLORS[i % 10]` だが、盤面は環なので末尾は先頭と隣り合う。`count % k == 1` の件数
 * （10 色なら 11 件・21 件）では継ぎ目が同じ色になるため、そのときだけ末尾の添字をずらす。
 * 他の項目は `index % k` のまま変えない。
 */
object SlicePalette {
    fun index(
        at: Int,
        count: Int,
        paletteSize: Int,
    ): Int {
        val base = at % paletteSize
        val seamCollides = paletteSize >= 3 && count > 1 && count % paletteSize == 1 && at == count - 1
        if (!seamCollides) return base

        // 先頭は添字 0、前隣は添字 k - 1。パレットは色相順に並んでいるので、両方から最も離れた真ん中を使う。
        return (paletteSize - 1) / 2
    }
}

/** 盤面の塗り。必ず件数つきで引く（継ぎ目の色を `SlicePalette` がずらすため）。 */
fun sliceColor(
    at: Int,
    count: Int,
): Color {
    val palette = DekiFixedColors.sliceColors
    return palette[SlicePalette.index(at, count, palette.size)]
}

/** 文字・色見本・細い枠に使うスライス色。必ず件数つきで引く。 */
fun DekiColorScheme.sliceAccent(
    at: Int,
    count: Int,
): Color = sliceAccents[SlicePalette.index(at, count, sliceAccents.size)]
