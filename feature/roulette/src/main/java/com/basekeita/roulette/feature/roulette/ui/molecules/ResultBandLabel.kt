package com.basekeita.roulette.feature.roulette.ui.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.basekeita.roulette.ui.rule.ResultBand
import com.basekeita.roulette.ui.rule.WheelLabel
import com.basekeita.roulette.ui.theme.DekiFixedColors
import com.basekeita.roulette.ui.theme.DekiTheme

/**
 * 盤面の中心より下に重ねる結果の帯。寸法は `ResultBand` で、盤面と同じく文字の大きさの設定には追従させない。
 * 地と文字は外観に依らず固定し、枠は止まったスライスの塗り（`accent`）にする。1 行に収まらなければ半分まで縮め、それでも収まらなければ末尾を省く。
 *
 * @param diameter 盤面の直径（dp）
 */
@Composable
internal fun ResultBandLabel(
    label: String,
    accent: Color,
    diameter: Float,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val scale = diameter / WheelLabel.REFERENCE_DIAMETER.toFloat()
    // 文字の大きさの設定で変わらないよう、dp の大きさをそのときの fontScale で割り戻して sp にする
    val fontSize = with(density) { ResultBand.fontSize(diameter.toDouble()).toFloat().dp.toSp() }
    val minimumFontSize = with(density) { (ResultBand.fontSize(diameter.toDouble()) * ResultBand.MINIMUM_SCALE_FACTOR).toFloat().dp.toSp() }

    BasicText(
        text = label,
        style = TextStyle(color = DekiFixedColors.resultBandInk, fontSize = fontSize, fontWeight = FontWeight.Black),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased(minFontSize = minimumFontSize, maxFontSize = fontSize),
        modifier = modifier
            .widthIn(max = ResultBand.maxWidth(diameter.toDouble()).toFloat().dp)
            .dropShadow(
                shape = CircleShape,
                shadow = Shadow(
                    radius = (4 * scale).dp,
                    color = DekiTheme.colorScheme.pointerShadow,
                    offset = DpOffset(0.dp, (2 * scale).dp),
                ),
            ).background(DekiFixedColors.resultBandFill, CircleShape)
            .border((2 * scale).dp, accent, CircleShape)
            .padding(
                horizontal = ResultBand.horizontalPadding(diameter.toDouble()).toFloat().dp,
                vertical = ResultBand.verticalPadding(diameter.toDouble()).toFloat().dp,
            ),
    )
}
