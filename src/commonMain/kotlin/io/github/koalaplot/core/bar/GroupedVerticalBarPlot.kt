package io.github.koalaplot.core.bar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import io.github.koalaplot.core.style.KoalaPlotTheme
import io.github.koalaplot.core.util.fixedHeight
import io.github.koalaplot.core.util.generateHueColorPalette
import io.github.koalaplot.core.xygraph.XYGraphScope
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Represents a set of points for a [GroupedVerticalBarPlot].
 *
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 */
public interface VerticalBarPlotGroupedPointEntry<X, Y> {
    /**
     * The x-axis value of the entry.
     */
    public val x: X

    /**
     * The y-axis values for each series corresponding to the [x]-axis value.
     */
    public val y: List<VerticalBarPosition<Y>>
}

public data class DefaultVerticalBarPlotGroupedPointEntry<X, Y>(
    override val x: X,
    override val y: List<VerticalBarPosition<Y>>
) : VerticalBarPlotGroupedPointEntry<X, Y>

/**
 * A Vertical Bar Plot to be used in an XYGraph and that plots multiple series side-by-side.
 *
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 * @param E The type of the data element holding the values for each bar
 * @param data Coordinate data for the bars to be plotted.
 * @param bar Composable function to emit a bar for each data element, see [VerticalBarComposable].
 * @param maxBarGroupWidth The fraction of space between adjacent x-axis bars or bar groups that
 * may be used. Must be between 0 and 1, defaults to 0.9.
 */
@Composable
public fun <X, Y, E : VerticalBarPlotGroupedPointEntry<X, Y>> XYGraphScope<X, Y>.GroupedVerticalBarPlot(
    data: List<E>,
    modifier: Modifier = Modifier,
    bar: @Composable BarScope.(dataIndex: Int, groupIndex: Int, entry: E) -> Unit = { i, g, _ ->
        val colors = remember(data) {
            generateHueColorPalette(data.maxOf { it.y.size })
        }
        DefaultVerticalBar(
            brush = SolidColor(colors[g]),
            modifier = Modifier.fillMaxWidth(KoalaPlotTheme.sizes.barWidth)
        )
    },
    maxBarGroupWidth: Float = 0.9f,
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec
) {
    require(maxBarGroupWidth in 0f..1f) { "maxBarGroupWidth must be between 0 and 1" }
    if (data.isEmpty()) return

    val barScope = remember { BarScopeImpl(this) }

    // Animation scale factor
    val beta = remember(data) { Animatable(0f) }
    LaunchedEffect(data) {
        beta.animateTo(1f, animationSpec = animationSpec)
    }

    Layout(
        modifier = modifier,
        contents = buildList {
            data.forEachIndexed { dataIndex, entry ->
                add {
                    entry.y.forEachIndexed { index, _ ->
                        Box { with(barScope) { bar(dataIndex, index, entry) } }
                    }
                }
            }
        }
    ) { measurables: List<List<Measurable>>, constraints: Constraints ->
        val placeables: MutableList<MutableList<Placeable>> = mutableListOf()
        val yAxisBarPositions: MutableList<MutableList<ClosedRange<Int>>> = mutableListOf()

        data.forEachIndexed { index, element ->
            val elementPlaceables: MutableList<Placeable> = mutableListOf()
            placeables.add(elementPlaceables)
            val elementYBarPositions: MutableList<ClosedRange<Int>> = mutableListOf()
            yAxisBarPositions.add(elementYBarPositions)

            val scaledBarWidth =
                (
                    computeNeighborDistance(index, data) * constraints.maxWidth * maxBarGroupWidth / element.y.size
                    ).toInt()

            element.y.forEachIndexed { i, verticalBarPosition ->
                val barMin = (
                    yAxisModel.computeOffset(verticalBarPosition.yMin).coerceIn(0f, 1f) * constraints.maxHeight
                    ).roundToInt()
                val barMax = (
                    yAxisModel.computeOffset(verticalBarPosition.yMax).coerceIn(0f, 1f) * constraints.maxHeight
                    ).roundToInt()

                val height = abs(barMax - barMin) * beta.value

                val p = measurables[index][i].measure(
                    Constraints(minWidth = 0, maxWidth = scaledBarWidth).fixedHeight(height.roundToInt())
                )
                elementPlaceables.add(p)
                elementYBarPositions.add(barMin..barMax)
            }
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEachIndexed { groupIndex, elementPlaceables ->
                // compute center of bar group & allowed width
                val barGroupCenter = xAxisModel.computeOffset(data[groupIndex].x) * constraints.maxWidth
                val scaledBarWidth = (
                    computeNeighborDistance(groupIndex, data) *
                        constraints.maxWidth * maxBarGroupWidth / elementPlaceables.size
                    ).toInt()
                val barGroupWidth = scaledBarWidth * elementPlaceables.size

                // Compute x-axis position for the bar to be centered within its allocated fraction of the
                // overall bar group width
                var xPos = (barGroupCenter - barGroupWidth / 2).toInt()
                elementPlaceables.forEachIndexed { index, placeable ->
                    if (placeable.height > 0) {
                        placeable.place(
                            xPos,
                            constraints.maxHeight -
                                (
                                    max(
                                        yAxisBarPositions[groupIndex][index].start,
                                        yAxisBarPositions[groupIndex][index].endInclusive
                                    ) * beta.value
                                    )
                                    .roundToInt()
                        )
                    }
                    xPos += scaledBarWidth
                }
            }
        }
    }
}

/**
 * Computes the minimum x-axis distance to the data point neighbors to the data point at [index].
 * If [index] is 0 then the distance to the next point is used. If [index] is the last data point, then
 * the distance to the next to last point is used. If [data] has size=1, then 1 is returned. Otherwise, the
 * minimum between the distance to the previous point and the distance to the next point is returned.
 */
private fun <E : VerticalBarPlotGroupedPointEntry<X, Y>, X, Y> XYGraphScope<X, Y>.computeNeighborDistance(
    index: Int,
    data: List<E>
): Float {
    return if (index == 0) {
        if (data.size == 1) {
            1f
        } else {
            val center = xAxisModel.computeOffset(data[index].x)
            val right = xAxisModel.computeOffset(data[index + 1].x)
            abs(center - right)
        }
    } else if (index == data.lastIndex) {
        val center = xAxisModel.computeOffset(data[index].x)
        val left = xAxisModel.computeOffset(data[index - 1].x)
        abs(center - left)
    } else {
        val left = xAxisModel.computeOffset(data[index - 1].x)
        val center = xAxisModel.computeOffset(data[index].x)
        val right = xAxisModel.computeOffset(data[index + 1].x)

        min(abs(center - left), abs(center - right))
    }
}

/**
 * A Vertical Bar Plot to be used in an XYGraph and that plots multiple series side-by-side.
 *
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 * @param maxBarGroupWidth The fraction of space between adjacent x-axis bars or bar groups that
 * may be used. Must be between 0 and 1, defaults to 0.9.
 * @param content A block which describes the content for the plot.
 */
@Composable
public fun <X, Y> XYGraphScope<X, Y>.GroupedVerticalBarPlot(
    modifier: Modifier = Modifier,
    maxBarGroupWidth: Float = 0.9f,
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec,
    content: GroupedVerticalBarPlotScope<X, Y>.() -> Unit
) {
    val scope = remember(content) {
        val scope = GroupedVerticalBarPlotScopeImpl<X, Y>()
        scope.content()
        scope
    }

    data class EntryWithBars<X, Y>(
        override val x: X,
        val yb: List<Pair<VerticalBarPosition<Y>, @Composable BarScope.() -> Unit>>
    ) : VerticalBarPlotGroupedPointEntry<X, Y> {
        override val y: List<VerticalBarPosition<Y>> = object : AbstractList<VerticalBarPosition<Y>>() {
            override val size: Int = yb.size
            override fun get(index: Int): VerticalBarPosition<Y> = yb[index].first
        }
    }

    class DataHolder : AbstractList<VerticalBarPlotGroupedPointEntry<X, Y>>() {
        val data: List<EntryWithBars<X, Y>>

        init {
            val dataMap: MutableMap<X, EntryWithBars<X, Y>> = mutableMapOf()
            scope.series.forEach {
                it.data.values.forEach { (entry, barFunc) ->
                    val pos = dataMap.getOrPut(entry.x) { EntryWithBars(entry.x, listOf()) }
                    dataMap[entry.x] = pos.copy(yb = pos.yb + Pair(entry.y, barFunc))
                }
            }

            data = dataMap.values.toList()
        }

        override val size: Int = data.size
        override fun get(index: Int): VerticalBarPlotGroupedPointEntry<X, Y> {
            return data[index]
        }
    }

    val data = remember(scope) { DataHolder() }

    GroupedVerticalBarPlot(
        data,
        modifier,
        { xIndex, seriesIndex, _ ->
            data.data[xIndex].yb[seriesIndex].second.invoke(this)
        },
        maxBarGroupWidth,
        animationSpec
    )
}

/**
 * Receiver scope used by [GroupedVerticalBarPlot].
 */
public interface GroupedVerticalBarPlotScope<X, Y> {
    /**
     * Starts a new series of bars to be plotted, with a [defaultBar] to use for rendering all
     * bars in this series.
     */
    public fun series(
        defaultBar: @Composable BarScope.() -> Unit = solidBar(Color.Blue),
        content: VerticalBarPlotScope<X, Y>.() -> Unit
    )
}

private class GroupedVerticalBarPlotScopeImpl<X, Y> : GroupedVerticalBarPlotScope<X, Y> {
    val series: MutableList<VerticalBarPlotScopeImpl<X, Y>> = mutableListOf()
    override fun series(defaultBar: @Composable BarScope.() -> Unit, content: VerticalBarPlotScope<X, Y>.() -> Unit) {
        val scope = VerticalBarPlotScopeImpl<X, Y>(defaultBar)
        series.add(scope)
        scope.content()
    }
}
