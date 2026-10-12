package com.basekeita.roulette.ui.component.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.min
import com.basekeita.roulette.ui.theme.DekiLayout

/**
 * 横並びにするか。ウィンドウの幅が [DekiLayout.expandedMinWidth] 以上なら横並び（iOS の `horizontalSizeClass == .regular` にあたる）。
 * 分割画面でウィンドウが狭くなれば縦積みに戻る。
 */
@Composable
fun isExpandedWidth(): Boolean = LocalWindowInfo.current.containerDpSize.width >= DekiLayout.expandedMinWidth

/**
 * 横幅に余裕があるときは横並び、なければ縦積み。Web 版の `lg:flex-row`、iOS 版の `AdaptiveStack` にあたる。
 *
 * 横並びでは [main] を左、[side] を右に置き、上下は中央でそろえる。[side] の幅は、2 列で均等に分けた幅と [sideMaxWidth] の小さいほうで、
 * 残りを [main] に渡す（iOS の `HStack` で、幅の上限がある側が先に均等割りを受け取るのと同じ配分）。
 * 縦積みでは両方を幅いっぱいにして上下に積む。各スロットには、その配置で使う幅の Modifier を渡す。
 */
@Composable
fun AdaptiveStack(
    horizontal: Boolean,
    spacing: Dp,
    sideMaxWidth: Dp,
    main: @Composable (Modifier) -> Unit,
    side: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (horizontal) {
        BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
            val sideWidth = min(sideMaxWidth, (maxWidth - spacing) / 2)
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                main(Modifier.weight(1f))
                side(Modifier.width(sideWidth))
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            main(Modifier.fillMaxWidth())
            side(Modifier.fillMaxWidth())
        }
    }
}
