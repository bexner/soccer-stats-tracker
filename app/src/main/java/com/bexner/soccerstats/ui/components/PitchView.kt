package com.bexner.soccerstats.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** A marker drawn on the pitch. Coordinates are normalized 0f..1f. */
data class PitchMarker(
    val id: Long,
    val x: Float,
    val y: Float,
    val label: String,
    val color: Color
)

private val PitchGrass = Color(0xFF3A7D44)
private val PitchLine = Color(0x99FFFFFF)

/**
 * Top-down pitch with markers laid over it. Your goal is at the bottom (y = 1f),
 * the opponent's at the top (y = 0f), matching how a lineup gets sketched.
 *
 * Passing [onMarkerMoved] makes the markers draggable, and [onPitchTapped]
 * reports taps on empty grass. Positions come back normalized and clamped, so
 * callers never deal in pixels. Lineups and the live game screen reuse this
 * same component.
 */
@Composable
fun PitchView(
    markers: List<PitchMarker>,
    modifier: Modifier = Modifier,
    markerSize: Dp = 44.dp,
    selectedMarkerId: Long? = null,
    onMarkerMoved: ((id: Long, x: Float, y: Float) -> Unit)? = null,
    onMarkerTapped: ((id: Long) -> Unit)? = null,
    onPitchTapped: ((x: Float, y: Float) -> Unit)? = null
) {
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(PitchGrass)
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val markerPx = with(density) { markerSize.toPx() }
        val currentOnPitchTapped by rememberUpdatedState(onPitchTapped)

        // Registered before the markers so marker taps, which sit above this in
        // the layout, still win where they overlap.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(onPitchTapped == null, widthPx, heightPx) {
                    if (currentOnPitchTapped == null) return@pointerInput
                    detectTapGestures { offset ->
                        if (widthPx <= 0f || heightPx <= 0f) return@detectTapGestures
                        currentOnPitchTapped?.invoke(
                            (offset.x / widthPx).coerceIn(0f, 1f),
                            (offset.y / heightPx).coerceIn(0f, 1f)
                        )
                    }
                }
        )

        PitchMarkings()

        markers.forEach { marker ->
            key(marker.id) {
                DraggableMarker(
                    marker = marker,
                    isSelected = marker.id == selectedMarkerId,
                    markerSize = markerSize,
                    markerPx = markerPx,
                    widthPx = widthPx,
                    heightPx = heightPx,
                    onMarkerMoved = onMarkerMoved,
                    onMarkerTapped = onMarkerTapped
                )
            }
        }
    }
}

@Composable
private fun DraggableMarker(
    marker: PitchMarker,
    isSelected: Boolean,
    markerSize: Dp,
    markerPx: Float,
    widthPx: Float,
    heightPx: Float,
    onMarkerMoved: ((id: Long, x: Float, y: Float) -> Unit)?,
    onMarkerTapped: ((id: Long) -> Unit)?
) {
    // Tracked in pixels while dragging so movement is smooth, normalized on drop.
    var dragOffset by remember(marker.id) { mutableStateOf<Offset?>(null) }

    // Keeps the newest callbacks reachable without making them pointerInput keys —
    // a lambda is a fresh instance each recomposition and would restart the gesture
    // detector constantly.
    val currentOnMoved by rememberUpdatedState(onMarkerMoved)
    val currentOnTapped by rememberUpdatedState(onMarkerTapped)

    val baseX = marker.x * widthPx - markerPx / 2f
    val baseY = marker.y * heightPx - markerPx / 2f
    val current = dragOffset ?: Offset(baseX, baseY)

    Box(
        modifier = Modifier
            .offset { IntOffset(current.x.roundToInt(), current.y.roundToInt()) }
            .size(markerSize)
            // Drag is registered before tap: the drag detector ignores events until
            // touch slop is exceeded, so a stationary press still reads as a tap.
            // Keyed on position too, so baseX/baseY are refreshed after each drop
            // instead of snapping back to where the marker first rendered.
            .pointerInput(marker.id, marker.x, marker.y) {
                if (currentOnMoved == null) return@pointerInput
                detectDragGestures(
                    onDragStart = { dragOffset = Offset(baseX, baseY) },
                    onDrag = { change, delta ->
                        change.consume()
                        val prev = dragOffset ?: Offset(baseX, baseY)
                        dragOffset = Offset(
                            (prev.x + delta.x).coerceIn(-markerPx / 2f, widthPx - markerPx / 2f),
                            (prev.y + delta.y).coerceIn(-markerPx / 2f, heightPx - markerPx / 2f)
                        )
                    },
                    onDragEnd = {
                        val end = dragOffset
                        if (end != null && widthPx > 0f && heightPx > 0f) {
                            val nx = ((end.x + markerPx / 2f) / widthPx).coerceIn(0.03f, 0.97f)
                            val ny = ((end.y + markerPx / 2f) / heightPx).coerceIn(0.03f, 0.97f)
                            currentOnMoved?.invoke(marker.id, nx, ny)
                        }
                        dragOffset = null
                    },
                    onDragCancel = { dragOffset = null }
                )
            }
            .pointerInput(marker.id) {
                if (currentOnTapped == null) return@pointerInput
                detectTapGestures(onTap = { currentOnTapped?.invoke(marker.id) })
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(marker.color)
                .then(
                    if (isSelected) Modifier.border(3.dp, Color.White, CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = marker.label,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

/** Touchlines, halfway line, centre circle, penalty areas and goal boxes. */
@Composable
private fun PitchMarkings() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val inset = 4.dp.toPx()
        val stroke = Stroke(width = 2.dp.toPx())

        drawRect(
            color = PitchLine,
            topLeft = Offset(inset, inset),
            size = Size(w - inset * 2, h - inset * 2),
            style = stroke
        )

        drawLine(
            color = PitchLine,
            start = Offset(inset, h / 2f),
            end = Offset(w - inset, h / 2f),
            strokeWidth = 2.dp.toPx()
        )
        drawCircle(
            color = PitchLine,
            radius = w * 0.13f,
            center = Offset(w / 2f, h / 2f),
            style = stroke
        )

        val boxW = w * 0.56f
        val boxH = h * 0.14f
        val sixW = w * 0.28f
        val sixH = h * 0.06f

        listOf(true, false).forEach { atTop ->
            drawRect(
                color = PitchLine,
                topLeft = Offset((w - boxW) / 2f, if (atTop) inset else h - inset - boxH),
                size = Size(boxW, boxH),
                style = stroke
            )
            drawRect(
                color = PitchLine,
                topLeft = Offset((w - sixW) / 2f, if (atTop) inset else h - inset - sixH),
                size = Size(sixW, sixH),
                style = stroke
            )
        }
    }
}
