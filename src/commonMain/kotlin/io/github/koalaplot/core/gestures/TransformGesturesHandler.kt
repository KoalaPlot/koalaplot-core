package io.github.koalaplot.core.gestures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.unit.IntSize
import io.github.koalaplot.core.util.ZoomFactor

/**
 * Interface for handling touch-based transformation gestures, such as zoom and pan
 */
internal interface TransformGesturesHandler {

    suspend fun detectTransformGestures(
        scope: PointerInputScope,
        gestureConfig: GestureConfig,
        onZoomChange: (size: IntSize, centroid: Offset, zoom: ZoomFactor) -> Unit,
        onPanChange: (size: IntSize, pan: Offset) -> Boolean,
    )
}
