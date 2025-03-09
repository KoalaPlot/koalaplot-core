package io.github.koalaplot.core.gestures

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastForEach
import io.github.koalaplot.core.util.ZoomFactor
import kotlin.math.abs
import kotlin.math.sqrt

internal const val DefaultPanValue = 0f

internal fun PointerEvent.consumeChangedPositions() {
    changes.fastForEach { change ->
        if (change.positionChanged()) change.consume()
    }
}

internal fun Offset.getDistanceX(): Float = abs(x)

internal fun Offset.getDistanceY(): Float = abs(y)

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

internal fun Offset.applyPanLocks(panXEnabled: Boolean, panYEnabled: Boolean): Offset = this.copy(
    x = if (panXEnabled) this.x else DefaultPanValue,
    y = if (panYEnabled) this.y else DefaultPanValue,
)

internal fun ZoomFactor.applyZoomLocks(zoomXEnabled: Boolean, zoomYEnabled: Boolean): ZoomFactor = this.copy(
    x = if (zoomXEnabled) this.x else ZoomFactor.NeutralPoint,
    y = if (zoomYEnabled) this.y else ZoomFactor.NeutralPoint,
)

/**
 * The magnitude of the Velocity.
 */
@Stable
internal fun Velocity.getSpeed(): Float = sqrt(x * x + y * y)
