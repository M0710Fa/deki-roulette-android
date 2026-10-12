package com.basekeita.roulette.ui.component.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.basekeita.roulette.ui.R
import com.basekeita.roulette.ui.component.atoms.MarkDot
import com.basekeita.roulette.ui.rule.TypeLayout
import com.basekeita.roulette.ui.theme.DekiLayout
import com.basekeita.roulette.ui.theme.DekiTheme
import com.basekeita.roulette.ui.theme.DekiTypography
import com.basekeita.roulette.ui.uimodel.ItemUiModel
import com.basekeita.roulette.ui.uimodel.MarkUiModel
import java.util.UUID

/** 指定の隠しジェスチャ。短すぎると通常のタップで誤爆する。 */
private const val LONG_PRESS_MILLIS = 600L

/** 長押しの間に指が動いてよい距離。超えたら長押しとみなさない。 */
private val longPressSlop = 10.dp

private val rowShape = RoundedCornerShape(12.dp)
private val removeIconSize = 14.dp
private val duplicateIconSize = 12.dp

/**
 * 項目リストの 1 行。色見本・ラベル・✕ボタン。左にスワイプすると削除する（M3 の `SwipeToDismissBox`）。
 *
 * 通常のタップでは何も起きない。長押しだけを拾うので、見ている人には「ただ項目に触れただけ」にしか映らない。
 * 長押しは自前のジェスチャで受け、読み上げのアクションとしては見せない（`combinedClickable` は使わない）。
 * 長押しが成立した操作では、そのまま指を動かしてもスワイプは始めない。
 *
 * @param mark 出す印。伏せている間は null を渡す（そのとき `selected` も `stateDescription` も付けない）。
 * @param onPressingChange 行に指が触れた / 離れた。印を押している間だけ出すために使う。
 * @param onRemove ✕ボタンかスワイプで削除した。
 */
@Composable
fun ItemRow(
    item: ItemUiModel,
    color: Color,
    mark: MarkUiModel?,
    busy: Boolean,
    onPressingChange: (Boolean) -> Unit,
    onLongPress: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = DekiTheme.colorScheme
    val markStateDescription = markStateDescription(mark)
    // 長押しのジェスチャは作り直さずに使い続けるので、最新の値は State から読む
    val currentBusy by rememberUpdatedState(busy)
    val currentOnPressingChange by rememberUpdatedState(onPressingChange)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    // 今回の操作で長押しの指定が成立した。触れ始めるたびに戻す
    var longPressed by remember { mutableStateOf(false) }

    val borderColor = if (mark != null) {
        colorScheme.ink500
    } else {
        colorScheme.ink700
    }

    SwipeToDeleteBox(
        onDelete = onRemove,
        enabled = !busy && !longPressed,
        shape = rowShape,
        modifier = modifier.fillMaxWidth(),
    ) {
        ItemRowContent(
            item = item,
            color = color,
            mark = mark,
            markStateDescription = markStateDescription,
            borderColor = borderColor,
            busy = busy,
            onPressingChange = { pressing ->
                if (pressing) longPressed = false
                currentOnPressingChange(pressing)
            },
            isBusy = { currentBusy },
            onLongPress = {
                longPressed = true
                currentOnLongPress()
            },
            onRemove = onRemove,
        )
    }
}

/** 行の中身。スワイプで横にずらす部分。 */
@Composable
private fun ItemRowContent(
    item: ItemUiModel,
    color: Color,
    mark: MarkUiModel?,
    markStateDescription: String?,
    borderColor: Color,
    busy: Boolean,
    onPressingChange: (Boolean) -> Unit,
    isBusy: () -> Boolean,
    onLongPress: () -> Unit,
    onRemove: () -> Unit,
) {
    val colorScheme = DekiTheme.colorScheme
    val currentOnPressingChange by rememberUpdatedState(onPressingChange)
    val currentOnLongPress by rememberUpdatedState(onLongPress)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.ink800, rowShape)
            .border(1.dp, borderColor, rowShape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .hiddenLongPress(
                    isBusy = isBusy,
                    onPressingChange = { pressing -> currentOnPressingChange(pressing) },
                    onLongPress = { currentOnLongPress() },
                ).semantics(mergeDescendants = true) {
                    // 印を伏せている間は選択中の読み上げも落とす。残すと指定先が伝わる
                    if (mark != null) {
                        selected = true
                        if (markStateDescription != null) {
                            stateDescription = markStateDescription
                        }
                    }
                }.padding(start = 12.dp, top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MarkDot(color = color, mark = mark)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = item.label,
                    style = DekiTypography.subheadline,
                    color = colorScheme.ivory,
                    maxLines = TypeLayout.labelMaxLines(LocalDensity.current.fontScale),
                    overflow = TextOverflow.Ellipsis,
                )
                // 同名の項目が他にある。印とは無関係に、同名の行すべてに同じ注意を出す（行の読み上げにも含める）
                if (item.isDuplicate) {
                    DuplicateNotice()
                }
            }
        }

        IconButton(
            onClick = onRemove,
            enabled = !busy,
            modifier = Modifier.size(DekiLayout.minTouchTarget),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.item_list_remove_accessibility_label, item.label),
                tint = colorScheme.ink400,
                modifier = Modifier.size(removeIconSize),
            )
        }
    }
}

/** 同名の項目があることの注意。アイコンは文言の 1 行目にそろえる。 */
@Composable
private fun DuplicateNotice() {
    val colorScheme = DekiTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(
            painter = painterResource(R.drawable.ic_warning),
            contentDescription = null,
            tint = colorScheme.muted,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(duplicateIconSize),
        )
        Text(
            text = stringResource(R.string.item_list_duplicate_notice),
            style = DekiTypography.caption,
            color = colorScheme.muted,
        )
    }
}

/** 印の状態の読み上げ。当たりの指定には付けない（選択中であることだけを伝える）。 */
@Composable
private fun markStateDescription(mark: MarkUiModel?): String? {
    if (mark == MarkUiModel.FIRST) return stringResource(R.string.mark_first)

    if (mark == MarkUiModel.LAST) return stringResource(R.string.mark_last)

    return null
}

/**
 * 指が触れている間を `onPressingChange` で伝え、動かさずに 600ms 押し続けたら `onLongPress` を呼ぶ。
 *
 * 指が 10dp より動いたとき、親の縦スクロールに取られたとき、指を離したときは長押しにしない。
 * イベントは消費しないので、縦スクロールの操作を妨げない。演出中は押している間の通知だけを続け、指定はしない。
 */
private fun Modifier.hiddenLongPress(
    isBusy: () -> Boolean,
    onPressingChange: (Boolean) -> Unit,
    onLongPress: () -> Unit,
): Modifier = this.pointerInput(Unit) {
    val slopPx = longPressSlop.toPx()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        onPressingChange(true)
        try {
            val interrupted = withTimeoutOrNull(LONG_PRESS_MILLIS) {
                awaitReleaseOrMove(down, slopPx)
            }
            // 時間切れ（null）になるまで指が止まっていたら長押し
            if (interrupted == null && !isBusy()) {
                onLongPress()
            }
        } finally {
            onPressingChange(false)
        }
    }
}

/** 指が離れる・動きすぎる・他のジェスチャに取られるのどれかが起きるまで待つ。 */
private suspend fun AwaitPointerEventScope.awaitReleaseOrMove(
    down: PointerInputChange,
    slopPx: Float,
) {
    while (true) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: return
        if (!change.pressed) return

        if (change.isConsumed) return

        if ((change.position - down.position).getDistance() > slopPx) return

        // 親の縦スクロールは子の後に消費するので、最後のパスで取られたかを確かめる
        val finalChange = awaitPointerEvent(PointerEventPass.Final).changes.firstOrNull { it.id == down.id }
        if (finalChange == null || finalChange.isConsumed) return
    }
}

@Preview
@Composable
private fun ItemRowPreview() {
    DekiTheme {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ItemRow(
                item = ItemUiModel(id = UUID.randomUUID(), label = "ラーメン", isDuplicate = false),
                color = Color(0xFFD92323),
                mark = null,
                busy = false,
                onPressingChange = {},
                onLongPress = {},
                onRemove = {},
            )
            ItemRow(
                item = ItemUiModel(id = UUID.randomUUID(), label = "カレー", isDuplicate = true),
                color = Color(0xFFAF571D),
                mark = MarkUiModel.TARGET,
                busy = false,
                onPressingChange = {},
                onLongPress = {},
                onRemove = {},
            )
        }
    }
}
