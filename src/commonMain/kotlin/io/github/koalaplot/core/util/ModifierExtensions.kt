package io.github.koalaplot.core.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

/**
 * Create a modifier for gesture processing
 *
 * ### Zooming
 * Desktop: Ctrl/Cmd + mouse wheel/touch scroll (Optional Shift for horizontal scaling)
 *
 * Js/JsWasm: mouse/touch scroll (Optional Shift for horizontal zooming). It doesn't work for mobile devices
 *
 * Android/iOS: standard gesture behavior
 *
 * ### Panning
 * Desktop: mouse wheel/touch scroll (Optional Shift for horizontal panning) or click with movement
 *
 * Js/JsWasm: just a click with a move. It doesn't work for mobile devices
 *
 * Android/iOS: standard gesture behavior
 *
 * Ctrl/Cmd are not used in Js/JsWasm because they are reserved for native scaling in browsers
 *
 * @param key1 see [androidx.compose.ui.input.pointer.pointerInput]
 * @param key2 see [androidx.compose.ui.input.pointer.pointerInput]
 * @param panLock If `true`, it blocks pan handling
 * @param zoomLock If `true`, it blocks zoom handling
 * @param lockZoomRatio If `true`, the split zoom will be blocked and the zoom will be calculated simultaneously
 * for the X and Y axes
 * @param onZoomChange Called every time the zoom changes
 * @param onPanChange Called every time the pan changes
 */
internal expect fun Modifier.onGestureInput(
    key1: Any?,
    key2: Any?,
    panLock: Boolean = false,
    zoomLock: Boolean = false,
    lockZoomRatio: Boolean = false,
    onZoomChange: (size: IntSize, centroid: Offset, zoomX: Float, zoomY: Float) -> Unit,
    onPanChange: (size: IntSize, pan: Offset) -> Unit,
): Modifier
