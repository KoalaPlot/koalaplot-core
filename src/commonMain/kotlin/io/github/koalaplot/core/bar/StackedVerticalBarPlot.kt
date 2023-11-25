package io.github.koalaplot.core.bar

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.koalaplot.core.style.KoalaPlotTheme
import io.github.koalaplot.core.xygraph.XYGraphScope

/**
 * Represents a set of data points for a [StackedVerticalBarPlot].
 */
public interface VerticalBarPlotStackedPointEntry<X, Y> {
    /**
     * The x-axis value of the entry.
     */
    public val x: X

    /**
     * The value for the y-axis Origin, which is the bottom of the lowest bar in the stack.
     */
    public val yOrigin: Y

    /**
     * The y-axis coordinate of the top of each bar in the stack, where lower indices are for bars lower in the stack.
     */
    public val y: List<Y>
}

public data class DefaultVerticalBarPlotStackedPointEntry<X, Y>(
    override val x: X,
    override val yOrigin: Y,
    override val y: List<Y>,
) : VerticalBarPlotStackedPointEntry<X, Y>

/**
 * Composes a stacked vertical bar plot. This is a convenience method which defers to [VerticalBarPlot]. A stacked
 * bar plot can be also achieved by placing multiple [VerticalBarPlot]s on an
 * [io.github.koalaplot.core.xygraph.XYGraph], one for each "layer" of bars.
 *
 * @param data A List of BarPlotStackedPointEntry data points to plot.
 * @param bar A Composable function to render each bar. xIndex is the index into the data List, barIndex corresponds
 * to the index provided to [VerticalBarPlotStackedPointEntry.y], and point is the data point from [data].
 */
@Composable
public fun <X, Y, E : VerticalBarPlotStackedPointEntry<X, Y>> XYGraphScope<X, Y>.StackedVerticalBarPlot(
    data: List<E>,
    modifier: Modifier = Modifier,
    bar: @Composable BarScope.(xIndex: Int, barIndex: Int) -> Unit,
    barWidth: Float = 0.9f,
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec
) {
    val maxBarCount = data.maxOf { it.y.size }

    for (barIndex in 0..<maxBarCount) {
        val layerData = StackToBarEntryAdapter(data, barIndex)
        VerticalBarPlot(
            layerData,
            modifier,
            { index ->
                bar(index, barIndex)
            },
            barWidth,
            animationSpec
        )
    }
}

private class StackToBarEntryAdapter<X, Y>(val data: List<VerticalBarPlotStackedPointEntry<X, Y>>, val barIndex: Int) :
    AbstractList<VerticalBarPlotEntry<X, Y>>() {
    override val size: Int = data.size

    override fun get(index: Int): VerticalBarPlotEntry<X, Y> {
        return object : VerticalBarPlotEntry<X, Y> {
            override val x: X = data[index].x
            override val y: VerticalBarPosition<Y>
                get() = VerticalBarPositionAdapter(data[index], barIndex)
        }
    }

    class VerticalBarPositionAdapter<X, Y>(entry: VerticalBarPlotStackedPointEntry<X, Y>, barIndex: Int) :
        VerticalBarPosition<Y> {
        override val yMin: Y = if (barIndex == 0) {
            entry.yOrigin
        } else {
            // barIndex can be greater than entry.y.lastIndex if some other x-axis entries had more
            // bars to display in the stack
            entry.y[(barIndex - 1).coerceAtMost(entry.y.lastIndex)]
        }

        override val yMax: Y = entry.y[barIndex.coerceAtMost(entry.y.lastIndex)]
    }
}

/**
 * A Vertical Bar Plot to be used in an XYGraph and that plots multiple series as a stack of bars.
 *
 * @param X The type of the x-axis values
 * @param barWidth The fraction of space between adjacent x-axis bars may be used. Must be between 0 and 1,
 * defaults to 0.9.
 * @param content A block which describes the content for the plot.
 */
@Composable
public fun <X> XYGraphScope<X, Float>.StackedVerticalBarPlot(
    modifier: Modifier = Modifier,
    barWidth: Float = 0.9f,
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec,
    content: StackedVerticalBarPlotScope<X>.() -> Unit
) {
    val scope = remember(content) {
        val scope = StackedVerticalBarPlotScopeImpl<X>()
        scope.content()
        scope
    }

    data class EntryWithBars<X>(
        override val x: X,
        val yb: List<Pair<Float, @Composable BarScope.() -> Unit>>
    ) : VerticalBarPlotStackedPointEntry<X, Float> {
        override val yOrigin = 0f

        override val y: List<Float> = object : AbstractList<Float>() {
            override val size: Int = yb.size
            override fun get(index: Int): Float = yb.subList(0, index + 1).fold(0F) { acc, (y, _) ->
                acc + y
            }
        }
    }

    class DataHolder : AbstractList<VerticalBarPlotStackedPointEntry<X, Float>>() {
        val data: List<EntryWithBars<X>>

        init {
            val dataMap: MutableMap<X, EntryWithBars<X>> = mutableMapOf()
            scope.series.forEach {
                it.data.entries.forEach { (x, entry) ->
                    val pos = dataMap.getOrPut(x) { EntryWithBars(x, listOf()) }
                    dataMap[x] = pos.copy(yb = pos.yb + Pair(entry.first, entry.second))
                }
            }

            data = dataMap.values.toList()
        }

        override val size: Int = data.size
        override fun get(index: Int): VerticalBarPlotStackedPointEntry<X, Float> {
            return data[index]
        }
    }

    val data = remember(scope) { DataHolder() }

    StackedVerticalBarPlot(
        data,
        modifier,
        { xIndex, seriesIndex ->
            data.data[xIndex].yb[seriesIndex].second.invoke(this)
        },
        barWidth,
        animationSpec
    )
}

/**
 * Receiver scope used by [StackedVerticalBarPlot].
 */
public interface StackedVerticalBarPlotScope<X> {
    /**
     * Starts a new series of bars to be plotted, with a [defaultBar] to use for rendering all
     * bars in this series.
     */
    public fun series(
        defaultBar: @Composable BarScope.() -> Unit = solidBar(Color.Blue),
        content: StackedVerticalBarPlotSeriesScope<X>.() -> Unit
    )
}

private class StackedVerticalBarPlotScopeImpl<X> : StackedVerticalBarPlotScope<X> {
    val series: MutableList<StackedVerticalBarPlotSeriesScopeImpl<X>> = mutableListOf()
    override fun series(
        defaultBar: @Composable BarScope.() -> Unit,
        content: StackedVerticalBarPlotSeriesScope<X>.() -> Unit
    ) {
        val scope = StackedVerticalBarPlotSeriesScopeImpl<X>(defaultBar)
        series.add(scope)
        scope.content()
    }
}

/**
 * Scope item to allow adding items to a [StackedVerticalBarPlot].
 */
public interface StackedVerticalBarPlotSeriesScope<X> {
    /**
     * Adds an item at the specified [x] axis coordinate, with a vertical extent [y], which will
     * be added to series elements at the same x-axis coordinate already added to the plot.
     * An optional [bar] can be provided to customize the Composable used to
     * generate the bar for this specific item.
     */
    public fun item(x: X, y: Float, bar: (@Composable BarScope.() -> Unit)? = null)
}

private class StackedVerticalBarPlotSeriesScopeImpl<X>(val defaultBar: @Composable BarScope.() -> Unit) :
    StackedVerticalBarPlotSeriesScope<X> {
    val data: MutableMap<X, Pair<Float, @Composable BarScope.() -> Unit>> = mutableMapOf()

    override fun item(x: X, y: Float, bar: (@Composable BarScope.() -> Unit)?) {
        data[x] = Pair(y, bar ?: defaultBar)
    }
}
