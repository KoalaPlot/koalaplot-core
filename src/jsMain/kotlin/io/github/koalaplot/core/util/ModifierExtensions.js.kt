package io.github.koalaplot.core.util

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.IntSize
import io.github.koalaplot.core.gestures.getMaxZoomDeviation

private const val ScrollDeltaMin = 0.75f
private const val ScrollDeltaMax = 1.25f
private const val DefaultScrollDelta = 1f
private const val JsScrollFactor = 100.0f

/**
 * See the documentation of the expected fun
 */
@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalKoalaPlotApi
@Suppress("LongParameterList") // expected
internal actual fun Modifier.onGestureInput(
    key1: Any?,
    key2: Any?,
    panLock: Boolean,
    zoomLock: Boolean,
    lockZoomRatio: Boolean,
    onZoomChange: (size: IntSize, centroid: Offset, zoomX: Float, zoomY: Float) -> Unit,
    onPanChange: (size: IntSize, pan: Offset) -> Unit,
): Modifier = this then Modifier
    .onPointerEvent(PointerEventType.Move) { event ->
        if (panLock) return@onPointerEvent
        val change = event.changes.lastOrNull() ?: return@onPointerEvent
        val result = change.position - change.previousPosition
        if (result == Offset.Zero) return@onPointerEvent
        onPanChange(size, result)
        change.consume()
    }
    .onPointerEvent(PointerEventType.Scroll) { event ->
        val change = event.changes.lastOrNull() ?: return@onPointerEvent
        if (!zoomLock) {
            val (scrollX, scrollY) = change.scrollDelta
            val (zoomX, zoomY) = if (lockZoomRatio) {
                val maxZoomDeviation = getMaxZoomDeviation(
                    normalizeScrollDeltaToZoom(scrollX),
                    normalizeScrollDeltaToZoom(scrollY),
                )
                maxZoomDeviation to maxZoomDeviation
            } else if (event.keyboardModifiers.isShiftPressed) {
                val maxZoomDeviation = getMaxZoomDeviation(
                    normalizeScrollDeltaToZoom(scrollX),
                    normalizeScrollDeltaToZoom(scrollY),
                )
                maxZoomDeviation to ZoomFactor.NeutralPoint
            } else {
                normalizeScrollDeltaToZoom(scrollX) to normalizeScrollDeltaToZoom(scrollY)
            }
            onZoomChange(size, change.position, zoomX, zoomY)
            change.consume()
        }
    }

/**
 * Returns a normalized scroll value that can be used as a zoom value. In `Desktop`, `JS` and `wasmJS`,
 * the scroll value is considered from `0` and it must be normalized to `1` to get the zoom value.
 * The values are additionally stabilized in the range from [ScrollDeltaMin] to [ScrollDeltaMax],
 * stabilization of the value in the range is necessary only for scrolling with the mouse wheel,
 * since the resulting value can be either `-1` or `1`. In `JS` and `wasmJS`, it is required to additionally
 * divide the value by `100`, because the values obtained are initially an integer type and are always equal to `N.0f`
 * @param value Scroll Value
 */
internal fun normalizeScrollDeltaToZoom(value: Float): Float {
    return (DefaultScrollDelta - value / JsScrollFactor).coerceIn(ScrollDeltaMin..ScrollDeltaMax)
}
