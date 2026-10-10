package com.basekeita.roulette.ui.component.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.basekeita.roulette.ui.R
import com.basekeita.roulette.ui.theme.DekiFixedColors
import com.basekeita.roulette.ui.theme.DekiTheme

/**
 * 行を左（end → start）にスワイプして削除する（M3 の `SwipeToDismissBox`）。項目リストの行と保存したリストの管理の行で共有する。
 *
 * スワイプで出る背景は飾りで、読み上げには出さない。読み上げでは行の「✕」やメニューで同じ操作を行う。
 *
 * @param enabled スワイプを受けるか。演出中や、長押しの指定が成立した操作では受けない。
 * @param shape 行の形。スワイプで出る背景も同じ形で塗る。
 */
@Composable
fun SwipeToDeleteBox(
    onDelete: () -> Unit,
    enabled: Boolean,
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val currentOnDelete by rememberUpdatedState(onDelete)
    val state = rememberSwipeToDismissBoxState()

    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            // 止まっている間は出さない。角丸の縁から背景の色がにじまないようにする
            if (state.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                DeleteBackground(shape = shape)
            }
        },
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        gesturesEnabled = enabled,
        onDismiss = { currentOnDelete() },
    ) {
        content()
    }
}

/** スワイプで現れる削除の背景。右端に削除のアイコンを置く。 */
@Composable
private fun DeleteBackground(shape: Shape) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(DekiFixedColors.destructive, shape)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_delete),
            contentDescription = null,
            tint = DekiFixedColors.onDestructive,
        )
    }
}

@Preview(widthDp = 360, heightDp = 56)
@Composable
private fun DeleteBackgroundPreview() {
    DekiTheme {
        DeleteBackground(shape = RoundedCornerShape(12.dp))
    }
}
