package io.github.koalaplot.core.gestures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.unit.IntSize
import io.github.koalaplot.core.util.ZoomFactor

/**
 * Interface for handling multi-touch transform gestures, including zooming and panning
 *
 * This interface defines a method for detecting and processing gestures that involve zoom
 * and translation (pan). Implementations should analyze touch input and invoke the appropriate
 * callbacks when zoom or pan changes occur
 */
internal interface TransformGesturesHandler {

    /**
     * Detects and processes transform gestures, such as zoom and pan
     *
     * @param scope The [PointerInputScope] within which gesture detection occurs
     * @param gestureConfig Configuration for gesture handling. See [GestureConfig]
     * @param onZoomChange A callback function invoked when a zoom change is detected.
     * It provides the component size, the centroid of the zoom gesture, and the zoom factor
     * @param onPanChange A callback function invoked when a pan change is detected.
     * It provides the component size and the pan offset.
     * Returns `true` if the pan change was consumed, `false` otherwise
     */
    suspend fun detectTransformGestures(
        scope: PointerInputScope,
        gestureConfig: GestureConfig,
        onZoomChange: (size: IntSize, centroid: Offset, zoom: ZoomFactor) -> Unit,
        onPanChange: (size: IntSize, pan: Offset) -> Boolean,
    )
}
