package com.basekeita.roulette.ui.theme

import androidx.compose.ui.unit.dp

/** 画面の寸法。iOS 版 `Theme.Layout` の値（pt を dp に読み替える）。 */
object DekiLayout {
    /** 縦積みでの盤面の上限。盤面のラベルの省略と文字サイズの基準の直径と同じ。 */
    val wheelMaxWidthCompact = 320.dp

    /** 横並びでの盤面の上限。 */
    val wheelMaxWidthExpanded = 480.dp

    /** 横並びでの項目リストの幅。 */
    val listWidthExpanded = 360.dp

    /** 横並びの 2 列の間隔。縦積みでは上下の間隔に使う。 */
    val columnSpacing = 48.dp

    /** ページの左右の余白。 */
    val pageHorizontalPadding = 20.dp

    /** ページ全体の上限。横並びの 2 列 + 間隔 + 左右の余白がちょうど収まる幅（928dp）。 */
    val pageMaxWidth = wheelMaxWidthExpanded + columnSpacing + listWidthExpanded + pageHorizontalPadding * 2

    /** 横並びに切り替える、利用できる幅の下限。 */
    val expandedMinWidth = 600.dp

    /** 押せる範囲の最小の大きさ（Android の推奨値）。 */
    val minTouchTarget = 48.dp
}
