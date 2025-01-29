package io.github.koalaplot.core.gestures.pan

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize

/**
 * Interface for handling pan fling gestures
 */
internal interface PanFlingHandler {

    /**
     * Processes a pan fling gesture with the given parameters
     *
     * @param size The measured size of the pointer input region. Input events will be reported with
     * a coordinate space of (0, 0) to (size.width, size,height) as the input region, with
     * (0, 0) indicating the upper left corner
     * @param velocityTracker The velocity tracker used to calculate fling velocity
     * @param onPanChange A callback function that receives the updated pan offset.
     * Returns `true` if the change was consumed, `false` otherwise
     */
    suspend fun perform(
        size: IntSize,
        velocityTracker: VelocityTracker,
        onPanChange: (size: IntSize, pan: Offset) -> Boolean,
    )
}
