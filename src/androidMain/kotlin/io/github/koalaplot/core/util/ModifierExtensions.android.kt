package io.github.koalaplot.core.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import io.github.koalaplot.core.gestures.DefaultTransformGesturesHandler
import io.github.koalaplot.core.gestures.TransformGesturesHandlerWithLockZoomRatio

/**
 * See the documentation of the expected fun
 */
@Suppress("LongParameterList") // expected
internal actual fun Modifier.onGestureInput(
    key1: Any?,
    key2: Any?,
    panLock: Boolean,
    zoomLock: Boolean,
    lockZoomRatio: Boolean,
    onZoomChange: (size: IntSize, centroid: Offset, zoomX: Float, zoomY: Float) -> Unit,
    onPanChange: (size: IntSize, pan: Offset) -> Unit,
): Modifier = this then Modifier.pointerInput(key1, key2, panLock, zoomLock, lockZoomRatio) {
    val gesturesHandler = if (lockZoomRatio) {
        TransformGesturesHandlerWithLockZoomRatio()
    } else {
        DefaultTransformGesturesHandler()
    }
    gesturesHandler.detectTransformGestures(
        scope = this,
        panLock = panLock,
        zoomLock = zoomLock,
        onZoomChange = onZoomChange,
        onPanChange = onPanChange
    )
}
