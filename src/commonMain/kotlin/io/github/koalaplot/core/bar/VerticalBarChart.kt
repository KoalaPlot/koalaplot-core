package io.github.koalaplot.core.bar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import io.github.koalaplot.core.style.KoalaPlotTheme
import io.github.koalaplot.core.util.HoverableElementAreaScope
import io.github.koalaplot.core.util.fixedHeight
import io.github.koalaplot.core.util.generateHueColorPalette
import io.github.koalaplot.core.xychart.XYChartScope
import io.github.koalaplot.core.xygraph.XYGraphScope
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Default implementation of a BarChartEntry.
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 */
public data class DefaultBarPlotEntry<X, Y>(
    public override val x: X,
    public override val y: VerticalBarPosition<Y>
) : BarPlotEntry<X, Y>

public data class DefaultVerticalBarPosition<Y>(
    public override val yMin: Y,
    public override val yMax: Y
) : VerticalBarPosition<Y>

@Deprecated("Use DefaultBarPlotEntry", ReplaceWith("DefaultBarPlotEntry"))
public data class DefaultBarChartEntry<X, Y>(
    public override val xValue: X,
    public override val yMin: Y,
    public override val yMax: Y,
) : BarChartEntry<X, Y>

/**
 * An interface that defines a data element to be plotted on a Bar chart.
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 */
public interface BarPlotEntry<X, Y> {
    /**
     * X-axis value at which the bar should be plotted
     */
    public val x: X

    /**
     * The y-axis value for the bar.
     */
    public val y: VerticalBarPosition<Y>
}

/**
 * An interface that defines the y-axis coordinates for vertical bars on a bar plot.
 */
public interface VerticalBarPosition<Y> {
    /**
     * The lowest value at which the bar begins on the y-axis
     */
    public val yMin: Y

    /**
     * The highest value at which the bar ends on the y-axis
     */
    public val yMax: Y
}

/**
 * Represents a set of points for a [GroupedVerticalBarPlot].
 *
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 */
public interface BarPlotGroupedPointEntry<X, Y> {
    /**
     * The x-axis value of the entry.
     */
    public val x: X

    /**
     * The y-axis values for each series corresponding to the [x]-axis value.
     */
    public val y: List<VerticalBarPosition<Y>>
}

public interface BarPlotStackedPointEntry<X, Y> {
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

public data class DefaultBarPlotStackedPointEntry<X, Y>(
    override val x: X,
    override val yOrigin: Y,
    override val y: List<Y>,
) : BarPlotStackedPointEntry<X, Y>

@Deprecated("Use BarPlotEntry", ReplaceWith("BarPlotEntry"))
public interface BarChartEntry<X, Y> {
    /**
     * X-axis value at which the bar should be plotted
     */
    public val xValue: X

    /**
     * The lowest value at which the bar begins on the y-axis
     */
    public val yMin: Y

    /**
     * The highest value at which the bar ends on the y-axis
     */
    public val yMax: Y
}

public interface BarScope : HoverableElementAreaScope

private class BarScopeImpl(val hoverableElementAreaScope: HoverableElementAreaScope) :
    BarScope, HoverableElementAreaScope by hoverableElementAreaScope

/**
 * Defines a Composable function used to emit a vertical bar.
 * The parameter series is the chart data series index.
 * The parameter index is the element index within the series.
 * The parameter value is the value of the element.
 */
public typealias VerticalBarComposable<E> = @Composable BarScope.(series: Int, index: Int, value: E) -> Unit

/**
 * A VerticalBarChart to be used in an XYChart and that can plot multiple series either side-by-side
 * or stacked as a stacked bar chart.
 *
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 * @param E The type of the data element holding the values for each bar
 * @param series Bar elements where the outer list contains each series of multiple items.
 * The BarChartEntry's xValues should be equal for entries at the same index within each series.
 * @param bar Composable function to emit a bar for each data element, see [VerticalBarComposable].
 * @param stacked If false, bars from the same index position in each series will be laid out
 * side-by-side occupying the space available between adjacent x-axis values. If true, bars from
 * the same index position in each series will be laid out centered on the x-axis value. The yMin
 * and yMax values for each entry are used to define the vertical positioning of each bar, therefore
 * this function can be used to render multiple stacked bar styles depending on their values.
 * @param maxBarGroupWidth The fraction of space between adjacent x-axis bars or bar groups that
 * may be used. Must be between 0 and 1, defaults to 0.9.
 */
@Deprecated("Replace with either GroupedVerticalBarPlot, VerticalBarPlot, or VerticalBarStackPlot")
@Composable
public fun <X, Y, E : BarChartEntry<X, Y>> XYChartScope<X, Y>.VerticalBarChart(
    series: List<List<E>>,
    modifier: Modifier = Modifier,
    bar: VerticalBarComposable<E> = { i, _, _ ->
        val colors = remember(series.size) { generateHueColorPalette(series.size) }
        DefaultVerticalBar(
            brush = SolidColor(colors[i]),
            modifier = Modifier.fillMaxWidth(KoalaPlotTheme.sizes.barWidth)
        )
    },
    stacked: Boolean = false,
    maxBarGroupWidth: Float = 0.9f,
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec
) {
    require(maxBarGroupWidth in 0f..1f) { "maxBarGroupWidth must be between 0 and 1" }

    val barScope = remember { BarScopeImpl(this) }

    // Animation scale factor
    val beta = remember(series) { Animatable(0f) }
    LaunchedEffect(series) {
        beta.animateTo(1f, animationSpec = animationSpec)
    }

    Layout(modifier = modifier, content = {
        series.forEachIndexed { series, elements ->
            elements.forEachIndexed { element, value ->
                Box { with(barScope) { bar(series, element, value) } }
            }
        }
    }) { measurables: List<Measurable>, constraints: Constraints ->
        val barOffsets = HashMap<Placeable, ClosedRange<Int>>()
        var measurableIndex = 0

        // compute width of area for bars at each x-axis value based on major tick spacing
        val barGroupWidth = xAxisState.let {
            if (it.majorTickOffsets.size > 1) {
                it.majorTickOffsets[1] - it.majorTickOffsets[0]
            } else {
                1f
            } * constraints.maxWidth * maxBarGroupWidth
        }

        val seriesPlaceables = series.map { innerSeries ->
            innerSeries.map { element ->
                val barMin = (
                    yAxisModel.computeOffset(element.yMin).coerceIn(0f, 1f) * constraints.maxHeight
                    ).roundToInt()
                val barMax = (
                    yAxisModel.computeOffset(element.yMax).coerceIn(0f, 1f) * constraints.maxHeight
                    ).roundToInt()

                val barWidth = if (!stacked) {
                    barGroupWidth / series.size
                } else {
                    barGroupWidth
                }

                val height = abs(barMax - barMin) * beta.value
                val p = measurables[measurableIndex++].measure(
                    Constraints(minWidth = 0, maxWidth = barWidth.toInt())
                        .fixedHeight(height.roundToInt())
                )
                barOffsets[p] = barMin..barMax
                p
            }
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            if (seriesPlaceables.isEmpty() || seriesPlaceables[0].isEmpty()) return@layout

            seriesPlaceables[0].indices.forEach { elementIndex ->
                val elementsWidth = seriesPlaceables.sumOf { it[elementIndex].width }
                var xOffset = if (!stacked) {
                    xAxisModel.computeOffset(series[0][elementIndex].xValue) *
                        constraints.maxWidth - elementsWidth / 2f
                } else {
                    xAxisState.majorTickOffsets[elementIndex] * constraints.maxWidth
                }

                seriesPlaceables.forEach { placeables ->
                    val xpos = xOffset + if (stacked) {
                        -placeables[elementIndex].width / 2f
                    } else {
                        0f
                    }
                    placeables[elementIndex].place(
                        (xpos).roundToInt(),
                        constraints.maxHeight -
                            (
                                max(
                                    barOffsets[placeables[elementIndex]]?.start ?: 0,
                                    barOffsets[placeables[elementIndex]]?.endInclusive ?: 0
                                ) * beta.value
                                ).roundToInt()
                    )

                    if (!stacked) xOffset += placeables[elementIndex].width
                }
            }
        }
    }
}

/**
 * A default implementation of a bar for bar charts.
 * @param brush The brush to paint the bar with
 * @param shape An optional shape for the bar.
 * @param border An optional border for the bar.
 * @param hoverElement An optional Composable to be displayed over the bar when hovered over by the
 * mouse or pointer.
 */
@Composable
public fun BarScope.DefaultVerticalBar(
    brush: Brush,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    border: BorderStroke? = null,
    hoverElement: @Composable () -> Unit = {},
) {
    Box(
        modifier = modifier.fillMaxSize()
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .background(brush = brush, shape = shape)
            .clip(shape)
            .hoverableElement(hoverElement)
    )
}

private class EntryToGroupedEntryListAdapter<X, Y>(
    val data: List<BarPlotEntry<X, Y>>
) : AbstractList<BarPlotGroupedPointEntry<X, Y>>() {
    override val size: Int
        get() = data.size

    override fun get(index: Int): BarPlotGroupedPointEntry<X, Y> {
        return EntryToGroupedEntryAdapter(data[index])
    }
}

private class EntryToGroupedEntryAdapter<X, Y>(val entry: BarPlotEntry<X, Y>) : BarPlotGroupedPointEntry<X, Y> {
    override val x: X = entry.x
    override val y: List<VerticalBarPosition<Y>>
        get() = object : AbstractList<VerticalBarPosition<Y>>() {
            override val size: Int = 1
            override fun get(index: Int): VerticalBarPosition<Y> = entry.y
        }
}

private class StackToBarEntryAdapter<X, Y>(val data: List<BarPlotStackedPointEntry<X, Y>>, val barIndex: Int) :
    AbstractList<BarPlotEntry<X, Y>>() {
    override val size: Int = data.size

    override fun get(index: Int): BarPlotEntry<X, Y> {
        return object : BarPlotEntry<X, Y> {
            override val x: X = data[index].x
            override val y: VerticalBarPosition<Y>
                get() = VerticalBarPositionAdapter(data[index], barIndex)
        }
    }

    class VerticalBarPositionAdapter<X, Y>(entry: BarPlotStackedPointEntry<X, Y>, barIndex: Int) :
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
 * Composes a stacked vertical bar plot. This is a convenience method which defers to [VerticalBarPlot]. A stacked
 * bar plot can be also achieved by placing multiple [VerticalBarPlot]s on an
 * [io.github.koalaplot.core.xygraph.XYGraph], one for each "layer" of bars.
 *
 * @param data A List of BarPlotStackedPointEntry data points to plot.
 * @param bar A Composable function to render each bar. xIndex is the index into the data List, barIndex corresponds
 * to the index provided to [BarPlotStackedPointEntry.y], and point is the data point from [data].
 */
@Composable
public fun <X, Y, E : BarPlotStackedPointEntry<X, Y>> XYGraphScope<X, Y>.StackedVerticalBarPlot(
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

/**
 * A VerticalBarPlot to be used in an XYGraph and that plots data points as vertical bars.
 *
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 * @param E The type of the data element holding the values for each bar
 * @param data Data points for where to plot the bars on the XYGraph
 * @param bar Composable function to emit a bar for each data element, given the index of the point in the [data] and
 * the value of the data point.
 * @param barWidth The fraction of space between adjacent x-axis bars that may be used. Must be between 0 and 1,
 * defaults to 0.9.
 */
@Composable
public fun <X, Y, E : BarPlotEntry<X, Y>> XYGraphScope<X, Y>.VerticalBarPlot(
    data: List<E>,
    modifier: Modifier = Modifier,
    bar: @Composable BarScope.(index: Int) -> Unit,
    barWidth: Float = 0.9f,
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec
) {
    val dataAdapter = remember {
        EntryToGroupedEntryListAdapter(data)
    }

    // Delegate to the GroupedVerticalBarPlot - non-grouped is like grouped but with only 1 group per x-axis position
    GroupedVerticalBarPlot(
        dataAdapter,
        modifier = modifier,
        bar = { dataIndex, _, _ ->
            bar(dataIndex)
        },
        maxBarGroupWidth = barWidth,
        animationSpec = animationSpec
    )
}

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
public fun <X, Y, E : BarPlotGroupedPointEntry<X, Y>> XYGraphScope<X, Y>.GroupedVerticalBarPlot(
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
                computeNeighborDistance(index, data) * constraints.maxWidth * maxBarGroupWidth / element.y.size

            element.y.forEachIndexed { i, verticalBarPosition ->
                val barMin = (
                    yAxisModel.computeOffset(verticalBarPosition.yMin).coerceIn(0f, 1f) * constraints.maxHeight
                    ).roundToInt()
                val barMax = (
                    yAxisModel.computeOffset(verticalBarPosition.yMax).coerceIn(0f, 1f) * constraints.maxHeight
                    ).roundToInt()

                val height = abs(barMax - barMin) * beta.value

                val p = measurables[index][i].measure(
                    Constraints(minWidth = 0, maxWidth = scaledBarWidth.toInt()).fixedHeight(height.roundToInt())
                )
                elementPlaceables.add(p)
                elementYBarPositions.add(barMin..barMax)
            }
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEachIndexed { groupIndex, elementPlaceables ->
                // compute center of bar group & allowed width
                val barGroupCenter = xAxisModel.computeOffset(data[groupIndex].x) * constraints.maxWidth
                val barGroupWidth = computeNeighborDistance(groupIndex, data) * constraints.maxWidth * maxBarGroupWidth

                elementPlaceables.forEachIndexed { index, placeable ->
                    // Compute x-axis position for the bar to be centered within its allocated fraction of the
                    // overall bar group width
                    val xPos = barGroupCenter - barGroupWidth / 2 + barGroupWidth / elementPlaceables.size * index +
                        barGroupWidth / elementPlaceables.size / 2 - placeable.width / 2

                    if (placeable.height > 0) {
                        placeable.place(
                            xPos.roundToInt(),
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
private fun <E : BarPlotGroupedPointEntry<X, Y>, X, Y> XYGraphScope<X, Y>.computeNeighborDistance(
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
