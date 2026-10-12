package com.basekeita.roulette.feature.roulette.ui.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.basekeita.roulette.ui.theme.DekiFixedColors
import com.basekeita.roulette.ui.theme.DekiTheme

/** 下向きの三角形。上辺が盤面の外側、頂点が盤面の中心を向く。 */
private val pointerShape = GenericShape { size, _ ->
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width / 2, size.height)
    close()
}

/** 盤面の 12 時に固定する針。位置（停止の瞬間の跳ね）は呼び出し側が `modifier` で決める。 */
@Composable
internal fun WheelPointer(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 22.dp, height = 26.dp)
            .dropShadow(
                shape = pointerShape,
                shadow = Shadow(radius = 3.dp, color = DekiTheme.colorScheme.pointerShadow, offset = DpOffset(0.dp, 3.dp)),
            ).background(DekiFixedColors.flare, pointerShape),
    )
}
