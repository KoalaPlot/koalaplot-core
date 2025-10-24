package io.github.koalaplot.core.bar

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.Layout
import io.github.koalaplot.core.animation.StartAnimationUseCase
import io.github.koalaplot.core.style.KoalaPlotTheme
import io.github.koalaplot.core.util.generateHueColorPalette
import io.github.koalaplot.core.xygraph.XYGraphScope

/**
 * A Horizontal Bar Plot to be used in an XYGraph and that plots multiple series side-by-side.
 *
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 * @param E The type of the data element holding the values for each bar
 * @param data Coordinate data for the bars to be plotted.
 * @param bar Composable function to emit a bar for each data element, see [VerticalBarComposable].
 * @param maxBarGroupWidth The fraction of space between adjacent y-axis bars or bar groups that
 * may be used. Must be between 0 and 1, defaults to 0.9.
 * @param startAnimationUseCase Controls the animation.
 */
@Composable
public fun <X, Y, E : BarPlotGroupedPointEntry<Y, X>> XYGraphScope<X, Y>.GroupedHorizontalBarPlot(
    data: List<E>,
    modifier: Modifier = Modifier,
    bar: @Composable BarScope.(dataIndex: Int, groupIndex: Int, entry: E) -> Unit = { i, g, _ ->
        val colors = remember(data) {
            generateHueColorPalette(data.maxOf { it.d.size })
        }
        DefaultBar(
            brush = SolidColor(colors[g]),
            modifier = Modifier.fillMaxWidth(KoalaPlotTheme.sizes.barWidth)
        )
    },
    maxBarGroupWidth: Float = 0.9f,
    startAnimationUseCase: StartAnimationUseCase =
        StartAnimationUseCase(
            executionType = StartAnimationUseCase.ExecutionType.Default,
            /* chart animation */
            KoalaPlotTheme.animationSpec,
        ),
) {
    require(maxBarGroupWidth in 0f..1f) { "maxBarGroupWidth must be between 0 and 1" }
    require(startAnimationUseCase.animatables.size == 1) { "startAnimationUseCase must have one animatable" }
    if (data.isEmpty()) return

    val barScope = remember { BarScopeImpl(this) }

    // Animation scale factor
    val beta = remember(data) { startAnimationUseCase.animatables[0] }
    startAnimationUseCase(key = data)

    Layout(
        modifier = modifier,
        contents = buildList {
            data.forEachIndexed { dataIndex, entry ->
                add {
                    entry.d.forEachIndexed { index, _ ->
                        Box { with(barScope) { bar(dataIndex, index, entry) } }
                    }
                }
            }
        },
        measurePolicy = BarPlotMeasurePolicyHorizontal(this, data, maxBarGroupWidth, beta)
    )
}

/**
 * A Horizontal Bar Plot to be used in an XYGraph and that plots multiple series side-by-side.
 *
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 * @param maxBarGroupWidth The fraction of space between adjacent x-axis bars or bar groups that
 * may be used. Must be between 0 and 1, defaults to 0.9.
 * @param startAnimationUseCase Controls the animation.
 * @param content A block which describes the content for the plot.
 */
@Composable
public fun <X, Y> XYGraphScope<X, Y>.GroupedHorizontalBarPlot(
    modifier: Modifier = Modifier,
    maxBarGroupWidth: Float = 0.9f,
    startAnimationUseCase: StartAnimationUseCase,
    content: GroupedHorizontalBarPlotScope<X, Y>.() -> Unit
) {
    val scope = remember(content) {
        val scope = GroupedHorizontalBarPlotScopeImpl<X, Y>()
        scope.content()
        scope
    }

    data class EntryWithBars<X, Y>(
        override val i: Y,
        val xb: List<Pair<BarPosition<X>, @Composable BarScope.() -> Unit>>
    ) : BarPlotGroupedPointEntry<Y, X> {
        override val d: List<BarPosition<X>> = object : AbstractList<BarPosition<X>>() {
            override val size: Int = xb.size
            override fun get(index: Int): BarPosition<X> = xb[index].first
        }
    }

    class DataHolder : AbstractList<BarPlotGroupedPointEntry<Y, X>>() {
        val data: List<EntryWithBars<X, Y>>

        init {
            val dataMap: MutableMap<Y, EntryWithBars<X, Y>> = mutableMapOf()
            scope.series.forEach {
                it.data.values.forEach { (entry, barFunc) ->
                    val pos = dataMap.getOrPut(entry.y) { EntryWithBars(entry.y, listOf()) }
                    dataMap[entry.y] = pos.copy(xb = pos.xb + Pair(entry.x, barFunc))
                }
            }

            data = dataMap.values.toList()
        }

        override val size: Int = data.size
        override fun get(index: Int): BarPlotGroupedPointEntry<Y, X> {
            return data[index]
        }
    }

    val data = remember(scope) { DataHolder() }

    GroupedHorizontalBarPlot(
        data,
        modifier,
        { xIndex, seriesIndex, _ ->
            data.data[xIndex].xb[seriesIndex].second.invoke(this)
        },
        maxBarGroupWidth,
        startAnimationUseCase = startAnimationUseCase,
    )
}

/**
 * A Horizontal Bar Plot to be used in an XYGraph and that plots multiple series side-by-side.
 *
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 * @param maxBarGroupWidth The fraction of space between adjacent x-axis bars or bar groups that
 * may be used. Must be between 0 and 1, defaults to 0.9.
 * @param animationSpec Specifies the animation to use when the pie chart is first drawn.
 * @param content A block which describes the content for the plot.
 */
@Composable
public fun <X, Y> XYGraphScope<X, Y>.GroupedHorizontalBarPlot(
    modifier: Modifier = Modifier,
    maxBarGroupWidth: Float = 0.9f,
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec,
    content: GroupedHorizontalBarPlotScope<X, Y>.() -> Unit
) {
    GroupedHorizontalBarPlot(
        modifier = modifier,
        maxBarGroupWidth = maxBarGroupWidth,
        startAnimationUseCase = StartAnimationUseCase(
            executionType = StartAnimationUseCase.ExecutionType.Default,
            /* chart animation */
            animationSpec,
        ),
        content = content
    )
}

/**
 * Receiver scope used by [GroupedHorizontalBarPlot].
 *
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 */
public interface GroupedHorizontalBarPlotScope<X, Y> {
    /**
     * Starts a new series of bars to be plotted, with a [defaultBar] to use for rendering all
     * bars in this series.
     */
    public fun series(
        defaultBar: @Composable BarScope.() -> Unit = solidBar(Color.Blue),
        content: HorizontalBarPlotScope<X, Y>.() -> Unit
    )
}

private class GroupedHorizontalBarPlotScopeImpl<X, Y> : GroupedHorizontalBarPlotScope<X, Y> {
    val series: MutableList<HorizontalBarPlotScopeImpl<X, Y>> = mutableListOf()
    override fun series(defaultBar: @Composable BarScope.() -> Unit, content: HorizontalBarPlotScope<X, Y>.() -> Unit) {
        val scope = HorizontalBarPlotScopeImpl<X, Y>(defaultBar)
        series.add(scope)
        scope.content()
    }
}
