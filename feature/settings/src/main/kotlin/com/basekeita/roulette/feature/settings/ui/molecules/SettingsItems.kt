package com.basekeita.roulette.feature.settings.ui.molecules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.basekeita.roulette.feature.settings.R
import com.basekeita.roulette.ui.theme.DekiTheme
import com.basekeita.roulette.ui.theme.DekiTypography
import com.basekeita.roulette.ui.R as UiR

/** 行の地はシートの色のまま。 */
@Composable
private fun settingsItemColors(): ListItemColors = ListItemDefaults.colors(containerColor = Color.Transparent)

/** 行のまとまりの見出し。 */
@Composable
internal fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = DekiTypography.footnote.copy(fontWeight = FontWeight.Bold),
        color = DekiTheme.colorScheme.muted,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
            .semantics { heading() },
    )
}

/** スイッチの 1 行。行のどこを押しても切り替わる。 */
@Composable
internal fun SettingsSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    supportingText: String? = null,
) {
    ListItem(
        headlineContent = { Text(text = title, style = DekiTypography.subheadline) },
        supportingContent = supportingText?.let { text ->
            { Text(text = text, style = DekiTypography.caption) }
        },
        // 切り替えは行の toggleable が受けるので、スイッチ自体は押せない飾りにする
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        colors = settingsItemColors(),
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    )
}

/** 押すと次のページに進む 1 行。 */
@Composable
internal fun SettingsNavigationItem(
    title: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(text = title, style = DekiTypography.subheadline) },
        trailingContent = {
            Icon(painter = painterResource(UiR.drawable.ic_chevron_right), contentDescription = null)
        },
        colors = settingsItemColors(),
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
    )
}

/** 項目を初期状態に戻す 1 行。 */
@Composable
internal fun SettingsResetItem(
    title: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(text = title, style = DekiTypography.subheadline) },
        leadingContent = {
            Icon(painter = painterResource(R.drawable.ic_reset), contentDescription = null)
        },
        colors = settingsItemColors(),
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
    )
}

/** 選択肢の 1 行。行のどこを押しても選べる。 */
@Composable
internal fun SettingsRadioItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(text = title, style = DekiTypography.subheadline) },
        // 選択は行の selectable が受けるので、ラジオボタン自体は押せない飾りにする
        leadingContent = { RadioButton(selected = selected, onClick = null) },
        colors = settingsItemColors(),
        modifier = Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    )
}

/** 押せない説明の 1 行（著作権など）。 */
@Composable
internal fun SettingsTextItem(
    title: String,
    supportingText: String? = null,
) {
    ListItem(
        headlineContent = { Text(text = title, style = DekiTypography.subheadline) },
        supportingContent = supportingText?.let { text ->
            { Text(text = text, style = DekiTypography.caption) }
        },
        colors = settingsItemColors(),
    )
}
