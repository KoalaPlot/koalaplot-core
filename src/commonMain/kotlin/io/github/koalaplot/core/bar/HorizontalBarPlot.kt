package io.github.koalaplot.core.bar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.koalaplot.core.animation.StartAnimationUseCase
import io.github.koalaplot.core.style.KoalaPlotTheme
import io.github.koalaplot.core.xygraph.XYGraphScope

/**
 * Default implementation of a HorizontalBarPlotEntry
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 */
public data class DefaultHorizontalBarPlotEntry<X, Y>(
    public override val y: Y,
    public override val x: BarPosition<X>
) : HorizontalBarPlotEntry<X, Y>

/**
 * An interface that defines a data element to be plotted on a Horizontal bar chart.
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 */
public interface HorizontalBarPlotEntry<X, Y> {
    /**
     * y-axis value at which the bar should be plotted
     */
    public val y: Y

    /**
     * The x-axis value for the bar position.
     */
    public val x: BarPosition<X>
}

/**
 * Convenience function for creating a HorizontalBarPosition.
 */
public fun <X> horizontalBarPosition(xMin: X, xMax: X): BarPosition<X> = DefaultBarPosition(xMin, xMax)

/**
 * Convenience function for creating a HorizontalBarPlotEntry.
 */
public fun <X, Y> horizontalBarPlotEntry(y: Y, xMin: X, xMax: X): HorizontalBarPlotEntry<X, Y> =
    DefaultHorizontalBarPlotEntry(y, horizontalBarPosition(xMin, xMax))

/**
 * Defines a Composable function used to emit a horizontal bar.
 * The parameter series is the chart data series index.
 * The parameter index is the element index within the series.
 * The parameter value is the value of the element.
 */
public typealias HorizontalBarComposable<E> = @Composable BarScope.(series: Int, index: Int, value: E) -> Unit

/**
 * A HorizontalBarPlot to be used in an XYGraph and that plots a single series of data points as horizontal bars.
 *
 * @param Y The type of the x-axis values
 * @param xData x-axis data points for each bar. Assumes each bar starts at 0.
 * @param yData y-axis data points for where to plot the bars on the XYGraph. The size of xData and yData must match.
 * @param bar Composable function to emit a bar for each data element, given the index of the point in the data and
 * the value of the data point.
 * @param barWidth The fraction of space between adjacent x-axis bars that may be used. Must be between 0 and 1,
 * defaults to 0.9.
 */
@Composable
public fun <Y> XYGraphScope<Float, Y>.HorizontalBarPlot(
    xData: List<Float>,
    yData: List<Y>,
    modifier: Modifier = Modifier,
    bar: @Composable BarScope.(index: Int) -> Unit,
    barWidth: Float = 0.9f,
    startAnimationUseCase: StartAnimationUseCase =
        StartAnimationUseCase(
            executionType = StartAnimationUseCase.ExecutionType.Default,
            /* chart animation */
            KoalaPlotTheme.animationSpec,
        )
) {
    require(xData.size == yData.size) { "xData and yData must be the same size." }
    HorizontalBarPlot(
        yData.mapIndexed { index, y ->
            DefaultHorizontalBarPlotEntry(y, DefaultBarPosition(0f, xData[index]))
        },
        modifier,
        bar,
        barWidth,
        startAnimationUseCase
    )
}

/**
 * A HorizontalBarPlot to be used in an XYGraph and that plots data points as horizontal bars.
 *
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 * @param E The type of the data element holding the values for each bar
 * @param data Data points for where to plot the bars on the XYGraph
 * @param bar Composable function to emit a bar for each data element, given the index of the point in the [data].
 * @param barWidth The fraction of space between adjacent x-axis bars that may be used. Must be between 0 and 1,
 * defaults to 0.9.
 */
@Composable
public fun <X, Y, E : HorizontalBarPlotEntry<X, Y>> XYGraphScope<X, Y>.HorizontalBarPlot(
    data: List<E>,
    modifier: Modifier = Modifier,
    bar: @Composable BarScope.(index: Int) -> Unit,
    barWidth: Float = 0.9f,
    startAnimationUseCase: StartAnimationUseCase =
        StartAnimationUseCase(
            executionType = StartAnimationUseCase.ExecutionType.Default,
            /* chart animation */
            KoalaPlotTheme.animationSpec,
        )
) {
    val dataAdapter = remember(data) {
        HorizontalEntryToGroupedEntryListAdapter(data)
    }

    GroupedHorizontalBarPlot(
        dataAdapter,
        modifier = modifier,
        bar = { dataIndex, _, _ ->
            bar(dataIndex)
        },
        maxBarGroupWidth = barWidth,
        startAnimationUseCase = startAnimationUseCase
    )
}

private class HorizontalEntryToGroupedEntryListAdapter<X, Y>(
    val data: List<HorizontalBarPlotEntry<X, Y>>
) : AbstractList<BarPlotGroupedPointEntry<Y, X>>() {
    override val size: Int
        get() = data.size

    override fun get(index: Int): BarPlotGroupedPointEntry<Y, X> {
        return HorizontalEntryToGroupedEntryAdapter(data[index])
    }
}

private class HorizontalEntryToGroupedEntryAdapter<X, Y>(val entry: HorizontalBarPlotEntry<X, Y>) :
    BarPlotGroupedPointEntry<Y, X> {
    override val i: Y = entry.y
    override val d: List<BarPosition<X>>
        get() = object : AbstractList<BarPosition<X>>() {
            override val size: Int = 1
            override fun get(index: Int): BarPosition<X> = entry.x
        }
}

/**
 * Creates a Horizontal Bar Plot.
 *
 * @param defaultBar A Composable to provide the bar if not specified on an individually added item.
 * @param barWidth The fraction of space between adjacent x-axis bars that may be used. Must be between 0 and 1,
 *  defaults to 0.9.
 * @param content A block which describes the content for the plot.
 */
@Composable
public fun <X, Y> XYGraphScope<X, Y>.HorizontalBarPlot(
    defaultBar: @Composable BarScope.() -> Unit = solidBar(Color.Blue),
    modifier: Modifier = Modifier,
    barWidth: Float = 0.9f,
    startAnimationUseCase: StartAnimationUseCase =
        StartAnimationUseCase(
            executionType = StartAnimationUseCase.ExecutionType.Default,
            /* chart animation */
            KoalaPlotTheme.animationSpec,
        ),
    content: HorizontalBarPlotScope<X, Y>.() -> Unit
) {
    val scope = remember(content, defaultBar) { HorizontalBarPlotScopeImpl<X, Y>(defaultBar) }
    val data = remember(scope) {
        scope.content()
        scope.data.values.toList()
    }

    HorizontalBarPlot(
        data.map { it.first },
        modifier,
        {
            data[it].second.invoke(this)
        },
        barWidth,
        startAnimationUseCase
    )
}

/**
 * Scope item to allow adding items to a [HorizontalBarPlot].
 */
public interface HorizontalBarPlotScope<X, Y> {
    /**
     * Adds an item at the specified [y] axis coordinate, with horizontal extent spanning from
     * [xMin] to [xMax]. An optional [bar] can be provided to customize the Composable used to
     * generate the bar for this specific item.
     */
    public fun item(y: Y, xMin: X, xMax: X, bar: (@Composable BarScope.() -> Unit)? = null)
}

internal class HorizontalBarPlotScopeImpl<X, Y>(private val defaultBar: @Composable BarScope.() -> Unit) :
    HorizontalBarPlotScope<X, Y> {
    val data: MutableMap<Y, Pair<HorizontalBarPlotEntry<X, Y>, @Composable BarScope.() -> Unit>> =
        mutableMapOf()

    override fun item(y: Y, xMin: X, xMax: X, bar: (@Composable BarScope.() -> Unit)?) {
        data[y] = Pair(horizontalBarPlotEntry(y, xMin, xMax), bar ?: defaultBar)
    }
}
