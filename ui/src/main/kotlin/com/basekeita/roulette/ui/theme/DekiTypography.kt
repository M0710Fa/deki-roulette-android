package com.basekeita.roulette.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/**
 * 文字の大きさ。iOS のテキストスタイル（既定の文字サイズでの値）を sp に置き換えたもの。
 * 書体はシステムの既定（iOS の rounded が効くのは欧文だけで、和文は通常のゴシックのため）。
 * 太さは使う側で `copy(fontWeight = ...)` を掛ける。
 */
object DekiTypography {
    val largeTitle = TextStyle(fontSize = 34.sp, lineHeight = 41.sp)
    val title3 = TextStyle(fontSize = 20.sp, lineHeight = 25.sp)
    val body = TextStyle(fontSize = 17.sp, lineHeight = 22.sp)
    val callout = TextStyle(fontSize = 16.sp, lineHeight = 21.sp)
    val subheadline = TextStyle(fontSize = 15.sp, lineHeight = 20.sp)
    val footnote = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)
    val caption = TextStyle(fontSize = 12.sp, lineHeight = 16.sp)
}
