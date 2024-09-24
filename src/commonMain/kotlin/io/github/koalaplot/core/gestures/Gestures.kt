package io.github.koalaplot.core.gestures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.util.fastForEach
import io.github.koalaplot.core.util.ZoomFactor
import kotlin.math.abs

internal fun PointerEvent.consumeChangedPositions() {
    changes.fastForEach { change ->
        if (change.positionChanged()) change.consume()
    }
}

internal fun Offset.getDistanceX(): Float = abs(x)

internal fun Offset.getDistanceY(): Float = abs(y)

/**
 * Returns the largest zoom value of the two axes
 * @param zoom The current zoom value on the X or Y axes
 * @param size The width of the area for the [zoom] axis
 */
internal fun calculateZoomMotion(zoom: Float, size: Float): Float = abs(ZoomFactor.NeutralPoint - zoom) * size

/**
 * Returns the largest zoom value of the two axes
 * @param zoomX The current zoom value on the X axis
 * @param zoomY The current zoom value on the Y axis
 */
internal fun getMaxZoomDeviation(zoomX: Float, zoomY: Float): Float {
    val deviationX = abs(zoomX - ZoomFactor.NeutralPoint)
    val deviationY = abs(zoomY - ZoomFactor.NeutralPoint)
    return if (deviationX > deviationY) zoomX else zoomY
}
