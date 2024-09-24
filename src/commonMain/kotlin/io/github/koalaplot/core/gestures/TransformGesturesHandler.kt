package io.github.koalaplot.core.gestures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.unit.IntSize

/**
 * Interface for handling touch-based transformation gestures, such as zoom and pan
 */
internal interface TransformGesturesHandler {

    suspend fun detectTransformGestures(
        scope: PointerInputScope,
        panLock: Boolean,
        zoomLock: Boolean,
        onZoomChange: (size: IntSize, centroid: Offset, zoomX: Float, zoomY: Float) -> Unit,
        onPanChange: (size: IntSize, pan: Offset) -> Unit,
    )
}
