package io.github.koalaplot.core.bar

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import io.github.koalaplot.core.style.KoalaPlotTheme
import io.github.koalaplot.core.util.HoverableElementAreaScope
import io.github.koalaplot.core.xygraph.XYGraphScope

/**
 * Default implementation of a BarChartEntry.
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 */
public data class DefaultVerticalBarPlotEntry<X, Y>(
    public override val x: X,
    public override val y: VerticalBarPosition<Y>
) : VerticalBarPlotEntry<X, Y>

public data class DefaultVerticalBarPosition<Y>(
    public override val yMin: Y,
    public override val yMax: Y
) : VerticalBarPosition<Y>

/**
 * An interface that defines a data element to be plotted on a Bar chart.
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 */
public interface VerticalBarPlotEntry<X, Y> {
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
 * Convenience function for creating a VerticalBarPosition.
 */
public fun <Y> verticalBarPosition(yMin: Y, yMax: Y): VerticalBarPosition<Y> = DefaultVerticalBarPosition(yMin, yMax)

/**
 * Convenience function for creating a VerticalBarPlotEntry.
 */
public fun <X, Y> verticalBarPlotEntry(x: X, yMin: Y, yMax: Y): VerticalBarPlotEntry<X, Y> =
    DefaultVerticalBarPlotEntry(x, verticalBarPosition(yMin, yMax))

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

public interface BarScope : HoverableElementAreaScope

internal class BarScopeImpl(val hoverableElementAreaScope: HoverableElementAreaScope) :
    BarScope, HoverableElementAreaScope by hoverableElementAreaScope

/**
 * Defines a Composable function used to emit a vertical bar.
 * The parameter series is the chart data series index.
 * The parameter index is the element index within the series.
 * The parameter value is the value of the element.
 */
public typealias VerticalBarComposable<E> = @Composable BarScope.(series: Int, index: Int, value: E) -> Unit

/**
 * Defines a Composable function used to emit a vertical bar for [VerticalBarPlotEntry] values.
 * Delegates to [VerticalBarComposable] with [VerticalBarPlotEntry] as type parameter.
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 */
public typealias DefaultVerticalBarComposable<X, Y> = VerticalBarComposable<VerticalBarPlotEntry<X, Y>>

/**
 * A VerticalBarPlot to be used in an XYGraph and that plots a single series of data points as vertical bars.
 *
 * @param X The type of the x-axis values
 * @param xData X-axis data points for where to plot the bars on the XYGraph. The size of xData and yData must match.
 * @param yData y-axis data points for each bar. Assumes each bar starts at 0.
 * @param bar Composable function to emit a bar for each data element, given the index of the point in the data and
 * the value of the data point.
 * @param barWidth The fraction of space between adjacent x-axis bars that may be used. Must be between 0 and 1,
 * defaults to 0.9.
 */
@Composable
public fun <X> XYGraphScope<X, Float>.VerticalBarPlot(
    xData: List<X>,
    yData: List<Float>,
    modifier: Modifier = Modifier,
    bar: DefaultVerticalBarComposable<X, Float>,
    barWidth: Float = 0.9f,
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec
) {
    require(xData.size == yData.size) { "xData and yData must be the same size." }
    VerticalBarPlot(
        xData.mapIndexed { index, x ->
            DefaultVerticalBarPlotEntry(x, DefaultVerticalBarPosition(0f, yData[index]))
        },
        modifier,
        bar,
        barWidth,
        animationSpec
    )
}

/**
 * A VerticalBarPlot to be used in an XYGraph and that plots data points as vertical bars.
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
public fun <X, Y, E : VerticalBarPlotEntry<X, Y>> XYGraphScope<X, Y>.VerticalBarPlot(
    data: List<E>,
    modifier: Modifier = Modifier,
    bar: DefaultVerticalBarComposable<X, Y>,
    barWidth: Float = 0.9f,
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec
) {
    val dataAdapter = remember(data) {
        EntryToGroupedEntryListAdapter(data)
    }

    // Delegate to the GroupedVerticalBarPlot - non-grouped is like grouped but with only 1 group per x-axis position
    GroupedVerticalBarPlot(
        dataAdapter,
        modifier = modifier,
        bar = { series, index, value ->
            bar(series, index, GroupedEntryToEntryAdapter(value))
        },
        maxBarGroupWidth = barWidth,
        animationSpec = animationSpec
    )
}

private class EntryToGroupedEntryListAdapter<X, Y>(
    val data: List<VerticalBarPlotEntry<X, Y>>
) : AbstractList<VerticalBarPlotGroupedPointEntry<X, Y>>() {
    override val size: Int
        get() = data.size

    override fun get(index: Int): VerticalBarPlotGroupedPointEntry<X, Y> {
        return EntryToGroupedEntryAdapter(data[index])
    }
}

private class EntryToGroupedEntryAdapter<X, Y>(val entry: VerticalBarPlotEntry<X, Y>) :
    VerticalBarPlotGroupedPointEntry<X, Y> {
    override val x: X = entry.x
    override val y: List<VerticalBarPosition<Y>>
        get() = object : AbstractList<VerticalBarPosition<Y>>() {
            override val size: Int = 1
            override fun get(index: Int): VerticalBarPosition<Y> = entry.y
        }
}

internal class GroupedEntryToEntryAdapter<X, Y>(
    private val entry: VerticalBarPlotGroupedPointEntry<X, Y>
) : VerticalBarPlotEntry<X, Y> {
    override val x: X
        get() = entry.x
    override val y: VerticalBarPosition<Y>
        get() = entry.y.first()
}

/**
 * Creates a Vertical Bar Plot.
 *
 * @param defaultBar A Composable to provide the bar if not specified on an individually added item.
 * @param barWidth The fraction of space between adjacent x-axis bars that may be used. Must be between 0 and 1,
 *  defaults to 0.9.
 * @param content A block which describes the content for the plot.
 */
@Composable
public fun <X, Y> XYGraphScope<X, Y>.VerticalBarPlot(
    defaultBar: DefaultVerticalBarComposable<X, Y> = solidBar(Color.Blue),
    modifier: Modifier = Modifier,
    barWidth: Float = 0.9f,
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec,
    content: VerticalBarPlotScope<X, Y>.() -> Unit
) {
    val scope = remember(content, defaultBar) { VerticalBarPlotScopeImpl(defaultBar) }
    val data = remember(scope) {
        scope.content()
        scope.data.values.toList()
    }

    VerticalBarPlot(
        data.map { it.first },
        modifier,
        { series, index, value ->
            data[index].second.invoke(this, series, index, value)
        },
        barWidth,
        animationSpec
    )
}

/**
 * Scope item to allow adding items to a [VerticalBarPlot].
 */
public interface VerticalBarPlotScope<X, Y> {
    /**
     * Adds an item at the specified [x] axis coordinate, with vertical extent spanning from
     * [yMin] to [yMax]. An optional [bar] can be provided to customize the Composable used to
     * generate the bar for this specific item.
     */
    public fun item(x: X, yMin: Y, yMax: Y, bar: (DefaultVerticalBarComposable<X, Y>)? = null)
}

internal class VerticalBarPlotScopeImpl<X, Y>(private val defaultBar: DefaultVerticalBarComposable<X, Y>) :
    VerticalBarPlotScope<X, Y> {
    val data: MutableMap<X, Pair<VerticalBarPlotEntry<X, Y>, DefaultVerticalBarComposable<X, Y>>> =
        mutableMapOf()

    override fun item(x: X, yMin: Y, yMax: Y, bar: (DefaultVerticalBarComposable<X, Y>)?) {
        data[x] = Pair(verticalBarPlotEntry(x, yMin, yMax), bar ?: defaultBar)
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

/**
 * A simplified DefaultVerticalBar that uses a Solid Color [color] and default [RectangleShape].
 */
@Composable
public fun BarScope.DefaultVerticalBar(
    color: Color,
    shape: Shape = RectangleShape,
    border: BorderStroke? = null,
) {
    DefaultVerticalBar(SolidColor(color), shape = shape, border = border)
}

/**
 * Factory function to create a Composable that emits a solid colored bar.
 */
public fun <X, Y> solidBar(
    color: Color,
    shape: Shape = RectangleShape,
    border: BorderStroke? = null,
): DefaultVerticalBarComposable<X, Y> = { _, _, _ ->
    DefaultVerticalBar(SolidColor(color), shape = shape, border = border)
}

/**
 * Factory function to create a Composable that emits a solid colored bar.
 * The endings of each bar consist of a concave and a convex shape.
 */
public fun <X> XYGraphScope<X, Float>.planoConvexBar(
    color: Color,
    border: BorderStroke? = null,
): DefaultVerticalBarComposable<X, Float> = { _, index, value ->
    DefaultVerticalBar(
        brush = SolidColor(color),
        shape = PlanoConvexShape(this@planoConvexBar, index, value),
        border = border
    )
}

/**
 * Factory function to create a Composable that emits a solid colored bar.
 * The endings of each bar consist of a concave and a convex shape.
 * There's an additional convex cutout at the bottom of the bar.
 */
public fun <X> XYGraphScope<X, Float>.biConvexBar(
    color: Color,
    border: BorderStroke? = null,
): DefaultVerticalBarComposable<X, Float> = { _, index, value ->
    DefaultVerticalBar(
        brush = SolidColor(color),
        shape = BiConvexShape(this@biConvexBar, index, value),
        border = border
    )
}
