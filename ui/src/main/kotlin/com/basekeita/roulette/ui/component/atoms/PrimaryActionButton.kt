package com.basekeita.roulette.ui.component.atoms

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basekeita.roulette.ui.theme.DekiFixedColors
import com.basekeita.roulette.ui.theme.DekiMotion
import com.basekeita.roulette.ui.theme.DekiTheme
import com.basekeita.roulette.ui.theme.DekiTypography

private const val PRESSED_SCALE = 0.95f

/** スピン・並べ替えの開始ボタン。`flare` はこの操作専用の色。 */
@Composable
fun PrimaryActionButton(
    title: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = DekiTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) PRESSED_SCALE else 1f,
        animationSpec = DekiMotion.pressScale(),
        label = "PrimaryActionButtonScale",
    )

    val shadowModifier = if (enabled) {
        Modifier.dropShadow(
            shape = CircleShape,
            shadow = Shadow(radius = 14.dp, color = colorScheme.flareGlow, offset = DpOffset(0.dp, 8.dp)),
        )
    } else {
        Modifier
    }

    val fillColor = if (enabled) {
        DekiFixedColors.flare
    } else {
        colorScheme.ink700
    }

    val textColor = if (enabled) {
        DekiFixedColors.onFlare
    } else {
        colorScheme.muted
    }

    Text(
        text = title,
        style = DekiTypography.title3.copy(fontWeight = FontWeight.Black, letterSpacing = 0.5.sp),
        color = textColor,
        modifier = modifier
            .scale(scale)
            .then(shadowModifier)
            .background(fillColor, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ).padding(horizontal = 48.dp, vertical = 14.dp),
    )
}

@Preview
@Composable
private fun PrimaryActionButtonPreview() {
    DekiTheme {
        PrimaryActionButton(title = "スピン", enabled = true, onClick = {})
    }
}
