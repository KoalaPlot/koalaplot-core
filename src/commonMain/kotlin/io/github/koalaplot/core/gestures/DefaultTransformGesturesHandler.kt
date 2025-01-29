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
import kotlin.math.abs

/**
 * Standard implementation of gesture processing, zoom occurs only vertically or only horizontally
 */
internal class DefaultTransformGesturesHandler : TransformGesturesHandler {

    override suspend fun detectTransformGestures(
        scope: PointerInputScope,
        gestureConfig: GestureConfig,
        onZoomChange: (size: IntSize, centroid: Offset, zoomX: ZoomFactor) -> Unit,
        onPanChange: (size: IntSize, pan: Offset) -> Boolean,
    ) = scope.awaitEachGesture {
        val minTouchesDistance = viewConfiguration.minimumTouchTargetSize.width.toPx()

        var isHorizontalZoom = false
        var isZoomDirectionSet = false

        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.fastAny { it.isConsumed }
            if (!canceled) {
                var zoomChange = event
                    .calculateZoomXY()
                    .applyZoomLocks(gestureConfig.zoomXEnabled, gestureConfig.zoomYEnabled)
                val panChange = event
                    .calculatePan()
                    .applyPanLocks(gestureConfig.panXEnabled, gestureConfig.panYEnabled)

                if (!isZoomDirectionSet) {
                    event.isHorizontalZoom()?.also {
                        isHorizontalZoom = it
                        isZoomDirectionSet = true
                    }
                } else {
                    zoomChange = zoomChange.resetOrthogonalAxis(isHorizontalZoom)
                }

                var allowConsumption = false

                val zoomAllowed = gestureConfig.zoomEnabled && zoomChange != ZoomFactor.Neutral &&
                    event.zoomGestureIsCorrect(minTouchesDistance, isHorizontalZoom)

                if (zoomAllowed) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    onZoomChange(size, centroid, zoomChange)
                    allowConsumption = true
                }

                val panAllowed = gestureConfig.panEnabled && panChange != Offset.Zero

                if (panAllowed) {
                    allowConsumption = allowConsumption || onPanChange(size, panChange)
                }
                if (allowConsumption) event.consumeChangedPositions()
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
