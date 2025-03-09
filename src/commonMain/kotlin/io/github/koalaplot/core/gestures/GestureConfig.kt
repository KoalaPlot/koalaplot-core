package io.github.koalaplot.core.gestures

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/**
 * Configuration for gesture handling, including settings for panning and zooming and their use
 *
 * @property panXEnabled Whether the pan is enabled for the X-axis
 * @property panYEnabled Whether the pan is enabled for the Y-axis
 * @property panXConsumptionEnabled Whether the pan on the X-axis should be consumed.
 * Has no effect for `js` and `wasmJs`
 * @property panYConsumptionEnabled Whether the pan on the Y-axis should be consumed.
 * Has no effect for `js` and `wasmJs`
 * @property zoomXEnabled Whether the zoom is enabled for the X-axis
 * @property zoomYEnabled Whether the zoom is enabled for the Y-axis
 * @property independentZoomEnabled Whether independent zoom (zooming on X and Y axes separately) is allowed
 * @property panFlingAnimationEnabled Whether the inertial pan fling animation is enabled
 */
@Immutable
public data class GestureConfig(

    /**
     * Whether the pan gesture is enabled for the X-axis
     *
     * If `true`, pan gestures along the X-axis will be processed
     *
     * If `false`, no pan gestures will be handled for the X-axis
     */
    val panXEnabled: Boolean = false,

    /**
     * Whether the pan gesture is enabled for the Y-axis
     *
     * If `true`, pan gestures along the Y-axis will be processed
     *
     * If `false`, no pan gestures will be handled for the Y-axis
     */
    val panYEnabled: Boolean = false,

    /**
     * Whether the pan gesture on the X-axis should be consumed. Has no effect for `js` and `wasmJs`
     *
     * If `true`, the pan gesture will be consumed and will not propagate further
     *
     * If `false`, the pan gesture will not be consumed and may propagate. However, the gesture will still be
     * processed but `PointerInputChange#consume` will not be called, allowing parent containers to process
     * the gesture if needed
     */
    val panXConsumptionEnabled: Boolean = true,

    /**
     * Whether the pan gesture on the Y-axis should be consumed. Has no effect for `js` and `wasmJs`
     *
     * If `true`, the pan gesture will be consumed and will not propagate further
     *
     * If `false`, the pan gesture will not be consumed and may propagate. However, the gesture will still be
     * processed but `PointerInputChange#consume` will not be called, allowing parent containers to process
     * the gesture if needed
     */
    val panYConsumptionEnabled: Boolean = true,

    /**
     * Whether the zoom gesture is enabled for the X-axis
     *
     * If `true`, zoom gestures along the X-axis will be processed
     *
     * If `false`, no zoom gestures will be handled for the X-axis
     */
    val zoomXEnabled: Boolean = false,

    /**
     * Whether the zoom gesture is enabled for the Y-axis
     *
     * If `true`, zoom gestures along the Y-axis will be processed
     *
     * If `false`, no zoom gestures will be handled for the Y-axis
     */
    val zoomYEnabled: Boolean = false,

    /**
     * Whether independent zoom (zooming on X and Y axes separately) is allowed
     *
     * If `true`, the zoom can be either only on the X axis, or only on the Y axis,
     * or independently on the X and Y axes at the same time (the behavior depends on the target platform),
     * False if the total zoom factor must be used.
     *
     * If `false`, zooming will be locked to a single ratio (X and Y zoom together)
     *
     * Behavior for Android and iOS:
     * True does not mean getting independent zoom coefficients simultaneously for each axis,
     * if the zoom was initiated:
     * - horizontally - the zoom coefficient will change only for the X axis,
     * - vertically - the zoom coefficient will change only for the Y axis.
     *
     * Behavior for Desktop platforms:
     * True means getting independent zoom coefficients simultaneously or separately for each axis,
     * if the zoom was initiated:
     * - horizontally - the zoom coefficient will change only for the X axis,
     * - vertically - the zoom coefficient will change only for the Y axis,
     * - diagonally - the zoom coefficient will change along the axes X and Y at the same time
     *
     * Behavior for JS and wasmJS:
     * True means getting independent zoom coefficients simultaneously or separately for each axis,
     * if the zoom was initiated:
     * - horizontally - the zoom coefficient will change only for the X axis,
     * - vertically - the zoom coefficient will change only for the Y axis,
     * - diagonally - the zoom coefficient will change along the axes X and Y at the same time.
     *
     * JS and wasmJS have slight differences in response behavior (for example, zoom coefficients for the same gesture
     * will be interpreted with a difference of several tenths or hundredths), and zoom handling with the mouse wheel
     * scroll while pressing Ctrl/Cmd is not supported (a problem with browser scaling)
     */
    val independentZoomEnabled: Boolean = false,

    /**
     * Whether the inertial pan fling animation is enabled
     *
     * If `true`, pan gestures will be followed by an inertial fling animation,
     * creating a natural sliding effect with gradual deceleration.
     *
     * If `false`, no inertial fling animation will be applied, and the pan gesture
     * will stop immediately after the user finishes the gesture.
     */
    val panFlingAnimationEnabled: Boolean = true,
) {
    @Stable
    val gesturesEnabled: Boolean
        get() = panXEnabled || panYEnabled || zoomXEnabled || zoomYEnabled

    @Stable
    val panEnabled: Boolean
        get() = panXEnabled || panYEnabled

    @Stable
    val zoomEnabled: Boolean = zoomXEnabled || zoomYEnabled
}
