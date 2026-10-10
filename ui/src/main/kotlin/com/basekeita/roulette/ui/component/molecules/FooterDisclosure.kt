package com.basekeita.roulette.ui.component.molecules

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.basekeita.roulette.ui.R
import com.basekeita.roulette.ui.theme.DekiLayout
import com.basekeita.roulette.ui.theme.DekiMotion
import com.basekeita.roulette.ui.theme.DekiTheme
import com.basekeita.roulette.ui.theme.DekiTypography

private val chevronSize = 20.dp

/**
 * フッターの折りたたみ。見出しを押すと中身を開閉する。見出しの行は押せる高さを 48dp 取る。
 * 開いている間は見出しを選択中として読み上げる（iOS の `isSelected` と同じ）。
 */
@Composable
fun FooterDisclosure(
    title: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colorScheme = DekiTheme.colorScheme
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) {
            180f
        } else {
            0f
        },
        animationSpec = DekiMotion.disclosure(),
        label = "FooterDisclosureChevron",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = DekiLayout.minTouchTarget)
                .clickable(role = Role.Button) { onExpandedChange(!expanded) }
                .semantics { selected = expanded },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_expand_more),
                contentDescription = null,
                tint = colorScheme.muted,
                modifier = Modifier
                    .size(chevronSize)
                    .rotate(chevronRotation),
            )
            Text(
                text = title,
                style = DekiTypography.callout.copy(fontWeight = FontWeight.Bold),
                color = colorScheme.ivory,
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(DekiMotion.disclosure()) + fadeIn(DekiMotion.disclosure()),
            exit = shrinkVertically(DekiMotion.disclosure()) + fadeOut(DekiMotion.disclosure()),
        ) {
            Column(
                modifier = Modifier.padding(start = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content,
            )
        }
    }
}
