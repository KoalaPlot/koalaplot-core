package io.github.koalaplot.core.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
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
        gestureConfig: GestureConfig,
        onZoomChange: (size: IntSize, centroid: Offset, zoom: ZoomFactor) -> Unit,
        onPanChange: (size: IntSize, pan: Offset) -> Boolean,
    ) = scope.awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.fastAny { it.isConsumed }
            if (!canceled) {
                val zoomChange = if (!gestureConfig.zoomXEnabled || !gestureConfig.zoomYEnabled) {
                    ZoomFactor.NeutralPoint
                } else {
                    event.calculateZoom()
                }
                val panChange = event
                    .calculatePan()
                    .applyPanLocks(gestureConfig.panXEnabled, gestureConfig.panYEnabled)

                val zoom = ZoomFactor(zoomChange, zoomChange)
                val zoomAllowed = gestureConfig.zoomEnabled && zoom != ZoomFactor.Neutral

                var allowConsumption = false

                if (zoomAllowed) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    onZoomChange(size, centroid, zoom)
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
}
