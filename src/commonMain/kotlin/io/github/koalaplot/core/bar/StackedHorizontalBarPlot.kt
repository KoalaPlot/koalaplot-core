package io.github.koalaplot.core.bar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.koalaplot.core.animation.StartAnimationUseCase
import io.github.koalaplot.core.style.KoalaPlotTheme
import io.github.koalaplot.core.xygraph.XYGraphScope

/**
 * Represents a set of data points for a [StackedHorizontalBarPlot].
 */
public interface HorizontalBarPlotStackedPointEntry<X, Y> {
    /**
     * The y-axis value of the entry.
     */
    public val y: Y

    /**
     * The value for the x-axis Origin, which is the left edge of the left most bar in the stack.
     */
    public val xOrigin: X

    /**
     * The x-axis coordinate of the right of each bar in the stack, where lower indices are for bars left in the stack.
     */
    public val x: List<X>
}

public data class DefaultHorizontalBarPlotStackedPointEntry<X, Y>(
    override val y: Y,
    override val xOrigin: X,
    override val x: List<X>,
) : HorizontalBarPlotStackedPointEntry<X, Y>

/**
 * Composes a stacked horizontal bar plot. This is a convenience method which defers to [HorizontalBarPlot]. A stacked
 * bar plot can be also achieved by placing multiple [HorizontalBarPlot]s on an
 * [io.github.koalaplot.core.xygraph.XYGraph], one for each "layer" of bars.
 *
 * @param data A List of BarPlotStackedPointEntry data points to plot.
 * @param bar A Composable function to render each bar. yIndex is the index into the data List, barIndex corresponds
 * to the index provided to [HorizontalBarPlotStackedPointEntry.x], and point is the data point from [data].
 */
@Composable
public fun <X, Y, E : HorizontalBarPlotStackedPointEntry<X, Y>> XYGraphScope<X, Y>.StackedHorizontalBarPlot(
    data: List<E>,
    modifier: Modifier = Modifier,
    bar: @Composable BarScope.(yIndex: Int, barIndex: Int) -> Unit,
    barWidth: Float = 0.9f,
    startAnimationUseCase: StartAnimationUseCase =
        StartAnimationUseCase(
            executionType = StartAnimationUseCase.ExecutionType.Default,
            /* chart animation */
            KoalaPlotTheme.animationSpec,
        )
) {
    val maxBarCount = data.maxOf { it.x.size }

    for (barIndex in 0..<maxBarCount) {
        val layerData = HorizontalStackToBarEntryAdapter(data, barIndex)
        HorizontalBarPlot(
            layerData,
            modifier,
            { index ->
                bar(index, barIndex)
            },
            barWidth,
            startAnimationUseCase
        )
    }
}

private class HorizontalStackToBarEntryAdapter<X, Y>(
    val data: List<HorizontalBarPlotStackedPointEntry<X, Y>>,
    val barIndex: Int
) :
    AbstractList<HorizontalBarPlotEntry<X, Y>>() {
    override val size: Int = data.size

    override fun get(index: Int): HorizontalBarPlotEntry<X, Y> {
        return object : HorizontalBarPlotEntry<X, Y> {
            override val y: Y = data[index].y
            override val x: BarPosition<X>
                get() = HorizontalBarPositionAdapter(data[index], barIndex)
        }
    }

    class HorizontalBarPositionAdapter<X, Y>(entry: HorizontalBarPlotStackedPointEntry<X, Y>, barIndex: Int) :
        BarPosition<X> {
        override val start: X = if (barIndex == 0) {
            entry.xOrigin
        } else {
            // barIndex can be greater than entry.y.lastIndex if some other x-axis entries had more
            // bars to display in the stack
            entry.x[(barIndex - 1).coerceAtMost(entry.x.lastIndex)]
        }

        override val end: X = entry.x[barIndex.coerceAtMost(entry.x.lastIndex)]
    }
}

/**
 * A Horizontal Bar Plot to be used in an XYGraph and that plots multiple series as a stack of bars.
 *
 * @param Y The type of the x-axis values
 * @param barWidth The fraction of space between adjacent x-axis bars may be used. Must be between 0 and 1,
 * defaults to 0.9.
 * @param content A block which describes the content for the plot.
 */
@Composable
public fun <Y> XYGraphScope<Float, Y>.StackedHorizontalBarPlot(
    modifier: Modifier = Modifier,
    barWidth: Float = 0.9f,
    startAnimationUseCase: StartAnimationUseCase =
        StartAnimationUseCase(
            executionType = StartAnimationUseCase.ExecutionType.Default,
            /* chart animation */
            KoalaPlotTheme.animationSpec,
        ),
    content: StackedHorizontalBarPlotScope<Y>.() -> Unit
) {
    val scope = remember(content) {
        val scope = StackedHorizontalBarPlotScopeImpl<Y>()
        scope.content()
        scope
    }

    data class EntryWithBars<Y>(
        override val y: Y,
        val xb: List<Pair<Float, @Composable BarScope.() -> Unit>>
    ) : HorizontalBarPlotStackedPointEntry<Float, Y> {
        override val xOrigin = 0f

        override val x: List<Float> = object : AbstractList<Float>() {
            override val size: Int = xb.size
            override fun get(index: Int): Float = xb.subList(0, index + 1).fold(0F) { acc, (y, _) ->
                acc + y
            }
        }
    }

    class DataHolder : AbstractList<HorizontalBarPlotStackedPointEntry<Float, Y>>() {
        val data: List<EntryWithBars<Y>>

        init {
            val dataMap: MutableMap<Y, EntryWithBars<Y>> = mutableMapOf()
            scope.series.forEach {
                it.data.entries.forEach { (y, entry) ->
                    val pos = dataMap.getOrPut(y) { EntryWithBars(y, listOf()) }
                    dataMap[y] = pos.copy(xb = pos.xb + Pair(entry.first, entry.second))
                }
            }

            data = dataMap.values.toList()
        }

        override val size: Int = data.size
        override fun get(index: Int): HorizontalBarPlotStackedPointEntry<Float, Y> {
            return data[index]
        }
    }

    val data = remember(scope) { DataHolder() }

    StackedHorizontalBarPlot(
        data,
        modifier,
        { yIndex, seriesIndex ->
            data.data[yIndex].xb[seriesIndex].second.invoke(this)
        },
        barWidth,
        startAnimationUseCase
    )
}

/**
 * Receiver scope used by [StackedHorizontalBarPlot].
 */
public interface StackedHorizontalBarPlotScope<Y> {
    /**
     * Starts a new series of bars to be plotted, with a [defaultBar] to use for rendering all
     * bars in this series.
     */
    public fun series(
        defaultBar: @Composable BarScope.() -> Unit = solidBar(Color.Blue),
        content: StackedHorizontalBarPlotSeriesScope<Y>.() -> Unit
    )
}

private class StackedHorizontalBarPlotScopeImpl<Y> : StackedHorizontalBarPlotScope<Y> {
    val series: MutableList<StackedHorizontalBarPlotSeriesScopeImpl<Y>> = mutableListOf()
    override fun series(
        defaultBar: @Composable BarScope.() -> Unit,
        content: StackedHorizontalBarPlotSeriesScope<Y>.() -> Unit
    ) {
        val scope = StackedHorizontalBarPlotSeriesScopeImpl<Y>(defaultBar)
        series.add(scope)
        scope.content()
    }
}

/**
 * Scope item to allow adding items to a [StackedHorizontalBarPlot].
 */
public interface StackedHorizontalBarPlotSeriesScope<Y> {
    /**
     * Adds an item at the specified [y] axis coordinate, with a horizontal extent [x], which will
     * be added to series elements at the same y-axis coordinate already added to the plot.
     * An optional [bar] can be provided to customize the Composable used to
     * generate the bar for this specific item.
     */
    public fun item(x: Float, y: Y, bar: (@Composable BarScope.() -> Unit)? = null)
}

private class StackedHorizontalBarPlotSeriesScopeImpl<Y>(val defaultBar: @Composable BarScope.() -> Unit) :
    StackedHorizontalBarPlotSeriesScope<Y> {
    val data: MutableMap<Y, Pair<Float, @Composable BarScope.() -> Unit>> = mutableMapOf()

    override fun item(x: Float, y: Y, bar: (@Composable BarScope.() -> Unit)?) {
        data[y] = Pair(x, bar ?: defaultBar)
    }
}
