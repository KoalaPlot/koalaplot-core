package io.github.koalaplot.core.util

import androidx.compose.animation.SplineBasedFloatDecayAnimationSpec
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import io.github.koalaplot.core.animation.PanFlingBehavior
import io.github.koalaplot.core.gestures.GestureConfig
import io.github.koalaplot.core.gestures.TransformGesturesHandler
import io.github.koalaplot.core.gestures.TransformGesturesHandlerImpl
import io.github.koalaplot.core.gestures.pan.PanFlingHandlerImpl
import io.github.koalaplot.core.gestures.pan.PanHandlerImpl
import io.github.koalaplot.core.gestures.zoom.LockedRatioZoomHandler
import io.github.koalaplot.core.gestures.zoom.StickyAxisZoomHandler

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
    val minTouchesDistance = viewConfiguration.minimumTouchTargetSize.width.toPx()

    val gesturesHandler: TransformGesturesHandler = TransformGesturesHandlerImpl(
        panHandler = PanHandlerImpl(),
        panFlingHandler = PanFlingHandlerImpl(
            panFlingBehavior = PanFlingBehavior(
                animationSpec = SplineBasedFloatDecayAnimationSpec(density = this),
            )
        ),
        zoomHandler = if (gestureConfig.independentZoomEnabled) {
            StickyAxisZoomHandler(minTouchesDistance)
        } else {
            LockedRatioZoomHandler()
        }
    )
    gesturesHandler.detectTransformGestures(
        scope = this,
        gestureConfig = gestureConfig,
        onZoomChange = onZoomChange,
        onPanChange = onPanChange,
    )
}
