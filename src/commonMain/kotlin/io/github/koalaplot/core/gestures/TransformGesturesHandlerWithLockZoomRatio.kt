package io.github.koalaplot.core.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastAny
import io.github.koalaplot.core.util.ZoomFactor

/**
 * Implementation of touch gesture processing with blocking of separate zoom changes on each axis
 *
 * The zoom factor is the same for each axis. The zoom value will be the ratio between the previous
 * and current distance between the touch points
 */
internal class TransformGesturesHandlerWithLockZoomRatio : TransformGesturesHandler {

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

        var zoomCombined = ZoomFactor.NeutralPoint

        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.fastAny { it.isConsumed }
            if (!canceled) {
                val zoomCombinedChange = event.calculateZoom()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoomCombined *= zoomCombinedChange
                    pan += panChange

                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = calculateZoomMotion(zoomCombined, centroidSize)
                    val panMotion = pan.getDistance()

                    if (zoomMotion > touchSlop || panMotion > touchSlop) {
                        pastTouchSlop = true
                    }
                } else {
                    if (!zoomLock && zoomCombinedChange != ZoomFactor.NeutralPoint) {
                        val centroid = event.calculateCentroid(useCurrent = false)
                        onZoomChange(size, centroid, zoomCombinedChange, zoomCombinedChange)
                    }
                    if (!panLock && panChange != Offset.Zero) {
                        onPanChange(size, panChange)
                    }
                    event.consumeChangedPositions()
                }
            }
        } while (!canceled && event.changes.fastAny { it.pressed })
    }
}
