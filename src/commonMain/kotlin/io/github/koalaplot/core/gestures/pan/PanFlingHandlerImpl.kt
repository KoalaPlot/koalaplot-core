package io.github.koalaplot.core.gestures.pan

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize
import io.github.koalaplot.core.animation.PanFlingBehavior

/**
 * Implementation of [PanFlingHandler] that processes pan fling gestures
 * using a specified [PanFlingBehavior]
 *
 * @property panFlingBehavior The behavior responsible for handling fling physics and motion
 */
internal class PanFlingHandlerImpl(
    private val panFlingBehavior: PanFlingBehavior,
) : PanFlingHandler {

    override suspend fun perform(
        size: IntSize,
        velocityTracker: VelocityTracker,
        onPanChange: (size: IntSize, pan: Offset) -> Boolean
    ) {
        val velocity = velocityTracker.calculateVelocity()
        panFlingBehavior.performFling(velocity) { pan ->
            onPanChange(size, pan)
        }
    }
}
