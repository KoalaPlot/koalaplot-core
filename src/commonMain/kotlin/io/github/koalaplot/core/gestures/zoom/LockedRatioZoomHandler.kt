package io.github.koalaplot.core.gestures.zoom

import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.unit.IntSize
import io.github.koalaplot.core.gestures.GestureConfig
import io.github.koalaplot.core.util.ZoomFactor

/**
 * Zoom gesture handler that enforces a uniform zoom scale across both axes
 *
 * This implementation ensures that zooming affects both the X and Y axes equally,
 * preventing independent scaling on each axis. The zoom factor is calculated as the ratio
 * of the previous and current distances between touch points
 *
 * If zooming is disabled for either axis (`zoomXEnabled` or `zoomYEnabled` in [GestureConfig]),
 * the zoom change is ignored
 */
internal class LockedRatioZoomHandler : ZoomHandler {

    override fun handle(
        size: IntSize,
        event: PointerEvent,
        isHorizontalZoom: Boolean,
        gestureConfig: GestureConfig,
        onZoomChange: (size: IntSize, centroid: Offset, zoom: ZoomFactor) -> Unit
    ): Boolean {
        val zoomChange = if (!gestureConfig.zoomXEnabled || !gestureConfig.zoomYEnabled) {
            ZoomFactor.NeutralPoint
        } else {
            event.calculateZoom()
        }

        val zoom = ZoomFactor(zoomChange, zoomChange)
        val zoomAllowed = gestureConfig.zoomEnabled && zoom != ZoomFactor.Neutral

        if (!zoomAllowed) return false

        val centroid = event.calculateCentroid(useCurrent = false)
        onZoomChange(size, centroid, ZoomFactor(zoomChange, zoomChange))
        return true
    }
}
