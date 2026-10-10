package com.basekeita.roulette.ui.component.atom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.basekeita.roulette.ui.theme.DekiTheme

private val borderWidth = 1.dp
private val dashLength = 4.dp

/** 中身が無いことを示す破線の枠（線 4 / 間隔 4）。項目が無いときと、順番の結果が無いときに使う。 */
@Composable
fun DashedBorderBox(
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val borderColor = DekiTheme.colorScheme.ink600
    Box(
        modifier = modifier.drawBehind {
            val strokeWidth = borderWidth.toPx()
            val dashPx = dashLength.toPx()
            // iOS の strokeBorder と同じく、線を枠の内側に収める
            val inset = strokeWidth / 2
            drawRoundRect(
                color = borderColor,
                topLeft = Offset(inset, inset),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                cornerRadius = CornerRadius(cornerRadius.toPx() - inset),
                style = Stroke(width = strokeWidth, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, dashPx))),
            )
        },
        contentAlignment = contentAlignment,
        content = content,
    )
}
