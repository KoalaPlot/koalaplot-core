package io.github.koalaplot.core.util

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.koalaplot.core.gestures.GestureConfig
import io.github.koalaplot.core.gestures.applyPanLocks
import io.github.koalaplot.core.gestures.applyZoomLocks
import io.github.koalaplot.core.gestures.getMaxZoomDeviation

private const val ScrollDeltaMin = 0.75f
private const val ScrollDeltaMax = 1.25f

/**
 * See the documentation of the expected fun
 */
@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalKoalaPlotApi
@Suppress("LongParameterList") // expected
internal actual fun Modifier.onGestureInput(
    key1: Any?,
    key2: Any?,
    gestureConfig: GestureConfig,
    onZoomChange: (size: IntSize, centroid: Offset, zoom: ZoomFactor) -> Unit,
    onPanChange: (size: IntSize, pan: Offset) -> Boolean,
): Modifier = this then Modifier
    .onPointerEvent(PointerEventType.Move) { event ->
        if (!gestureConfig.panEnabled) return@onPointerEvent
        val change = event.changes.lastOrNull() ?: return@onPointerEvent

        val pan = (change.position - change.previousPosition)
            .applyPanLocks(gestureConfig.panXEnabled, gestureConfig.panYEnabled)

        if (pan == Offset.Zero) return@onPointerEvent
        if (onPanChange(size, pan)) change.consume()
    }
    .onPointerEvent(PointerEventType.Scroll) { event ->
        val change = event.changes.lastOrNull() ?: return@onPointerEvent
        val isZoomEvent = event.keyboardModifiers.isCtrlPressed

        if (isZoomEvent && gestureConfig.zoomEnabled) {
            val (scrollX, scrollY) = change.scrollDelta
            val rawZoom = if (!gestureConfig.independentZoomEnabled) {
                val maxZoomDeviation = getMaxZoomDeviation(
                    normalizeScrollDeltaToZoom(scrollX),
                    normalizeScrollDeltaToZoom(scrollY),
                )
                ZoomFactor(maxZoomDeviation, maxZoomDeviation)
            } else {
                ZoomFactor(normalizeScrollDeltaToZoom(scrollX), normalizeScrollDeltaToZoom(scrollY))
            }

            val zoom = rawZoom.applyZoomLocks(gestureConfig.zoomXEnabled, gestureConfig.zoomYEnabled)

            if (zoom == ZoomFactor.Neutral) return@onPointerEvent
            onZoomChange(size, change.position, zoom)
            change.consume()
        } else if (gestureConfig.panEnabled) {
            val rawPan = change.scrollDelta * -64.dp.toPx()
            val pan = rawPan.applyPanLocks(gestureConfig.panXEnabled, gestureConfig.panYEnabled)

            if (pan == Offset.Zero) return@onPointerEvent
            if (onPanChange(size, pan)) change.consume()
        }
    }

/**
 * Returns a normalized scroll value that can be used as a zoom value. In `Desktop`, `JS` and `wasmJS`,
 * the scroll value is considered from `0` and it must be normalized to `1` to get the zoom value.
 * The values are additionally stabilized in the range from [ScrollDeltaMin] to [ScrollDeltaMax],
 * stabilization of the value in the range is necessary only for scrolling with the mouse wheel,
 * since the resulting value can be either `-1` or `1`.
 * @param value Scroll Value
 */
internal fun normalizeScrollDeltaToZoom(value: Float): Float {
    return (ZoomFactor.NeutralPoint - value).coerceIn(ScrollDeltaMin..ScrollDeltaMax)
}
