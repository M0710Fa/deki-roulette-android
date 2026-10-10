package com.basekeita.roulette.ui.modifier

import androidx.compose.animation.core.Animatable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.basekeita.roulette.ui.theme.DekiMotion
import kotlinx.coroutines.launch

/** 現れる前に下へずらしておく距離。 */
private val revealOffset = 6.dp

/** 現れる前の縮み。 */
private const val REVEAL_START_SCALE = 0.96f

/**
 * 表示された時点から `delayMillis` 後に、下から浮かび上がるように現れる（iOS 版 `revealOnAppear`）。
 * `reducedMotion` のときは動かさずに即座に出す。出し直すときは `key` で要素ごと作り直す。
 */
fun Modifier.revealOnAppear(
    reducedMotion: Boolean,
    delayMillis: Int = 0,
): Modifier = this then RevealOnAppearElement(reducedMotion, delayMillis)

private data class RevealOnAppearElement(
    val reducedMotion: Boolean,
    val delayMillis: Int,
) : ModifierNodeElement<RevealOnAppearNode>() {
    override fun create(): RevealOnAppearNode = RevealOnAppearNode(reducedMotion, delayMillis)

    // 現れる動きは表示されたときの 1 回だけなので、途中で値が変わっても始め直さない
    override fun update(node: RevealOnAppearNode) = Unit

    override fun InspectorInfo.inspectableProperties() {
        name = "revealOnAppear"
        properties["reducedMotion"] = reducedMotion
        properties["delayMillis"] = delayMillis
    }
}

private class RevealOnAppearNode(
    private val reducedMotion: Boolean,
    private val delayMillis: Int,
) : Modifier.Node(),
    LayoutModifierNode {
    private val progress = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            if (reducedMotion) {
                progress.snapTo(1f)
            } else {
                progress.animateTo(1f, DekiMotion.reveal(delayMillis))
            }
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        val offsetPx = revealOffset.toPx()
        return layout(placeable.width, placeable.height) {
            // 進み具合はレイヤーの中で読み、動いている間は描き直しだけで済ませる
            placeable.placeWithLayer(0, 0) {
                val shown = progress.value
                alpha = shown
                translationY = offsetPx * (1f - shown)
                val scale = REVEAL_START_SCALE + (1f - REVEAL_START_SCALE) * shown
                scaleX = scale
                scaleY = scale
            }
        }
    }
}
