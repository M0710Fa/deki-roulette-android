package com.basekeita.roulette.ui.component.templates

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basekeita.roulette.ui.R
import com.basekeita.roulette.ui.component.molecules.FooterDisclosure
import com.basekeita.roulette.ui.theme.DekiLayout
import com.basekeita.roulette.ui.theme.DekiTheme
import com.basekeita.roulette.ui.theme.DekiTypography

/** フッターの説明文。iOS の `lineSpacing(3)` に合わせて行の高さを本文より 3 広げる。 */
private val footerTextStyle = DekiTypography.subheadline.copy(
    lineHeight = (DekiTypography.subheadline.lineHeight.value + 3).sp,
)

/**
 * 両画面で共通の枠。縦スクロールの中に、ヘッダ（タイトル・タグライン・設定ボタン）、本文、フッターの折りたたみ 3 つを置く。
 *
 * 演出が始まったら（`busy`）「使い方」だけを閉じる。隠し操作の説明は「使い方」の中にだけ置くので、人前で回すときに開いたままにしない。
 *
 * 枠は M3 の `Scaffold` で、下端に Snackbar（項目を削除したときの「元に戻す」）を出す。
 *
 * @param useCases 「こんなときに」の本文。
 * @param snackbarHostState 画面の Snackbar。項目リストが「元に戻す」を出す。
 * @param help 「使い方」の中身。段落は `FooterText`、小見出しは `HelpHeading` で並べる。
 */
@Composable
fun PageFrame(
    title: String,
    tagline: String,
    busy: Boolean,
    useCases: String,
    snackbarHostState: SnackbarHostState,
    onSettingsClick: () -> Unit,
    help: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colorScheme = DekiTheme.colorScheme
    var helpOpen by rememberSaveable { mutableStateOf(false) }
    var useCasesOpen by rememberSaveable { mutableStateOf(false) }
    var noticeOpen by rememberSaveable { mutableStateOf(false) }

    // 開始した瞬間に畳む。人前で回すときに開きっぱなしを踏まないための保険
    LaunchedEffect(busy) {
        if (busy) helpOpen = false
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = colorScheme.ink900,
        // システムバーの分は app が空ける
        contentWindowInsets = WindowInsets(0.dp),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = DekiLayout.pageMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = DekiLayout.pageHorizontalPadding, vertical = 24.dp),
            ) {
                PageHeader(
                    title = title,
                    tagline = tagline,
                    onSettingsClick = onSettingsClick,
                    modifier = Modifier.padding(bottom = 40.dp),
                )
                content()
                Column(
                    modifier = Modifier.padding(top = 56.dp),
                    // 見出しの行が 48dp の高さを持つので、間隔は詰める
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    HorizontalDivider(color = colorScheme.ink700)
                    FooterDisclosure(
                        title = stringResource(R.string.page_help_title),
                        expanded = helpOpen,
                        onExpandedChange = { expanded -> helpOpen = expanded },
                        content = help,
                    )
                    FooterDisclosure(
                        title = stringResource(R.string.page_use_cases_title),
                        expanded = useCasesOpen,
                        onExpandedChange = { expanded -> useCasesOpen = expanded },
                    ) {
                        FooterText(text = useCases)
                    }
                    FooterDisclosure(
                        title = stringResource(R.string.page_notice_title),
                        expanded = noticeOpen,
                        onExpandedChange = { expanded -> noticeOpen = expanded },
                    ) {
                        FooterText(text = stringResource(R.string.page_notice))
                    }
                }
            }
        }
    }
}

/** フッターの説明文の 1 段落。 */
@Composable
fun FooterText(text: String) {
    Text(
        text = text,
        style = footerTextStyle,
        color = DekiTheme.colorScheme.muted,
    )
}

/** フッターの説明文で使う小見出し。 */
@Composable
fun HelpHeading(text: String) {
    Text(
        text = text,
        style = DekiTypography.subheadline.copy(fontWeight = FontWeight.Bold),
        color = DekiTheme.colorScheme.ivory,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
private fun PageHeader(
    title: String,
    tagline: String,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = DekiTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = DekiTypography.largeTitle.copy(fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
                color = colorScheme.ivory,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = tagline,
                style = DekiTypography.subheadline,
                color = colorScheme.muted,
            )
        }
        SettingsButton(onClick = onSettingsClick)
    }
}

/** ヘッダの歯車（M3 の `OutlinedIconButton`。押せる範囲は 48dp）。 */
@Composable
private fun SettingsButton(onClick: () -> Unit) {
    val colorScheme = DekiTheme.colorScheme
    OutlinedIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.outlinedIconButtonColors(
            containerColor = colorScheme.ink800,
            contentColor = colorScheme.muted,
        ),
        border = BorderStroke(1.dp, colorScheme.ink700),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_settings),
            contentDescription = stringResource(R.string.page_settings),
        )
    }
}

@Preview(widthDp = 360)
@Composable
private fun PageFramePreview() {
    DekiTheme {
        PageFrame(
            title = "デキレーレット",
            tagline = "迷ったら、まわす。",
            busy = false,
            useCases = "飲み会の幹事決め、ランチのお店選び。",
            snackbarHostState = remember { SnackbarHostState() },
            onSettingsClick = {},
            help = {
                FooterText(text = "項目を追加してスピンを押すだけです。")
                HelpHeading(text = "結果を決めておきたいとき")
            },
        ) {
            FooterText(text = "本文")
        }
    }
}
