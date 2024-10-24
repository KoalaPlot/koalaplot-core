package io.github.koalaplot.core.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import io.github.koalaplot.core.util.ZoomFactor
import io.github.koalaplot.core.util.max
import kotlin.math.abs

/**
 * Standard implementation of gesture processing, zoom occurs only vertically or only horizontally
 */
internal class DefaultTransformGesturesHandler : TransformGesturesHandler {

    override suspend fun detectTransformGestures(
        scope: PointerInputScope,
        panLock: Boolean,
        zoomLock: Boolean,
        onZoomChange: (size: IntSize, centroid: Offset, zoomX: Float, zoomY: Float) -> Unit,
        onPanChange: (size: IntSize, pan: Offset) -> Unit,
    ) = scope.awaitEachGesture {
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        val minTouchesDistance = viewConfiguration.minimumTouchTargetSize.width.toPx()

        var (zoomX, zoomY) = ZoomFactor.Neutral
        var isHorizontalZoom = false
        var isZoomDirectionSet = false

        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.fastAny { it.isConsumed }
            if (!canceled) {
                var zoomXYChange = event.calculateZoomXY()
                val panChange = event.calculatePan()

                if (!isZoomDirectionSet) {
                    event.isHorizontalZoom()?.also {
                        isHorizontalZoom = it
                        isZoomDirectionSet = true
                    }
                } else {
                    zoomXYChange = zoomXYChange.resetOrthogonalAxis(isHorizontalZoom)
                }

                if (!pastTouchSlop) {
                    zoomX *= zoomXYChange.x
                    zoomY *= zoomXYChange.y
                    pan += panChange

                    val (centroidSizeX, centroidSizeY) = event.calculateCentroidSizeXY(useCurrent = false)
                    val zoomMotion = max(
                        calculateZoomMotion(zoomX, centroidSizeX),
                        calculateZoomMotion(zoomY, centroidSizeY),
                    )

                    val panMotion = pan.getDistance()

                    if (zoomMotion > touchSlop || panMotion > touchSlop) {
                        pastTouchSlop = true
                    }
                } else {
                    if (!zoomLock && zoomXYChange != ZoomFactor.Neutral &&
                        event.zoomGestureIsCorrect(minTouchesDistance, isHorizontalZoom)
                    ) {
                        val centroid = event.calculateCentroid(useCurrent = false)
                        onZoomChange(size, centroid, zoomXYChange.x, zoomXYChange.y)
                    }
                    if (!panLock && panChange != Offset.Zero) {
                        onPanChange(size, panChange)
                    }
                    event.consumeChangedPositions()
                }
            }
        } while (!canceled && event.changes.fastAny { it.pressed })
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

    /**
     * Returns `true` if the zoom changes along the X axis, `false` if the zoom changes along the Y axis,
     * `null` if there are less than 2 touches(it is impossible to determine the zoom direction)
     */
    private fun PointerEvent.isHorizontalZoom(): Boolean? {
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
        if (firstTouch == null || secondTouch == null) return null
        val touchDiff = firstTouch - secondTouch
        return abs(touchDiff.x) > abs(touchDiff.y)
    }
}
