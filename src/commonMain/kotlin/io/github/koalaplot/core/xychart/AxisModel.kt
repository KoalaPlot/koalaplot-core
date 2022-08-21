package io.github.koalaplot.core.xychart

import androidx.compose.ui.unit.Dp

/**
 * Holds the offsets for major and minor ticks within an axis.
 */
public interface AxisState {
    public val majorTickOffsets: List<Float>
    public val minorTickOffsets: List<Float>
}

/**
 * The major and minor tick values for an axis.
 * @param T The type of the axis values.
 */
public interface TickValues<T> {
    public val majorTickValues: List<T>
    public val minorTickValues: List<T>
}

/**
 * An interface for classes representing a plot axis.
 *
 * @param T The data type for the axis values.
 */
public interface AxisModel<T> {
    /**
     * Specifies the minimum spacing for major ticks, in Dp units. Must be greater than 0.
     */
    public val minimumMajorTickSpacing: Dp

    /**
     * Computes major and minor tick values based on the minimum tick spacing and the overall
     * [axisLength].
     */
    public fun computeTickValues(axisLength: Dp): TickValues<T>

    /**
     * Computes the linear offset of the provided point along this axis relative to its min value.
     * For a linear axis this is offset = (point-min)/(max-min). Values less than 0 or greater
     * than 1 mean the point is before or beyond the range of the axis, respectively.
     * Nonlinear, e.g. log, axes can be implemented with appropriate transformations in this
     * function.
     */
    public fun computeOffset(point: T): Float

    /**
     * Asks the AxisState to compute new ranges and tick values after zooming, if the axis supports
     * zooming.
     *
     * @param zoomFactor Amount to zoom about the [pivot]. Must be greater than 0,
     * where values less than 1 zoom out and values greater than 1 zoom in.
     * @param pivot The point around which to zoom. Must be between 0 and 1, and represents the
     * distance of the pivot point from the minimum value as a fraction of the overall range
     * of the AxisState. That is, to zoom about a particular point, pivot should be the value
     * returned by [computeOffset].
     */
    public fun zoom(zoomFactor: Float, pivot: Float) {}

    /**
     * Asks the AxisState to compute new ranges and tick values after panning, if the axis
     * supports panning.
     *
     * @param amount to pan as a fraction of the overall axis size. e.g. a value of 0.1 will pan
     * the axis by 10% of its current range, increasing the values. Negative values will pan by
     * decreasing the minimum and maximum axis range.
     */
    public fun pan(amount: Float) {}
}
