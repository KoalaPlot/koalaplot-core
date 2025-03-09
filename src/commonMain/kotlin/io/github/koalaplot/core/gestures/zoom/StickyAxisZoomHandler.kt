package io.github.koalaplot.core.gestures.zoom

import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach
import io.github.koalaplot.core.gestures.GestureConfig
import io.github.koalaplot.core.gestures.applyZoomLocks
import io.github.koalaplot.core.gestures.getDistanceX
import io.github.koalaplot.core.gestures.getDistanceY
import io.github.koalaplot.core.util.ZoomFactor

/**
 * Zoom gesture handler that prioritizes a dominant zoom axis while restricting scaling on the orthogonal axis
 *
 * This implementation ensures that zooming primarily affects the axis where the gesture was initiated,
 * preventing unintended scaling along the perpendicular axis. The zoom factor is calculated separately
 * for the X and Y axes, with restrictions applied based on gesture configuration and touch conditions
 *
 * The handler also validates the distance between touch points to prevent extreme zoom factors
 * when the touch points are too close together
 *
 * @param minTouchesDistance The minimum allowed distance between touch points to consider the zoom gesture valid
 */
internal class StickyAxisZoomHandler(
    private val minTouchesDistance: Float,
) : ZoomHandler {

    override fun handle(
        size: IntSize,
        event: PointerEvent,
        isHorizontalZoom: Boolean,
        gestureConfig: GestureConfig,
        onZoomChange: (size: IntSize, centroid: Offset, zoom: ZoomFactor) -> Unit
    ): Boolean {
        var zoomChange = event
            .calculateZoomXY()
            .applyZoomLocks(gestureConfig.zoomXEnabled, gestureConfig.zoomYEnabled)

        zoomChange = zoomChange.resetOrthogonalAxis(isHorizontalZoom)

        val zoomAllowed = gestureConfig.zoomEnabled && zoomChange != ZoomFactor.Neutral &&
            event.zoomGestureIsCorrect(minTouchesDistance, isHorizontalZoom)

        if (!zoomAllowed) return false

        val centroid = event.calculateCentroid(useCurrent = false)
        onZoomChange(size, centroid, zoomChange)
        return true
    }

    private fun PointerEvent.calculateZoomXY(): ZoomFactor {
        val currentCentroidSize = calculateCentroidSizeXY(useCurrent = true)
        val previousCentroidSize = calculateCentroidSizeXY(useCurrent = false)
        if (currentCentroidSize == Offset.Zero || previousCentroidSize == Offset.Zero) {
            return ZoomFactor.Neutral
        }
        return ZoomFactor(
            x = currentCentroidSize.x / previousCentroidSize.x,
            y = currentCentroidSize.y / previousCentroidSize.y,
        )
    }

    private fun PointerEvent.calculateCentroidSizeXY(useCurrent: Boolean = true): Offset {
        val centroid = calculateCentroid(useCurrent)
        if (centroid == Offset.Unspecified) {
            return Offset.Zero
        }

        var distanceToCentroidX = 0f
        var distanceToCentroidY = 0f
        var distanceWeight = 0
        changes.fastForEach { change ->
            if (change.pressed && change.previousPressed) {
                val position = if (useCurrent) change.position else change.previousPosition
                val offsetToCentroid = position - centroid
                distanceToCentroidX += offsetToCentroid.getDistanceX()
                distanceToCentroidY += offsetToCentroid.getDistanceY()
                distanceWeight++
            }
        }
        return Offset(distanceToCentroidX, distanceToCentroidY) / distanceWeight.toFloat()
    }

    private fun ZoomFactor.resetOrthogonalAxis(isHorizontalZoom: Boolean) = this.copy(
        x = if (isHorizontalZoom) x else ZoomFactor.NeutralPoint,
        y = if (!isHorizontalZoom) y else ZoomFactor.NeutralPoint,
    )

    /**
     * Returns the result of checking the distance between touches for correctness in order
     * to prevent getting large zoom coefficient
     * @param minTouchesDistance Minimum allowed distance between touches
     * @param isHorizontalZoom For which axis the zooming is change
     */
    private fun PointerEvent.zoomGestureIsCorrect(minTouchesDistance: Float, isHorizontalZoom: Boolean): Boolean {
        var firstTouch: Offset? = null
        var secondTouch: Offset? = null
        for (change in changes) {
            if (change.pressed && change.previousPressed) {
                if (firstTouch == null) {
                    firstTouch = change.position
                } else {
                    secondTouch = change.position
                    break
                }
            }
        }
        if (firstTouch == null || secondTouch == null) return false
        val touchDiff = firstTouch - secondTouch
        return if (isHorizontalZoom) {
            touchDiff.getDistanceX() > minTouchesDistance
        } else {
            touchDiff.getDistanceY() > minTouchesDistance
        }
    }
}
