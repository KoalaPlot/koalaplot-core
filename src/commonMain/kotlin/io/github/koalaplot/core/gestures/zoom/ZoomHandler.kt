package io.github.koalaplot.core.gestures.zoom

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.unit.IntSize
import io.github.koalaplot.core.gestures.GestureConfig
import io.github.koalaplot.core.util.ZoomFactor

/**
 * Interface for handling zoom gestures
 */
internal interface ZoomHandler {

    /**
     * Handles a zoom gesture event
     *
     * @param size The measured size of the pointer input region. Input events will be reported with
     * a coordinate space of (0, 0) to (size.width, size,height) as the input region, with
     * (0, 0) indicating the upper left corner
     * @param event The pointer event associated with the zoom gesture
     * @param isHorizontalZoom A flag indicating the axis on which the zoom was initiated.
     * `true` for horizontal zoom, `false` for vertical zoom
     * @param gestureConfig Configuration for gesture handling. See [GestureConfig]
     * @param onZoomChange A callback function that receives the updated zoom factor.
     * It provides the component size, the centroid of the zoom gesture, and the zoom scale factor
     * @return `true` if the zoom gesture was handled, `false` otherwise.
     */
    fun handle(
        size: IntSize,
        event: PointerEvent,
        isHorizontalZoom: Boolean,
        gestureConfig: GestureConfig,
        onZoomChange: (size: IntSize, centroid: Offset, zoom: ZoomFactor) -> Unit,
    ): Boolean
}
