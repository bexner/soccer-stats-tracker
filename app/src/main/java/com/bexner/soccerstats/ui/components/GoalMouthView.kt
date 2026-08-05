package com.bexner.soccerstats.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/** A shot placement drawn on the goal face, normalized 0f..1f. */
data class GoalMark(
    val x: Float,
    val y: Float,
    val color: Color
)

private val NetColor = Color(0x33FFFFFF)
private val FrameColor = Color(0xFFFFFFFF)
private val BackgroundColor = Color(0xFF2F6B38)
private val ZoneLine = Color(0x33FFFFFF)

// Fraction of the view given over to margin around the goal frame. Shared by the
// tap handler and the renderer so taps, drawn marks and zone guides all agree.
private const val POST_INSET_FRACTION = 0.06f
private const val BAR_INSET_FRACTION = 0.10f

/**
 * The goal seen from the shooter's side. Tapping anywhere records an exact
 * placement, so the coach never has to hit a small target mid-game; the nine-box
 * zone used by stats is derived from that point afterwards.
 *
 * Coordinates are normalized **against the goal frame, not the view** — x = 0f is
 * the left post, x = 1f the right post, y = 0f the crossbar, y = 1f the line. A
 * top-corner finish lands near (0.05, 0.1). Taps in the margin clamp to the frame,
 * which keeps the drawn thirds and [GameEvent.goalZone] in exact agreement.
 */
@Composable
fun GoalMouthView(
    modifier: Modifier = Modifier,
    marks: List<GoalMark> = emptyList(),
    onTap: ((x: Float, y: Float) -> Unit)? = null
) {
    BoxWithConstraints(modifier = modifier) {
        val viewW = constraints.maxWidth.toFloat()
        val viewH = constraints.maxHeight.toFloat()
        val postInset = viewW * POST_INSET_FRACTION
        val barInset = viewH * BAR_INSET_FRACTION
        val goalW = viewW - postInset * 2
        val goalH = viewH - barInset * 2

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(onTap == null, viewW, viewH) {
                    if (onTap == null) return@pointerInput
                    detectTapGestures { offset ->
                        if (goalW <= 0f || goalH <= 0f) return@detectTapGestures
                        onTap(
                            ((offset.x - postInset) / goalW).coerceIn(0f, 1f),
                            ((offset.y - barInset) / goalH).coerceIn(0f, 1f)
                        )
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val px = w * POST_INSET_FRACTION
                val by = h * BAR_INSET_FRACTION
                val gw = w - px * 2
                val gh = h - by * 2

                drawRect(color = BackgroundColor, size = Size(w, h))

                // Net mesh, inside the frame only.
                val cells = 10
                repeat(cells + 1) { i ->
                    val t = i / cells.toFloat()
                    drawLine(
                        color = NetColor,
                        start = Offset(px + gw * t, by),
                        end = Offset(px + gw * t, by + gh),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = NetColor,
                        start = Offset(px, by + gh * t),
                        end = Offset(px + gw, by + gh * t),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Nine-box guides at exactly the thresholds goalZone uses.
                listOf(1f / 3f, 2f / 3f).forEach { t ->
                    drawLine(
                        color = ZoneLine,
                        start = Offset(px + gw * t, by),
                        end = Offset(px + gw * t, by + gh),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawLine(
                        color = ZoneLine,
                        start = Offset(px, by + gh * t),
                        end = Offset(px + gw, by + gh * t),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // Posts and crossbar.
                drawRect(
                    color = FrameColor,
                    topLeft = Offset(px, by),
                    size = Size(gw, gh),
                    style = Stroke(width = 6.dp.toPx())
                )

                // Placements, mapped back through the same frame transform.
                marks.forEach { mark ->
                    val center = Offset(px + mark.x * gw, by + mark.y * gh)
                    drawCircle(color = mark.color, radius = 9.dp.toPx(), center = center)
                    drawCircle(
                        color = Color.White,
                        radius = 9.dp.toPx(),
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}
