package com.basekeita.roulette.feature.roulette.ui.organisms

import android.graphics.BlurMaskFilter
import android.graphics.Matrix
import android.graphics.SweepGradient
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.basekeita.roulette.feature.roulette.flick.FlickSample
import com.basekeita.roulette.feature.roulette.flick.FlickSampleBuffer
import com.basekeita.roulette.feature.roulette.flick.FlickVelocity
import com.basekeita.roulette.feature.roulette.ui.molecules.WheelPointer
import com.basekeita.roulette.feature.roulette.uimodel.GlowStyleUiModel
import com.basekeita.roulette.feature.roulette.uimodel.SpinAnimationUiModel
import com.basekeita.roulette.feature.roulette.uimodel.SpinEasingUiModel
import com.basekeita.roulette.feature.roulette.uimodel.WheelSliceUiModel
import com.basekeita.roulette.ui.rule.WheelDrag
import com.basekeita.roulette.ui.rule.WheelLabel
import com.basekeita.roulette.ui.theme.DekiFixedColors
import com.basekeita.roulette.ui.theme.DekiMotion
import com.basekeita.roulette.ui.theme.DekiStopEmphasis
import com.basekeita.roulette.ui.theme.DekiTheme
import com.basekeita.roulette.ui.theme.sliceColor
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** 盤面の外枠からスライスの円までの距離（基準直径での dp）。 */
private const val SLICE_INSET = 16f

/** 外周の縁の外側の半径（スライスの円からの距離。基準直径での dp）。 */
private const val RIM_OUTSET = 11f

/** 外周線の半径（スライスの円からの距離。基準直径での dp）と太さ。 */
private const val EDGE_OUTSET = 5f
private const val EDGE_WIDTH = 1.5f

/** スライスの区切り線の太さ（基準直径での dp）。 */
private const val SEPARATOR_WIDTH = 2f

/** 中心のハブの直径・縁の太さ・中心の点の直径（基準直径での dp）。 */
private const val HUB_DIAMETER = 38f
private const val HUB_BORDER_WIDTH = 2.5f
private const val HUB_DOT_DIAMETER = 12f

/** 針を盤面の上端からはみ出させる量。 */
private val pointerOverhang = 6.dp

/**
 * スピンの回転を開発者オプションのアニメーション倍率に依らず決まった長さで回す。
 * 長さが変わると、開始時に求めた回転音・触覚の時刻とずれるため。
 */
private object UnscaledMotionDuration : MotionDurationScale {
    override val scaleFactor: Float = 1f
}

/**
 * 盤面。角度は 12 時を 0 度として時計回りに数え、針は 12 時に固定して盤面を回す。
 *
 * スピンが始まるたびに（`spin.spinCount` が変わるたびに）`spin.targetRotation` まで `spin.easing` の曲線で回し、止まったら `onSpinFinished` を呼ぶ。
 * `highlightedIndex` を渡すと、そのスライスを止まった位置として強調する（他を沈め、止まったスライスに縁取りと光彩を付け、
 * 短く押し出して少し大きいまま残し、針を跳ねさせる）。null に戻すと強調も解ける。
 * 盤面は装飾として扱い、読み上げない（結果は読み上げで伝える）。
 *
 * 止まっている間は、盤面をドラッグすると指に追従して回り、指を離すとその角度からフリックのスピンを始める（結果は開始時に決まる）。
 * 追従で回した角度はこの部品が持ち、指を離したら `onRelease` で ViewModel に渡す（Animatable にも取り込んで盤面を跳ねさせない）。
 * 結果が出たあとに指で動かしたら強調を解く（結果の表示は残る）。新しい結果（または結果なし）になったら戻す。
 *
 * @param spinning 演出中。止まっている間に回転の値が変わったら（画面を作り直したとき）回さずに合わせる
 * @param interactive 指で回せるか（演出中は false）。演出中に触り始めたドラッグは、指を離すまで追従もフリックもしない
 * @param reducedMotion 「アニメーションを削除」。回さずに止まる角度へ移し（終わらせるのは保険のタイマー）、
 *   押し出しと針の跳ねを省いて沈める・縁取り・光彩だけにする
 * @param onBoundaryCross 追従で針がスライスの境目を越えた。引数はその時刻（秒）
 * @param onRelease 指を離した。角速度（度/秒、時計回りが正）と、追従で回した角度。指を離さずに取り消されたときは角速度が null
 */
@Composable
internal fun RouletteWheel(
    slices: List<WheelSliceUiModel>,
    spin: SpinAnimationUiModel,
    spinning: Boolean,
    interactive: Boolean,
    highlightedIndex: Int?,
    glowStyle: GlowStyleUiModel,
    reducedMotion: Boolean,
    onSpinFinished: () -> Unit,
    onBoundaryCross: (timeSeconds: Double) -> Unit,
    onRelease: (angularVelocity: Double?, dragRotation: Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation = remember { Animatable(spin.targetRotation) }
    val currentSpinning by rememberUpdatedState(spinning)
    val currentReducedMotion by rememberUpdatedState(reducedMotion)
    val currentOnSpinFinished by rememberUpdatedState(onSpinFinished)
    // 追従で回した角度。指を離すと Animatable に取り込んで 0 に戻す。スピンの曲線では補間しない
    var dragRotation by remember { mutableFloatStateOf(0f) }
    // 結果が出たあとに盤面を指で動かした。止まったスライスが針の下から外れるので強調を解く
    var displacedByDrag by remember { mutableStateOf(false) }
    val emphasis = rememberStopEmphasis(highlightedIndex.takeUnless { displacedByDrag }, reducedMotion)

    // 新しい結果（または結果なし）になったら、指で動かした分の強調の解除を戻す
    LaunchedEffect(highlightedIndex) {
        displacedByDrag = false
    }

    LaunchedEffect(spin.spinCount) {
        val target = spin.targetRotation
        // 回し始める前に画面を作り直したときなど、すでに止まる角度にいるなら合わせるだけにする（終わらせるのは保険のタイマー）。
        // 「アニメーションを削除」のときも回さずに合わせる
        if (!currentSpinning || currentReducedMotion || rotation.value == target) {
            rotation.snapTo(target)
            return@LaunchedEffect
        }

        withContext(UnscaledMotionDuration) {
            rotation.animateTo(target, tween(durationMillis = spin.durationMillis, easing = spin.easing.toEasing()))
        }
        currentOnSpinFinished()
    }

    val scope = rememberCoroutineScope()
    val currentInteractive by rememberUpdatedState(interactive)
    val currentSliceCount by rememberUpdatedState(slices.size)
    val currentHighlightedIndex by rememberUpdatedState(highlightedIndex)
    val currentOnBoundaryCross by rememberUpdatedState(onBoundaryCross)
    val currentOnRelease by rememberUpdatedState(onRelease)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clearAndSetSemantics {}
            .wheelDrag(
                isInteractive = { currentInteractive },
                onFollow = { delta, timeSeconds ->
                    val before = rotation.value + dragRotation
                    dragRotation += delta.toFloat()
                    if (currentHighlightedIndex != null) displacedByDrag = true
                    val crossings = WheelDrag.boundaryCrossings(before.toDouble(), (rotation.value + dragRotation).toDouble(), currentSliceCount)
                    if (crossings > 0) currentOnBoundaryCross(timeSeconds)
                },
                onRelease = { angularVelocity ->
                    val rotated = dragRotation
                    // 追従した角度を Animatable に取り込むのと 0 に戻すのを同じ更新に入れて、盤面を跳ねさせない
                    scope.launch {
                        rotation.snapTo(rotation.value + rotated)
                        dragRotation = 0f
                        currentOnRelease(angularVelocity, rotated.toDouble())
                    }
                },
            ),
        contentAlignment = Alignment.TopCenter,
    ) {
        WheelFace(
            slices = slices,
            rotation = { rotation.value + dragRotation },
            emphasis = emphasis,
            glowStyle = glowStyle,
            modifier = Modifier.fillMaxSize(),
        )
        WheelPointer(
            modifier = Modifier.offset {
                val bounce = DekiStopEmphasis.pointerBounceOffset.toPx() * emphasis.pointerBounce.value
                IntOffset(0, (bounce - pointerOverhang.toPx()).roundToInt())
            },
        )
    }
}

/**
 * 盤面のドラッグ。盤面の中で始まったドラッグは親の縦スクロールより優先する（タッチスロップを越えたら消費する）。
 * 位置は dp で `WheelDrag` / `FlickVelocity` に渡す（中心付近の不感帯を dp で決めているため）。
 *
 * @param onFollow 前の位置から今の位置までに指が中心のまわりを回った角度（度）と、その時刻（秒）
 * @param onRelease 指を離した（角速度）か、取り消された（null）
 */
private fun Modifier.wheelDrag(
    isInteractive: () -> Boolean,
    onFollow: (delta: Double, timeSeconds: Double) -> Unit,
    onRelease: (angularVelocity: Double?) -> Unit,
): Modifier = this.pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        // 演出中に触り始めたドラッグは、指を離すまで追従もフリックもしない（途中で演出が終わっても拾わない）
        val ignored = !isInteractive()
        val slopChange =
            awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() } ?: return@awaitEachGesture
        if (ignored) {
            drag(slopChange.id) { change -> change.consume() }
            return@awaitEachGesture
        }

        val center = Offset(size.width / 2f, size.height / 2f).pxToDp(density)
        val samples = FlickSampleBuffer()
        var previous = down.position.pxToDp(density)
        val follow = { change: PointerInputChange ->
            val position = change.position.pxToDp(density)
            val timeSeconds = change.uptimeMillis / 1_000.0
            val delta = WheelDrag.rotationDelta(previous, position, center)
            previous = position
            if (delta != 0.0) onFollow(delta, timeSeconds)
            samples.append(FlickSample(timeSeconds, position))
        }

        follow(slopChange)
        val released =
            drag(slopChange.id) { change ->
                follow(change)
                change.consume()
            }
        val angularVelocity =
            if (released) {
                FlickVelocity.angularVelocity(samples.samples, center)
            } else {
                null
            }
        onRelease(angularVelocity)
    }
}

/** px の位置を dp にする。 */
private fun Offset.pxToDp(density: Float): Offset = Offset(x / density, y / density)

/**
 * 停止の強調の状態。強調が解けていく間も `index` を残し、沈めた色と縁取りを戻しきるまで描く。
 * 画面を作り直したときは、止まったあとの姿（`settled`）から始める。
 */
private class StopEmphasis(
    initialIndex: Int?,
    settled: Boolean,
    reducedMotion: Boolean,
) {
    var index by mutableStateOf(initialIndex)

    /** 沈める・縁取り・光彩の濃さ（0..1）。 */
    val progress = Animatable(if (settled) 1f else 0f)

    /** 止まったスライスの倍率。 */
    val scale = Animatable(if (settled && !reducedMotion) DekiStopEmphasis.HOLD_SCALE else 1f)

    /** 針の沈み込み（0..1）。 */
    val pointerBounce = Animatable(0f)
}

@Composable
private fun rememberStopEmphasis(
    highlightedIndex: Int?,
    reducedMotion: Boolean,
): StopEmphasis {
    // 画面を作り直したときは、いまの強調をそのまま出す（止まった瞬間の動きは繰り返さない）
    val emphasis =
        remember {
            StopEmphasis(initialIndex = highlightedIndex, settled = highlightedIndex != null, reducedMotion = reducedMotion)
        }
    var firstRun by remember { mutableStateOf(true) }

    LaunchedEffect(highlightedIndex) {
        if (firstRun) {
            firstRun = false
            return@LaunchedEffect
        }

        if (highlightedIndex != null) {
            emphasis.index = highlightedIndex
            launch { emphasis.progress.animateTo(1f, DekiMotion.stopDim()) }
            if (reducedMotion) return@LaunchedEffect

            launch {
                emphasis.pointerBounce.animateTo(1f, DekiMotion.stopPulse())
                emphasis.pointerBounce.animateTo(0f, DekiMotion.stopSettle())
            }
            emphasis.scale.snapTo(1f)
            emphasis.scale.animateTo(DekiStopEmphasis.PULSE_SCALE, DekiMotion.stopPulse())
            emphasis.scale.animateTo(DekiStopEmphasis.HOLD_SCALE, DekiMotion.stopSettle())
            return@LaunchedEffect
        }

        // 強調が解けたら（結果が消えた）沈めた色を戻し、弾ませて元の大きさに戻す
        val shrink = launch { emphasis.scale.animateTo(1f, DekiMotion.stopSettle()) }
        emphasis.progress.animateTo(0f, DekiMotion.stopDim())
        shrink.join()
        emphasis.index = null
    }
    return emphasis
}

/** 盤面の円板（外周・スライス・ラベル・ハブ）と影。回転はレイヤーで掛け、回している間は描き直さない。 */
@Composable
private fun WheelFace(
    slices: List<WheelSliceUiModel>,
    rotation: () -> Float,
    emphasis: StopEmphasis,
    glowStyle: GlowStyleUiModel,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val wheelShadow = DekiTheme.colorScheme.wheelShadow

    BoxWithConstraints(modifier = modifier) {
        val side = minOf(maxWidth, maxHeight)
        val scale = side.value / WheelLabel.REFERENCE_DIAMETER.toFloat()
        val count = slices.size
        val labelLayouts =
            remember(slices, side, density) {
                val labelLimit = WheelLabel.limit(count, side.value.toDouble())
                val fontSize = with(density) { WheelLabel.fontSize(count, side.value.toDouble()).toFloat().dp.toSp() }
                val style = TextStyle(color = DekiFixedColors.onSlice, fontSize = fontSize, fontWeight = FontWeight.Bold)
                slices.map { slice -> textMeasurer.measure(WheelLabel.truncate(slice.label, labelLimit), style, softWrap = false) }
            }

        // 盤面の影。外周の縁（盤面の枠から縮めた円）の形で落とす
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(((SLICE_INSET - RIM_OUTSET) * scale).dp)
                .dropShadow(
                    shape = CircleShape,
                    shadow = Shadow(radius = 15.dp, color = wheelShadow, offset = DpOffset(0.dp, 10.dp)),
                ),
        )
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotation() },
        ) {
            val unit = size.minDimension / WheelLabel.REFERENCE_DIAMETER.toFloat()
            val geometry = WheelGeometry(center = center, radius = size.minDimension / 2 - SLICE_INSET * unit, unit = unit)
            drawRim(geometry)
            drawSlices(geometry, labelLayouts, emphasis, glowStyle)
            drawHub(geometry)
        }
    }
}

/** 盤面の中心・スライスの半径・基準直径での 1dp に当たる px。 */
private class WheelGeometry(
    val center: Offset,
    val radius: Float,
    val unit: Float,
)

private fun DrawScope.drawRim(geometry: WheelGeometry) {
    drawCircle(color = DekiFixedColors.wheelRim, radius = geometry.radius + RIM_OUTSET * geometry.unit, center = geometry.center)
    val edgeWidth = EDGE_WIDTH * geometry.unit
    // iOS の strokeBorder と同じく、線を円の内側に収める
    drawCircle(
        color = DekiFixedColors.wheelEdge,
        radius = geometry.radius + EDGE_OUTSET * geometry.unit - edgeWidth / 2,
        center = geometry.center,
        style = Stroke(width = edgeWidth),
    )
}

private fun DrawScope.drawSlices(
    geometry: WheelGeometry,
    labelLayouts: List<TextLayoutResult>,
    emphasis: StopEmphasis,
    glowStyle: GlowStyleUiModel,
) {
    val count = labelLayouts.size
    if (count == 0) {
        drawCircle(color = DekiFixedColors.wheelRim, radius = geometry.radius, center = geometry.center)
        return
    }

    val emphasizedIndex = emphasis.index
    val progress = emphasis.progress.value
    if (count == 1) {
        drawSingleSlice(geometry, labelLayouts.first(), emphasized = emphasizedIndex == 0, progress, glowStyle)
        return
    }

    val sliceAngle = 360f / count
    // 止まったスライスは隣に隠れないよう最後に描く
    for (index in 0 until count) {
        if (index == emphasizedIndex) continue

        val path = slicePath(geometry, index * sliceAngle, sliceAngle)
        drawSlice(geometry, path, index, count, sliceAngle, labelLayouts[index])
        // 止まったスライス以外に地色を薄く重ねて沈める
        if (emphasizedIndex != null) {
            drawPath(path, color = DekiFixedColors.sliceDim.copy(alpha = DekiFixedColors.sliceDim.alpha * progress))
        }
    }

    if (emphasizedIndex == null || emphasizedIndex !in 0 until count) return

    val path = slicePath(geometry, emphasizedIndex * sliceAngle, sliceAngle)
    val emphasizedScale = emphasis.scale.value
    scale(emphasizedScale, emphasizedScale, pivot = geometry.center) {
        // 光彩は前に出したスライスの後ろに敷き、沈めた隣に重ねる
        drawStopGlow(geometry, path, sliceColor(emphasizedIndex, count), glowStyle, progress)
        drawSlice(geometry, path, emphasizedIndex, count, sliceAngle, labelLayouts[emphasizedIndex])
        drawPath(
            path,
            color = DekiFixedColors.stopOutline.copy(alpha = progress),
            style = Stroke(width = DekiStopEmphasis.OUTLINE_WIDTH * geometry.unit, join = StrokeJoin.Round),
        )
    }
}

/** 項目が 1 件だけのとき。円全体を 1 色で塗り、ラベルは中央に横向きで置く。 */
private fun DrawScope.drawSingleSlice(
    geometry: WheelGeometry,
    labelLayout: TextLayoutResult,
    emphasized: Boolean,
    progress: Float,
    glowStyle: GlowStyleUiModel,
) {
    val color = sliceColor(0, 1)
    val path = Path().apply { addOval(Rect(geometry.center, geometry.radius)) }
    val shownProgress =
        if (emphasized) {
            progress
        } else {
            0f
        }
    drawStopGlow(geometry, path, color, glowStyle, shownProgress)
    drawPath(path, color = color)
    val outlineWidth = DekiStopEmphasis.OUTLINE_WIDTH * geometry.unit
    drawCircle(
        color = DekiFixedColors.stopOutline.copy(alpha = shownProgress),
        radius = geometry.radius - outlineWidth / 2,
        center = geometry.center,
        style = Stroke(width = outlineWidth),
    )
    drawLabel(labelLayout, geometry.center)
}

/** スライスの塗り・区切り線・ラベル。ラベルは半径方向に沿わせる。 */
private fun DrawScope.drawSlice(
    geometry: WheelGeometry,
    path: Path,
    index: Int,
    count: Int,
    sliceAngle: Float,
    labelLayout: TextLayoutResult,
) {
    drawPath(path, color = sliceColor(index, count))
    drawPath(path, color = DekiFixedColors.onSlice, style = Stroke(width = SEPARATOR_WIDTH * geometry.unit))
    val midAngle = index * sliceAngle + sliceAngle / 2
    val labelPoint = polar(geometry.center, midAngle, geometry.radius * WheelLabel.RADIUS_FRACTION.toFloat())
    rotate(WheelLabel.rotation(midAngle.toDouble()).toFloat(), pivot = labelPoint) {
        drawLabel(labelLayout, labelPoint)
    }
}

private fun DrawScope.drawLabel(
    labelLayout: TextLayoutResult,
    center: Offset,
) {
    val topLeft = Offset(center.x - labelLayout.size.width / 2f, center.y - labelLayout.size.height / 2f)
    drawText(labelLayout, topLeft = topLeft)
}

/**
 * 止まったスライスの外側への光彩。白系と止まったスライスの色は、形をぼかして後ろに敷く。
 * 虹色は、スライスの輪郭を盤面の中心まわりのグラデーションで太くなぞってぼかす（スライスの塗りは不透明なので、外側にはみ出した分だけが見える）。
 */
private fun DrawScope.drawStopGlow(
    geometry: WheelGeometry,
    path: Path,
    sliceColor: Color,
    glowStyle: GlowStyleUiModel,
    progress: Float,
) {
    if (progress <= 0f) return

    val glowRadius = DekiStopEmphasis.GLOW_RADIUS * geometry.unit
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        if (glowStyle == GlowStyleUiModel.RAINBOW) {
            paint.style = android.graphics.Paint.Style.STROKE
            paint.strokeWidth = glowRadius
            paint.strokeJoin = android.graphics.Paint.Join.ROUND
            paint.maskFilter = BlurMaskFilter(glowRadius / 2, BlurMaskFilter.Blur.NORMAL)
            paint.shader = rainbowShader(geometry.center)
            paint.alpha = (255 * progress).roundToInt()
        } else {
            paint.style = android.graphics.Paint.Style.FILL
            paint.maskFilter = BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.NORMAL)
            paint.color = glowColor(glowStyle, sliceColor).let { color -> color.copy(alpha = color.alpha * progress) }.toArgb()
        }
        canvas.nativeCanvas.drawPath(path.asAndroidPath(), paint)
    }
}

private fun glowColor(
    glowStyle: GlowStyleUiModel,
    sliceColor: Color,
): Color {
    if (glowStyle == GlowStyleUiModel.SLICE) return sliceColor.copy(alpha = DekiFixedColors.STOP_GLOW_ALPHA)

    return DekiFixedColors.stopGlow
}

/** 虹色。`SweepGradient` は 3 時から始まるので、12 時から時計回りに一周するよう 90 度戻す。 */
private fun rainbowShader(center: Offset): SweepGradient {
    val colors = DekiFixedColors.stopGlowRainbow.map { color -> color.toArgb() }.toIntArray()
    return SweepGradient(center.x, center.y, colors, null).apply {
        setLocalMatrix(Matrix().apply { setRotate(-90f, center.x, center.y) })
    }
}

private fun DrawScope.drawHub(geometry: WheelGeometry) {
    val hubRadius = HUB_DIAMETER / 2 * geometry.unit
    val borderWidth = HUB_BORDER_WIDTH * geometry.unit
    drawCircle(color = DekiFixedColors.wheelHub, radius = hubRadius, center = geometry.center)
    // iOS の strokeBorder と同じく、線を円の内側に収める
    drawCircle(
        color = DekiFixedColors.wheelHubMark,
        radius = hubRadius - borderWidth / 2,
        center = geometry.center,
        style = Stroke(width = borderWidth),
    )
    drawCircle(color = DekiFixedColors.wheelHubMark, radius = HUB_DOT_DIAMETER / 2 * geometry.unit, center = geometry.center)
}

/** 12 時を 0 度とする時計回りの扇形。 */
private fun slicePath(
    geometry: WheelGeometry,
    startAngle: Float,
    sweepAngle: Float,
): Path = Path().apply {
    moveTo(geometry.center.x, geometry.center.y)
    // Compose の角度は 3 時が 0 度で時計回りなので、12 時を 0 度にするため 90 度戻す
    arcTo(Rect(geometry.center, geometry.radius), startAngle - 90f, sweepAngle, forceMoveTo = false)
    close()
}

/** 12 時を 0 度とする時計回りの角度 `angle` の方向に、中心から `distance` 離れた点。 */
private fun polar(
    center: Offset,
    angle: Float,
    distance: Float,
): Offset {
    val radians = (angle - 90f) * PI.toFloat() / 180f
    return Offset(center.x + distance * cos(radians), center.y + distance * sin(radians))
}

private fun SpinEasingUiModel.toEasing(): CubicBezierEasing = CubicBezierEasing(x1, y1, x2, y2)

@Preview(widthDp = 360, heightDp = 360)
@Composable
private fun RouletteWheelPreview() {
    DekiTheme {
        RouletteWheel(
            slices = listOf("ラーメン", "カレー", "寿司", "焼肉").map { label -> WheelSliceUiModel(UUID.randomUUID(), label) },
            spin = SpinAnimationUiModel(
                spinCount = 0,
                targetRotation = 30f,
                easing = SpinEasingUiModel(0.15f, 0.85f, 0.3f, 1f),
                durationMillis = 4_500,
            ),
            spinning = false,
            interactive = true,
            highlightedIndex = 1,
            glowStyle = GlowStyleUiModel.WHITE,
            reducedMotion = false,
            onSpinFinished = {},
            onBoundaryCross = {},
            onRelease = { _, _ -> },
            modifier = Modifier.padding(20.dp),
        )
    }
}
