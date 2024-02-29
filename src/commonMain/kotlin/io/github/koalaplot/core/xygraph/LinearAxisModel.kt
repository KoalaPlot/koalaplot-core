package io.github.koalaplot.core.xygraph

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val TickRatios = listOf(0.1f, 0.2f, 0.5f, 1f, 2f)

internal const val ZoomRangeLimitDefault = 0.2f
internal const val MinimumMajorTickIncrementDefault = 0.1f

/**
 * An [AxisModel] that uses Float values and is linear.
 *
 * @param range  The minimum to maximum values allowed to be represented on this Axis. Zoom and
 * scroll modifications may not exceed this range.
 * @param zoomRangeLimit Specifies the minimum allowed range after zooming. Must
 * be greater than 0 and less than the difference between the start and end of [range].
 * @param minimumMajorTickIncrement The minimum value between adjacent major ticks.
 * @param minimumMajorTickSpacing Specifies the minimum physical spacing for major ticks, in
 * Dp units. Must be greater than 0.
 * @param minorTickCount The number of minor ticks per major tick interval.
 * @param allowZooming If the axis should allow zooming
 * @param allowPanning If the axis should allow panning.
 */
@Deprecated("Use FloatLinearAxisModel instead", replaceWith = ReplaceWith("FloatLinearAxisModel"))
public class LinearAxisModel(
    public val range: ClosedFloatingPointRange<Float>,
    private val zoomRangeLimit: Float = (range.endInclusive - range.start) * ZoomRangeLimitDefault,
    private val minimumMajorTickIncrement: Float =
        (range.endInclusive - range.start) * MinimumMajorTickIncrementDefault,
    override val minimumMajorTickSpacing: Dp = 50.dp,
    private val minorTickCount: Int = 4,
    private val allowZooming: Boolean = true,
    private val allowPanning: Boolean = true,
) : AxisModel<Float> by FloatLinearAxisModel(
    range,
    zoomRangeLimit,
    minimumMajorTickIncrement,
    minimumMajorTickSpacing,
    minorTickCount,
    allowZooming,
    allowPanning
)

public interface ILinearAxisModel<T> : AxisModel<T> where T : Comparable<T>, T : Number {
    public val range: ClosedRange<T>
}

/**
 * Create and remember a LinearAxisModel.
 */
@Deprecated(message = "Use rememberFloatLinearAxisModel", replaceWith = ReplaceWith("rememberFloatLinearAxisModel"))
@Composable
public fun rememberLinearAxisModel(
    range: ClosedFloatingPointRange<Float>,
    zoomRangeLimit: Float = (range.endInclusive - range.start) * ZoomRangeLimitDefault,
    minimumMajorTickIncrement: Float = (range.endInclusive - range.start) * MinimumMajorTickIncrementDefault,
    minimumMajorTickSpacing: Dp = 50.dp,
    minorTickCount: Int = 4,
    allowZooming: Boolean = true,
    allowPanning: Boolean = true,
): FloatLinearAxisModel = remember(
    range,
    zoomRangeLimit,
    minimumMajorTickIncrement,
    minimumMajorTickSpacing,
    minorTickCount,
    allowZooming,
    allowPanning
) {
    FloatLinearAxisModel(
        range,
        zoomRangeLimit,
        minimumMajorTickIncrement,
        minimumMajorTickSpacing,
        minorTickCount,
        allowZooming,
        allowPanning
    )
}

internal fun <X, Y> List<Point<X, Y>>.toXList(): List<X> = object : AbstractList<X>() {
    override val size: Int
        get() = this@toXList.size

    override fun get(index: Int): X {
        return this@toXList[index].x
    }
}

internal fun <X, Y> List<Point<X, Y>>.toYList(): List<Y> = object : AbstractList<Y>() {
    override val size: Int
        get() = this@toYList.size

    override fun get(index: Int): Y {
        return this@toYList[index].y
    }
}
