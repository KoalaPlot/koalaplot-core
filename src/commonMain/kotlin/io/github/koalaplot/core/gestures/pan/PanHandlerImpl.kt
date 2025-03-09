package io.github.koalaplot.core.gestures.pan

import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.unit.IntSize
import io.github.koalaplot.core.gestures.GestureConfig
import io.github.koalaplot.core.gestures.applyPanLocks

/**
 * Implementation of [PanHandler] that processes pan gestures
 * based on pointer events and gesture configurations
 */
internal class PanHandlerImpl : PanHandler {

    override fun handle(
        size: IntSize,
        event: PointerEvent,
        gestureConfig: GestureConfig,
        velocityTracker: VelocityTracker,
        onPanChange: (size: IntSize, pan: Offset) -> Boolean
    ): Boolean {
        val panChange = event
            .calculatePan()
            .applyPanLocks(gestureConfig.panXEnabled, gestureConfig.panYEnabled)

        val panAllowed = gestureConfig.panEnabled && panChange != Offset.Zero

        if (!panAllowed) return false

        // TODO Currently, the speed is calculated only if panning occurs with a single touch.
        //  Panning with two or more taps is not supported. The implementation needs to be changed when
        //  the implementation of recording the touch speed based on the motion vector, rather than
        //  the current position, is added to VelocityTracker
        //  For more information, see [VelocityTracker.addPosition]
        event.changes.singleOrNull()?.also(velocityTracker::addPointerInputChange)
        return onPanChange(size, panChange)
    }
}
