package com.basekeita.roulette.ui.component.atoms

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.basekeita.roulette.ui.theme.DekiMotion
import com.basekeita.roulette.ui.uimodel.MarkUiModel

private val dotSize = 12.dp
private val ringWidth = 2.dp
private val lastMarkCenterSize = 4.dp

/**
 * 項目の色見本。指定中は塗りを抜いてリングにする。位置も大きさも変えないので、
 * 気づいていない人にはただの色見本のままに見える。末尾指定だけは中心にも点を置いて先頭と見分けられるようにする。
 * 読み上げには含めない（印の状態は行の `stateDescription` で伝える）。
 */
@Composable
fun MarkDot(
    color: Color,
    mark: MarkUiModel?,
    modifier: Modifier = Modifier,
) {
    Crossfade(
        targetState = mark,
        animationSpec = DekiMotion.markChange(),
        modifier = modifier.size(dotSize),
        label = "MarkDot",
    ) { shownMark ->
        Canvas(modifier = Modifier.size(dotSize)) {
            if (shownMark == null) {
                drawCircle(color = color)
                return@Canvas
            }

            val strokeWidth = ringWidth.toPx()
            // iOS の strokeBorder と同じく、線を円の内側に収める
            drawCircle(
                color = color,
                radius = (size.minDimension - strokeWidth) / 2,
                style = Stroke(width = strokeWidth),
            )
            if (shownMark == MarkUiModel.LAST) {
                drawCircle(color = color, radius = lastMarkCenterSize.toPx() / 2)
            }
        }
    }
}

@Preview
@Composable
private fun MarkDotPreview() {
    Row {
        MarkDot(color = Color(0xFFD92323), mark = null)
        MarkDot(color = Color(0xFFD92323), mark = MarkUiModel.TARGET)
        MarkDot(color = Color(0xFFD92323), mark = MarkUiModel.LAST)
    }
}
