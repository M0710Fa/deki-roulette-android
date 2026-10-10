package com.basekeita.roulette.ui.component.molecules

import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.basekeita.roulette.ui.R
import com.basekeita.roulette.ui.theme.DekiTheme
import com.basekeita.roulette.ui.theme.DekiTypography

/** 追加の入力欄が伸びる行数の上限。これを超えるとスクロールする。 */
private const val INPUT_VISIBLE_LINES = 5

/** 入力欄を横並びにするために最低限確保したい幅（文字サイズに合わせて伸ばす）。 */
private val inputMinWidth = 160.dp

private val formSpacing = 8.dp

/**
 * 項目を追加する入力欄（`OutlinedTextField`）とボタン（`FilledTonalButton`）。入力欄に最低限の幅を取れる間は横並び、取れなければ縦積みにする。
 *
 * 入力欄は複数行で、末尾に改行が入ったら送信の合図として扱う。追加したあともフォーカス（キーボード）は保つ。
 *
 * @param input 入力中の文字列。行ごとの文字数の上限は呼び出し側で掛ける。
 * @param lineCount 追加する件数（改行区切りで正規化したあとの行数）。0 なら追加できない。2 件以上ならボタンに件数を出す。
 * @param onSubmit 追加ボタンか、末尾の改行で送信された。
 */
@Composable
fun AddItemForm(
    input: String,
    onInputChange: (String) -> Unit,
    enabled: Boolean,
    lineCount: Int,
    onSubmit: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val minFieldWidthPx = with(LocalDensity.current) { (inputMinWidth * fontScale).roundToPx() }
    val spacingPx = with(LocalDensity.current) { formSpacing.roundToPx() }

    val buttonText = if (lineCount > 1) {
        pluralStringResource(R.plurals.item_list_add_items_button, lineCount, lineCount)
    } else {
        stringResource(R.string.item_list_add_button)
    }

    Layout(
        modifier = modifier,
        content = {
            OutlinedTextField(
                value = input,
                onValueChange = { newValue ->
                    // 末尾の改行を送信の合図として扱う。改行そのものは入力に残さない
                    if (newValue.endsWith('\n')) {
                        onInputChange(newValue.trimEnd('\n', '\r'))
                        onSubmit()
                    } else {
                        onInputChange(newValue)
                    }
                },
                enabled = enabled,
                textStyle = DekiTypography.subheadline,
                placeholder = {
                    Text(text = stringResource(R.string.item_list_add_placeholder), style = DekiTypography.subheadline)
                },
                maxLines = INPUT_VISIBLE_LINES,
                modifier = Modifier.focusRequester(focusRequester),
            )
            FilledTonalButton(
                onClick = onSubmit,
                enabled = enabled && lineCount > 0,
            ) {
                Text(text = buttonText, style = DekiTypography.subheadline.copy(fontWeight = FontWeight.Bold))
            }
        },
    ) { measurables, constraints ->
        val (fieldMeasurable, buttonMeasurable) = measurables
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val button = buttonMeasurable.measure(looseConstraints)
        val fieldWidthInRow = constraints.maxWidth - button.width - spacingPx

        // iOS の ViewThatFits と同じく、入力欄の最小幅が取れる間は横並びにする
        if (fieldWidthInRow >= minFieldWidthPx) {
            val field = fieldMeasurable.measure(Constraints.fixedWidth(fieldWidthInRow))
            val height = maxOf(field.height, button.height)
            layout(constraints.maxWidth, height) {
                field.placeRelative(0, 0)
                button.placeRelative(fieldWidthInRow + spacingPx, (height - button.height) / 2)
            }
        } else {
            val field = fieldMeasurable.measure(Constraints.fixedWidth(constraints.maxWidth))
            layout(constraints.maxWidth, field.height + spacingPx + button.height) {
                field.placeRelative(0, 0)
                button.placeRelative(0, field.height + spacingPx)
            }
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun AddItemFormPreview() {
    DekiTheme {
        AddItemForm(
            input = "",
            onInputChange = {},
            enabled = true,
            lineCount = 0,
            onSubmit = {},
            focusRequester = remember { FocusRequester() },
        )
    }
}
