package io.github.koalaplot.core.line

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import io.github.koalaplot.core.style.AreaStyle
import io.github.koalaplot.core.style.KoalaPlotTheme
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.util.HoverableElementAreaScope
import io.github.koalaplot.core.util.lineTo
import io.github.koalaplot.core.util.moveTo
import io.github.koalaplot.core.xygraph.Point
import io.github.koalaplot.core.xygraph.XYGraphScope

/**
 * Specifies baseline coordinates for drawing filled areas on line charts.
 */
public sealed interface AreaBaseline<X, Y> {

    /**
     * Specifies that the area should be drawn to a constant y-axis value across the x-axis range.
     */
    public data class ConstantLine<X, Y>(val value: Y) : AreaBaseline<X, Y>

    /**
     * Specifies an arbitrary line to which the area should be drawn. The number of values and their
     * x-axis coordinates must match the data provided to [LinePlot].
     */
    public data class ArbitraryLine<X, Y>(val values: List<Point<X, Y>>) : AreaBaseline<X, Y>
}

/**
 * Provides styling for a single series in a [StackedAreaPlot].
 *
 * @param lineStyle The style to apply to the line.
 * @param areaStyle The style to apply to the area.
 *
 */
public data class StackedAreaStyle(val lineStyle: LineStyle, val areaStyle: AreaStyle)

/**
 * An area plot that draws data as points and lines with a filled area to a baseline.
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 * @param data Data series to plot.
 * @param lineStyle Style to use for the line that connects the data points. If null, no line is drawn.
 * @param symbol Composable for the symbol to be shown at each data point.
 * @param areaStyle Style to use for filling the area between the line and a baseline. If null, no area will be drawn.
 * @param areaBaseline Baseline location for the area. If [areaBaseline] is an [AreaBaseline.ArbitraryLine] then it is
 * recommended that the first and last x-axis values for the baseline match those in the [data] so the
 * left and right area bounds will be vertical.
 * @param modifier Modifier for the plot.
 */
@Composable
public fun <X, Y> XYGraphScope<X, Y>.AreaPlot2(
    data: List<Point<X, Y>>,
    areaBaseline: AreaBaseline<X, Y>,
    areaStyle: AreaStyle,
    modifier: Modifier = Modifier.Companion,
    lineStyle: LineStyle? = null,
    symbol: (@Composable (Point<X, Y>) -> Unit)? = null,
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec
) {
    AreaPlot(data, areaBaseline, areaStyle, modifier, lineStyle, { symbol?.invoke(it) }, animationSpec)
}

/**
 * An area plot that draws data as points and lines with a filled area to a baseline.
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 * @param data Data series to plot.
 * @param lineStyle Style to use for the line that connects the data points. If null, no line is drawn.
 * @param symbol Composable for the symbol to be shown at each data point.
 * @param areaStyle Style to use for filling the area between the line and a baseline. If null, no area will be drawn.
 * @param areaBaseline Baseline location for the area. If [areaBaseline] is an [AreaBaseline.ArbitraryLine] then it is
 * recommended that the first and last x-axis values for the baseline match those in the [data] so the
 * left and right area bounds will be vertical.
 * @param modifier Modifier for the plot.
 */
@Deprecated("Use AreaPlot2 instead", replaceWith = ReplaceWith("AreaPlot2"))
@Composable
public fun <X, Y> XYGraphScope<X, Y>.AreaPlot(
    data: List<Point<X, Y>>,
    areaBaseline: AreaBaseline<X, Y>,
    areaStyle: AreaStyle,
    modifier: Modifier = Modifier,
    lineStyle: LineStyle? = null,
    symbol: (@Composable HoverableElementAreaScope.(Point<X, Y>) -> Unit)? = null,
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec
) {
    if (data.isEmpty()) return

    GeneralLinePlot(
        data,
        modifier,
        lineStyle,
        symbol,
        areaStyle,
        areaBaseline,
        animationSpec
    ) { points: List<Point<X, Y>>, size: Size ->
        moveTo(scale(points[0], size))
        for (index in 1..points.lastIndex) {
            lineTo(scale(points[index], size))
        }
    }
}

/**
 * A Stacked Area Plot is like a line plot but with filled areas between lines, and where each successive line
 * is added to all of the lines before it, so they stack.
 *
 * @param X Data type of x-axis values
 * @param Y Data type of y-axis values
 * @param data List of [StackedAreaPlotEntry] data items for the plot. Each MultiPoint must hold the same number of
 * y-axis values.
 * @param styles A list of [StackedAreaStyle]s to be applied to each series in the data. The size of this list must
 * match the number of data series provided by [data]
 * @param firstBaseline Provides the value for the bottom of the first line's area, in units of [Y]. Typically
 * this will be the 0 value for [Y]'s data type. Note: If the y-axis is logarithmic  this value cannot be 0.
 * @param animationSpec The animation to provide to the graph when it is created or changed.
 */
@Composable
public fun <X, Y> XYGraphScope<X, Y>.StackedAreaPlot(
    data: List<StackedAreaPlotEntry<X, Y>>,
    styles: List<StackedAreaStyle>,
    firstBaseline: AreaBaseline.ConstantLine<X, Y>,
    modifier: Modifier = Modifier.Companion,
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec
) {
    if (data.isEmpty()) return

    /**
     * An adapter between Multipoints and Points for use with GeneralLinePlot.
     */
    class ListAdapter(val series: Int) : AbstractList<Point<X, Y>>() {
        override val size: Int
            get() = data.size

        override fun get(index: Int): Point<X, Y> {
            return Point(data[index].x, data[index].y[series])
        }
    }

    GeneralLinePlot(
        ListAdapter(0),
        modifier,
        styles[0].lineStyle,
        null,
        styles[0].areaStyle,
        firstBaseline,
        animationSpec
    ) { points: List<Point<X, Y>>, size: Size ->
        moveTo(scale(points[0], size))
        for (index in 1..points.lastIndex) {
            lineTo(scale(points[index], size))
        }
    }

    for (series in 1..<data[0].y.size) {
        GeneralLinePlot(
            ListAdapter(series),
            modifier,
            styles[series].lineStyle,
            null,
            styles[series].areaStyle,
            AreaBaseline.ArbitraryLine(ListAdapter(series - 1)),
            animationSpec
        ) { points: List<Point<X, Y>>, size: Size ->
            moveTo(scale(points[0], size))
            for (index in 1..points.lastIndex) {
                lineTo(scale(points[index], size))
            }
        }
    }
}

/**
 * Represents a set of points for a [StackedAreaPlot].
 *
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 */
public interface StackedAreaPlotEntry<X, Y> {
    /**
     * The x-axis value of this [StackedAreaPlotEntry].
     */
    public val x: X

    /**
     * The y-axis coordinate of each line in the stack, where lower indices are for lines lower in the stack.
     */
    public val y: Array<Y>
}

/**
 * Returns an instance of a [StackedAreaPlotEntry] using the provided data.
 */
public fun <X, Y> stackedAreaPlotEntry(x: X, y: Array<Y>): StackedAreaPlotEntry<X, Y> =
    object : StackedAreaPlotEntry<X, Y> {
        override val x = x
        override val y = y
    }

/**
 * Adapts data for use in a [StackedAreaPlot] where the input data consists of a List of x-axis coordinates and
 * multiple Lists of Float y-axis coordinates, one per line, where the values are before stacking. This adapter
 * will sum y-axis values to compute each line's height in the [StackedAreaPlot]. The size of [xData] and all
 * series in [yData] must be equal.
 */
public class StackedAreaPlotDataAdapter<X>(private val xData: List<X>, private val yData: List<List<Float>>) :
    AbstractList<StackedAreaPlotEntry<X, Float>>() {

    init {
        if (xData.isNotEmpty()) {
            require(yData.isNotEmpty()) { "yData must not be empty if xData is not empty" }
            yData.forEachIndexed { index, data ->
                require(xData.size == data.size) {
                    "Size of yData with index $index must be the same size as xData."
                }
            }
        }
    }

    override val size: Int = xData.size

    override fun get(index: Int): StackedAreaPlotEntry<X, Float> {
        return object : StackedAreaPlotEntry<X, Float> {
            override val x: X = xData[index]
            override val y: Array<Float>
                get() {
                    var last = 0f
                    return Array(yData.size) {
                        last += yData[it][index]
                        last
                    }
                }
        }
    }
}
