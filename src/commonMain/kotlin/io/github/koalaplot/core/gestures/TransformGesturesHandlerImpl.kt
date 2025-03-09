package io.github.koalaplot.core.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastAny
import io.github.koalaplot.core.gestures.pan.PanFlingHandler
import io.github.koalaplot.core.gestures.pan.PanHandler
import io.github.koalaplot.core.gestures.zoom.ZoomHandler
import io.github.koalaplot.core.util.ZoomFactor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Implementation of [TransformGesturesHandler] that detects and processes zoom and pan gestures
 *
 * This class coordinates gesture handling by delegating zoom-related events to [zoomHandler],
 * pan-related events to [panHandler], and fling-based panning to [panFlingHandler]
 *
 * It tracks touch events, determines whether zooming occurs along the horizontal or vertical axis,
 * and dispatches the appropriate gesture events based on the provided [GestureConfig]
 *
 * If pan fling animation is enabled, a fling effect is triggered after the gesture ends
 *
 * @param panHandler Handles panning gestures
 * @param panFlingHandler Handles fling-based panning animations
 * @param zoomHandler Handles zoom gestures
 */
internal class TransformGesturesHandlerImpl(
    private val panHandler: PanHandler,
    private val panFlingHandler: PanFlingHandler,
    private val zoomHandler: ZoomHandler,
) : TransformGesturesHandler {

    private var flingJob: Job? = null

    override suspend fun detectTransformGestures(
        scope: PointerInputScope,
        gestureConfig: GestureConfig,
        onZoomChange: (size: IntSize, centroid: Offset, zoomX: ZoomFactor) -> Unit,
        onPanChange: (size: IntSize, pan: Offset) -> Boolean,
    ) {
        val coroutineScope = CoroutineScope(currentCoroutineContext())

        scope.awaitEachGesture {
            var isHorizontalZoom: Boolean? = null
            val velocityTracker = VelocityTracker()

            awaitFirstDown(requireUnconsumed = false)
            flingJob?.cancel()
            do {
                val event = awaitPointerEvent()
                val canceled = event.changes.fastAny { it.isConsumed }
                if (!canceled) {
                    isHorizontalZoom = isHorizontalZoom ?: event.isHorizontalZoom()

                    val wasZoomConsumed = isHorizontalZoom?.let {
                        zoomHandler.handle(size, event, it, gestureConfig, onZoomChange)
                    } ?: false
                    val wasPanConsumed = panHandler.handle(size, event, gestureConfig, velocityTracker, onPanChange)

                    if (wasZoomConsumed || wasPanConsumed) event.consumeChangedPositions()
                }
            } while (!canceled && event.changes.fastAny { it.pressed })

            if (gestureConfig.panEnabled && gestureConfig.panFlingAnimationEnabled) {
                flingJob = coroutineScope.launch {
                    panFlingHandler.perform(size, velocityTracker, onPanChange)
                }
            }
        }
    }

    /**
     * Returns `true` if the zoom changes along the X axis, `false` if the zoom changes along the Y axis,
     * `null` if there are less than 2 touches(it is impossible to determine the zoom direction)
     */
    private fun PointerEvent.isHorizontalZoom(): Boolean? {
        var firstTouch: Offset? = null
        var secondTouch: Offset? = null
        for (change in changes) {
            if (change.pressed && change.previousPressed) {
                if (firstTouch == null) {
                    firstTouch = change.position
                } else {
                    secondTouch = change.position
                    break
                }
            }
        }
        if (firstTouch == null || secondTouch == null) return null
        val touchDiff = firstTouch - secondTouch
        return abs(touchDiff.x) > abs(touchDiff.y)
    }
}
