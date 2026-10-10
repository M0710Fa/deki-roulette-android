package com.basekeita.roulette.feature.order.ui.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.basekeita.roulette.feature.order.R
import com.basekeita.roulette.feature.order.viewdata.OrderResultUiModel
import com.basekeita.roulette.feature.order.viewdata.OrderedItemUiModel
import com.basekeita.roulette.ui.component.atom.DashedBorderBox
import com.basekeita.roulette.ui.modifier.revealOnAppear
import com.basekeita.roulette.ui.rule.TypeLayout
import com.basekeita.roulette.ui.theme.DekiTheme
import com.basekeita.roulette.ui.theme.DekiTypography
import java.util.UUID

/** 結果のリストの最大幅。 */
private val resultMaxWidth = 384.dp

/** 順位の欄の幅。2 桁の数字が入る幅を、文字と同じ比率で伸ばす（iOS の `@ScaledMetric`）。 */
private val rankWidth = 20.dp

private val rowShape = RoundedCornerShape(12.dp)

/** 1 位の行の背景と枠に掛ける `gold` の不透明度。 */
private const val TOP_ROW_BACKGROUND_ALPHA = 0.1f
private const val TOP_ROW_BORDER_ALPHA = 0.6f

/**
 * 順位付きの結果。1 件ずつ現れる遅延をここで掛ける。結果が無いときは破線の枠で案内を出す。
 *
 * @param result 並べ替えの結果。null のときは案内を出す。
 * @param revealing 演出中。結果が無いまま演出中のときは「並べ替え中…」と出す。
 * @param reducedMotion 「アニメーションを削除」。行を動かさずに出す。
 */
@Composable
internal fun OrderResultList(
    result: OrderResultUiModel?,
    revealing: Boolean,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val colorScheme = DekiTheme.colorScheme
    if (result == null) {
        DashedBorderBox(
            cornerRadius = 16.dp,
            modifier = modifier
                .widthIn(max = resultMaxWidth)
                .fillMaxWidth(),
        ) {
            Text(
                text = if (revealing) {
                    stringResource(R.string.order_shuffling)
                } else {
                    stringResource(R.string.order_result_placeholder)
                },
                style = DekiTypography.caption,
                color = colorScheme.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 40.dp),
            )
        }
        return
    }

    // 結果が変わるたびに行を作り直して演出をやり直す
    key(result.shuffleCount) {
        Column(
            modifier = modifier
                .widthIn(max = resultMaxWidth)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            result.rows.forEachIndexed { index, row ->
                OrderedRow(
                    rank = index + 1,
                    row = row,
                    modifier = Modifier.revealOnAppear(reducedMotion = reducedMotion, delayMillis = row.revealDelayMillis),
                )
            }
        }
    }
}

@Composable
private fun OrderedRow(
    rank: Int,
    row: OrderedItemUiModel,
    modifier: Modifier = Modifier,
) {
    val colorScheme = DekiTheme.colorScheme
    val top = rank == 1
    val fontScale = LocalDensity.current.fontScale
    val rankDescription = stringResource(R.string.order_rank_accessibility_label, rank)
    val textColor = if (top) {
        colorScheme.gold
    } else {
        colorScheme.ivory
    }
    val backgroundColor = if (top) {
        colorScheme.gold.copy(alpha = TOP_ROW_BACKGROUND_ALPHA)
    } else {
        colorScheme.ink800
    }
    val borderColor = if (top) {
        colorScheme.gold.copy(alpha = TOP_ROW_BORDER_ALPHA)
    } else {
        colorScheme.ink700
    }
    val rankColor = if (top) {
        colorScheme.gold
    } else {
        colorScheme.muted
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor, rowShape)
            .border(1.dp, borderColor, rowShape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rank.toString(),
            // 順位の幅が揺れないよう、数字を等幅にする
            style = DekiTypography.subheadline.copy(fontWeight = FontWeight.Black, fontFeatureSettings = "tnum"),
            color = rankColor,
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(rankWidth * fontScale)
                .semantics { contentDescription = rankDescription },
        )
        Text(
            text = row.label,
            style = DekiTypography.subheadline.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            maxLines = TypeLayout.labelMaxLines(fontScale),
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(widthDp = 360)
@Composable
private fun OrderResultListPreview() {
    val rows = listOf("A チーム", "B チーム", "C チーム", "D チーム").map { label ->
        OrderedItemUiModel(id = UUID.randomUUID(), label = label, revealDelayMillis = 0)
    }
    DekiTheme {
        OrderResultList(
            result = OrderResultUiModel(shuffleCount = 1, rows = rows),
            revealing = false,
            reducedMotion = false,
        )
    }
}

@Preview(widthDp = 360)
@Composable
private fun OrderResultListPlaceholderPreview() {
    DekiTheme {
        OrderResultList(result = null, revealing = false, reducedMotion = false)
    }
}
