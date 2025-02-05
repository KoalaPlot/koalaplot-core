package io.github.koalaplot.core.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import io.github.koalaplot.core.gestures.DefaultTransformGesturesHandler
import io.github.koalaplot.core.gestures.GestureConfig
import io.github.koalaplot.core.gestures.TransformGesturesHandlerWithLockZoomRatio

/**
 * See the documentation of the expected fun
 */
@Suppress("LongParameterList") // expected
internal actual fun Modifier.onGestureInput(
    key1: Any?,
    key2: Any?,
    gestureConfig: GestureConfig,
    onZoomChange: (size: IntSize, centroid: Offset, zoom: ZoomFactor) -> Unit,
    onPanChange: (size: IntSize, pan: Offset) -> Boolean,
): Modifier = this then Modifier.pointerInput(key1, key2, gestureConfig) {
    val gesturesHandler = if (gestureConfig.independentZoomEnabled) {
        DefaultTransformGesturesHandler()
    } else {
        TransformGesturesHandlerWithLockZoomRatio()
    }
    gesturesHandler.detectTransformGestures(
        scope = this,
        gestureConfig = gestureConfig,
        onZoomChange = onZoomChange,
        onPanChange = onPanChange
    )
}
