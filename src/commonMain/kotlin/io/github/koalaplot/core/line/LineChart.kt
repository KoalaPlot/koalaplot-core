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
import io.github.koalaplot.core.xychart.XYChartScope
import io.github.koalaplot.core.xychart.XYChartScopeAdapter
import io.github.koalaplot.core.xygraph.Point

@Deprecated(
    "Replace with the version that uses XYGraphScope as receiver",
    ReplaceWith("XYGraphScope.StackedAreaPlot")
)
@Composable
public fun <X, Y> XYChartScope<X, Y>.StackedAreaChart(
    data: List<MultiPoint<X, Y>>,
    styles: List<StackedAreaStyle>,
    firstBaseline: AreaBaseline.ConstantLine<X, Y>,
    modifier: Modifier = Modifier,
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec
) {
    /**
     * An adapter between Multipoints and Points. This approach is prioritizing memory usage over number of
     * calculations as it is not caching values and recomputes them instead.
     */
    class ListAdapter(val series: Int) : AbstractList<Point<X, Y>>() {
        override val size: Int
            get() = data.size

        override fun get(index: Int): Point<X, Y> {
            return data[index].accumulateTo(series)
        }
    }

    fun Point<X, Y>.pt(): io.github.koalaplot.core.xychart.Point<X, Y> {
        return io.github.koalaplot.core.xychart.Point(x, y)
    }

    XYChartScopeAdapter(this).GeneralLinePlot(
        ListAdapter(0),
        modifier,
        styles[0].lineStyle,
        null,
        styles[0].areaStyle,
        firstBaseline,
        animationSpec
    ) { points: List<Point<X, Y>>, size: Size ->

        moveTo(scale(points[0].pt(), size))
        for (index in 1..points.lastIndex) {
            lineTo(scale(points[index].pt(), size))
        }
    }

    for (series in 1..<data[0].y.size) {
        XYChartScopeAdapter(this).GeneralLinePlot(
            ListAdapter(series),
            modifier,
            styles[series].lineStyle,
            null,
            styles[series].areaStyle,
            AreaBaseline.ArbitraryLine(ListAdapter(series - 1)),
            animationSpec
        ) { points: List<Point<X, Y>>, size: Size ->
            moveTo(scale(points[0].pt(), size))
            for (index in 1..points.lastIndex) {
                lineTo(scale(points[index].pt(), size))
            }
        }
    }
}

@Deprecated(
    "Replace with the version that uses XYGraphScope as receiver",
    ReplaceWith("XYGraphScope.LinePlot")
)
@Composable
public fun <X, Y> XYChartScope<X, Y>.LineChart(
    data: List<io.github.koalaplot.core.xychart.Point<X, Y>>,
    modifier: Modifier = Modifier,
    lineStyle: LineStyle? = null,
    symbol: (@Composable HoverableElementAreaScope.(Point<X, Y>) -> Unit)? = null,
    areaStyle: AreaStyle? = null,
    areaBaseline: AreaBaseline<X, Y>? = null,
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec
) {
    if (areaStyle != null && areaBaseline != null) {
        XYChartScopeAdapter(this).AreaPlot(
            PointListAdapter(data),
            areaBaseline,
            areaStyle,
            modifier,
            lineStyle,
            symbol,
            animationSpec
        )
    } else {
        XYChartScopeAdapter(this).LinePlot(
            PointListAdapter(data),
            modifier,
            lineStyle,
            symbol,
            animationSpec
        )
    }
}

private class PointListAdapter<X, Y>(val data: List<io.github.koalaplot.core.xychart.Point<X, Y>>) :
    AbstractList<Point<X, Y>>() {
    override val size: Int
        get() = data.size

    override fun get(index: Int): Point<X, Y> {
        return object : Point<X, Y> {
            override val x: X
                get() = data[index].x
            override val y: Y
                get() = data[index].y
        }
    }
}

@Deprecated(
    "Replace with the version that uses XYGraphScope as receiver",
    ReplaceWith("XYGraphScope.StairstepPlot")
)
@Composable
public fun <X, Y> XYChartScope<X, Y>.StairstepChart(
    data: List<io.github.koalaplot.core.xychart.Point<X, Y>>,
    lineStyle: LineStyle,
    modifier: Modifier = Modifier,
    symbol: (@Composable HoverableElementAreaScope.(Point<X, Y>) -> Unit)? = null,
    areaStyle: AreaStyle? = null,
    areaBaseline: AreaBaseline<X, Y>? = null,
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec
) {
    XYChartScopeAdapter(this).StairstepPlot(
        PointListAdapter(data),
        lineStyle,
        modifier,
        symbol,
        areaStyle,
        areaBaseline,
        animationSpec
    )
}

/**
 * Represents a set of points for a [StackedAreaPlot].
 *
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 */
@Deprecated("Use StackedAreaPlotEntry instead.")
public interface MultiPoint<X, Y> {
    /**
     * The x-axis value of this [MultiPoint].
     */
    public val x: X

    /**
     * The y-axis values for each line series corresponding to the [x]-axis value.
     */
    public val y: List<Y>

    /**
     * Computes the stacked value of the point up to and including the series at index [series] from the
     * [y] value List and returns it as a Point.
     */
    public fun accumulateTo(series: Int): Point<X, Y>
}

/**
 * Default implementation of the [MultiPoint] interface using Floats for y-axis values.
 *
 * @param X Data type for x-axis values
 */
@Deprecated("Use StackedAreaPlotEntry instead.")
public data class DefaultMultiPoint<X>(override val x: X, override val y: List<Float>) : MultiPoint<X, Float> {
    override fun accumulateTo(series: Int): Point<X, Float> {
        var sum = 0f
        for (seriesIndex in 0..series) {
            sum += y[seriesIndex]
        }
        return Point(x, sum)
    }
}
