package com.basekeita.roulette.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalDekiColorScheme = staticCompositionLocalOf { lightDekiColorScheme }

/** アプリのテーマ。色は `DekiTheme.colorScheme`（外観で変わる色）と `DekiFixedColors`（変わらない色）から引く。 */
object DekiTheme {
    val colorScheme: DekiColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalDekiColorScheme.current

    @Composable
    operator fun invoke(
        darkTheme: Boolean = isSystemInDarkTheme(),
        content: @Composable () -> Unit,
    ) {
        val dekiColorScheme = if (darkTheme) {
            darkDekiColorScheme
        } else {
            lightDekiColorScheme
        }

        val materialColorScheme = if (darkTheme) {
            materialColorSchemeOf(darkDekiColorScheme, darkColorScheme())
        } else {
            materialColorSchemeOf(lightDekiColorScheme, lightColorScheme())
        }

        CompositionLocalProvider(LocalDekiColorScheme provides dekiColorScheme) {
            MaterialTheme(
                colorScheme = materialColorScheme,
                content = content,
            )
        }
    }
}

/**
 * Material の部品（ダイアログ・メニュー・タブバー）が使う色を、アプリの配色に寄せる。
 * 部品の既定の紫を出さないため、使う役割をすべてアプリのトークンで上書きする。
 */
private fun materialColorSchemeOf(
    scheme: DekiColorScheme,
    base: ColorScheme,
): ColorScheme = base.copy(
    primary = scheme.ivory,
    onPrimary = scheme.ink900,
    primaryContainer = scheme.ink700,
    onPrimaryContainer = scheme.ivory,
    secondary = scheme.muted,
    onSecondary = scheme.ink900,
    secondaryContainer = scheme.ink700,
    onSecondaryContainer = scheme.ivory,
    background = scheme.ink900,
    onBackground = scheme.ivory,
    surface = scheme.ink800,
    onSurface = scheme.ivory,
    surfaceVariant = scheme.ink700,
    onSurfaceVariant = scheme.muted,
    surfaceContainerLowest = scheme.ink800,
    surfaceContainerLow = scheme.ink800,
    surfaceContainer = scheme.ink800,
    surfaceContainerHigh = scheme.ink800,
    surfaceContainerHighest = scheme.ink700,
    surfaceTint = scheme.ink800,
    outline = scheme.ink600,
    outlineVariant = scheme.ink700,
    error = scheme.flareText,
    // Snackbar（「元に戻す」）の地と文字。画面の地と本文を入れ替えた色にする
    inverseSurface = scheme.ivory,
    inverseOnSurface = scheme.ink900,
    inversePrimary = scheme.ink900,
)
