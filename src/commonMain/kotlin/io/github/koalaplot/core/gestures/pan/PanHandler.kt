package io.github.koalaplot.core.gestures.pan

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize
import io.github.koalaplot.core.gestures.GestureConfig

/**
 * Interface for handling pan gestures
 */
internal interface PanHandler {

    /**
     * Handles a pan gesture event
     *
     * @param size The measured size of the pointer input region. Input events will be reported with
     * a coordinate space of (0, 0) to (size.width, size,height) as the input region, with
     * (0, 0) indicating the upper left corner
     * @param event The pointer event associated with the pan gesture
     * @param gestureConfig Configuration for gesture handling. See [GestureConfig]
     * @param velocityTracker The velocity tracker used to calculate gesture velocity
     * @param onPanChange A callback function that receives the updated pan offset.
     * Returns `true` if the change was consumed, `false` otherwise
     * @return `true` if the gesture event was handled, `false` otherwise
     */
    fun handle(
        size: IntSize,
        event: PointerEvent,
        gestureConfig: GestureConfig,
        velocityTracker: VelocityTracker,
        onPanChange: (size: IntSize, pan: Offset) -> Boolean,
    ): Boolean
}
