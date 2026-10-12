package com.basekeita.roulette.ui.component.templates

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.basekeita.roulette.ui.R
import com.basekeita.roulette.ui.theme.DekiTheme
import com.basekeita.roulette.ui.theme.DekiTypography

/**
 * ボトムシートで出す画面の枠。上に見出しの帯（`TopAppBar`）、その下に中身を置く。
 * シートは下げる・外側を押す・戻る操作で閉じるので、「閉じる」ボタンは置かない。
 * シートの中で次のページに進んでいるときは [onBack] を渡し、見出しの左に戻るボタンを出す。
 * 中身のスクロールは中身の側で持つ。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetFrame(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        TopAppBar(
            title = {
                Text(
                    text = title,
                    style = DekiTypography.body.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                }
            },
            // シートの中なのでシステムバーの分は空けず、地はシートの色のまま
            windowInsets = WindowInsets(0.dp),
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )
        content()
    }
}

@Preview(widthDp = 360)
@Composable
private fun SheetFramePreview() {
    DekiTheme {
        SheetFrame(title = "ルーレットの詳細設定", onBack = {}) {
            Text(
                text = "中身",
                color = DekiTheme.colorScheme.ivory,
                modifier = Modifier.padding(20.dp),
            )
        }
    }
}
