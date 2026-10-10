package com.basekeita.roulette.ui.component.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.basekeita.roulette.ui.R
import com.basekeita.roulette.ui.rule.ResultText
import com.basekeita.roulette.ui.theme.DekiTheme
import com.basekeita.roulette.ui.theme.DekiTypography
import kotlinx.coroutines.delay

/** 「コピーしました」を出しておく時間。 */
private const val COPY_FEEDBACK_MILLIS = 1_800L

private val shareIconSize = 12.dp

/**
 * 結果の下に置く「結果をコピー」と「共有」。両画面で同じ見た目にする。
 * 開始ボタンの `flare` より目立たせないため、どちらも控えめな見た目にする。
 * 結果が変わると呼び出し側で作り直すので、「コピーしました」も自然に戻る。
 *
 * @param copyText クリップボードに書き込む本文（`ResultText`）。共有にはこの後にアプリ名を足す
 */
@Composable
fun ResultActions(
    copyText: String,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shareText = ResultText.share(copyText, appName = stringResource(R.string.result_app_name))
    var copied by remember { mutableStateOf(false) }
    var copyCount by remember { mutableIntStateOf(0) }

    // コピーし直すたびに表示時間を延ばす
    LaunchedEffect(copyCount) {
        if (copyCount == 0) return@LaunchedEffect

        copied = true
        delay(COPY_FEEDBACK_MILLIS)
        copied = false
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ResultActionLabel(
            title = if (copied) {
                stringResource(R.string.result_copied)
            } else {
                stringResource(R.string.result_copy)
            },
            showsShareIcon = false,
            onClick = {
                onCopy(copyText)
                copyCount += 1
            },
        )
        ResultActionLabel(
            title = stringResource(R.string.result_share),
            showsShareIcon = true,
            onClick = { onShare(shareText) },
        )
    }
}

/** コピー・共有ボタンのラベル。`muted` の文字にカプセルの地と枠。 */
@Composable
private fun ResultActionLabel(
    title: String,
    showsShareIcon: Boolean,
    onClick: () -> Unit,
) {
    val colorScheme = DekiTheme.colorScheme
    Row(
        modifier = Modifier
            // 見た目のカプセルは小さいまま、押せる範囲は 48dp 取る
            .minimumInteractiveComponentSize()
            .background(colorScheme.ink800, CircleShape)
            .border(1.dp, colorScheme.ink700, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showsShareIcon) {
            Icon(
                painter = painterResource(R.drawable.ic_share),
                contentDescription = null,
                tint = colorScheme.muted,
                modifier = Modifier.size(shareIconSize),
            )
        }
        Text(
            text = title,
            style = DekiTypography.caption.copy(fontWeight = FontWeight.Bold),
            color = colorScheme.muted,
            // 隣のラベルが「コピーしました」に変わって幅が動いても、こちらが折り返さないようにする
            softWrap = false,
            maxLines = 1,
        )
    }
}

@Preview
@Composable
private fun ResultActionsPreview() {
    DekiTheme {
        ResultActions(copyText = "結果: ラーメン", onCopy = {}, onShare = {})
    }
}
