package io.github.koalaplot.core.line

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.unit.Constraints
import io.github.koalaplot.core.style.AreaStyle
import io.github.koalaplot.core.style.KoalaPlotTheme
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.util.HoverableElementAreaScope
import io.github.koalaplot.core.util.lineTo
import io.github.koalaplot.core.util.moveTo
import io.github.koalaplot.core.xychart.Point
import io.github.koalaplot.core.xychart.XYChartScope
import kotlin.math.pow
import kotlin.math.sqrt

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
     * x-axis coordinates must match the data provided to [LineChart].
     */
    public data class ArbitraryLine<X, Y>(val values: List<Point<X, Y>>) : AreaBaseline<X, Y>
}

/**
 * Provides styling for a single series in a [StackedAreaChart].
 *
 * @param lineStyle The style to apply to the line.
 * @param areaStyle The style to apply to the area.
 *
 */
public data class StackedAreaStyle(val lineStyle: LineStyle, val areaStyle: AreaStyle)

/**
 * A Stacked Area Chart is like a line chart with filled areas between lines, but where each successive line
 * is added to all of the lines before it, so they stack.
 *
 * @param X Data type of x-axis values
 * @param Y Data type of y-axis values
 * @param data List of [MultiPoint] data items for the chart. Each MultiPoint must hold the same number of
 * y-axis values.
 * @param styles A list of [StackedAreaStyle]s to be applied to each series in the data. The size of this list must
 * match the number of data series provided by [data]
 * @param firstBaseline Provides the value for the bottom of the first line's area, in units of [Y]. Typically
 * this will be the 0 value for [Y]'s data type. Note: If the y-axis is logarithmic  this value cannot be 0.
 * @param animationSpec The animation to provide to the graph when it is created or changed.
 */
@Composable
public fun <X, Y> XYChartScope<X, Y>.StackedAreaChart(
    data: List<MultiPoint<X, Y>>,
    styles: List<StackedAreaStyle>,
    firstBaseline: AreaBaseline.ConstantLine<X, Y>,
    modifier: Modifier = Modifier,
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec
) {
    if (data.isEmpty()) return

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

    GeneralLineChart(
        ListAdapter(0),
        modifier,
        styles[0].lineStyle,
        null,
        styles[0].areaStyle,
        firstBaseline,
        animationSpec
    ) { points: List<Point<X, Y>> ->
        moveTo(scale(points[0]))
        for (index in 1..points.lastIndex) {
            lineTo(scale(points[index]))
        }
    }

    for (series in 1..<data[0].y.size) {
        GeneralLineChart(
            ListAdapter(series),
            modifier,
            styles[series].lineStyle,
            null,
            styles[series].areaStyle,
            AreaBaseline.ArbitraryLine(ListAdapter(series - 1)),
            animationSpec
        ) { points: List<Point<X, Y>> ->
            moveTo(scale(points[0]))
            for (index in 1..points.lastIndex) {
                lineTo(scale(points[index]))
            }
        }
    }
}

/**
 * An XY Chart that draws series as points and lines.
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 * @param data Data series to plot.
 * @param lineStyle Style to use for the line that connects the data points. If null, no line is drawn.
 * @param symbol Composable for the symbol to be shown at each data point.
 * @param areaStyle Style to use for filling the area between the line and a baseline. If null, no area will be drawn.
 * [lineStyle] must also be non-null for the area to be drawn.
 * @param areaBaseline Baseline location for the area. Must be not be null if areaStyle and lineStyle are also not null.
 * If [areaBaseline] is an [AreaBaseline.ArbitraryLine] then the size of the line data must be equal to that of
 * [data], and their x-axis values must match.
 * @param modifier Modifier for the chart.
 */
@Composable
public fun <X, Y> XYChartScope<X, Y>.LineChart(
    data: List<Point<X, Y>>,
    modifier: Modifier = Modifier,
    lineStyle: LineStyle? = null,
    symbol: (@Composable HoverableElementAreaScope.(Point<X, Y>) -> Unit)? = null,
    areaStyle: AreaStyle? = null,
    areaBaseline: AreaBaseline<X, Y>? = null,
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec
) {
    if (data.isEmpty()) return

    if (areaStyle != null) {
        require(lineStyle != null) { "lineStyle must be provided for area charts" }
        require(areaBaseline != null) { "areaBaseline must be provided for area charts" }
        if (areaBaseline is AreaBaseline.ArbitraryLine) {
            require(areaBaseline.values.size == data.size) {
                "baseline values must be the same size as the data"
            }
        }
    }

    GeneralLineChart(
        data,
        modifier,
        lineStyle,
        symbol,
        areaStyle,
        areaBaseline,
        animationSpec
    ) { points: List<Point<X, Y>> ->
        moveTo(scale(points[0]))
        for (index in 1..points.lastIndex) {
            lineTo(scale(points[index]))
        }
    }
}

/**
 * An XY Chart that draws series as points and stairsteps between points.
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 * @param data Data series to plot.
 * @param lineStyle Style to use for the line that connects the data points.
 * @param areaStyle Style to use for filling the area between the line and the 0-cross of the y-axis, or the
 *  y-axis value closest to 0 if the axis does not include 0. If null, no area will be drawn.
 *  [lineStyle] must also be non-null for the area to be drawn.
 * each point having the same x-axis value.
 * @param areaBaseline Baseline location for the area. Must be not be null if areaStyle and lineStyle are also not null.
 * If [areaBaseline] is an [AreaBaseline.ArbitraryLine] then the size of the line data must be equal to that of
 * [data], and their x-axis values must match.
 * @param symbol Composable for the symbol to be shown at each data point.
 * @param modifier Modifier for the chart.
 */
@Composable
public fun <X, Y> XYChartScope<X, Y>.StairstepChart(
    data: List<Point<X, Y>>,
    lineStyle: LineStyle,
    modifier: Modifier = Modifier,
    symbol: (@Composable HoverableElementAreaScope.(Point<X, Y>) -> Unit)? = null,
    areaStyle: AreaStyle? = null,
    areaBaseline: AreaBaseline<X, Y>? = null,
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec
) {
    if (data.isEmpty()) return

    if (areaStyle != null) {
        require(areaBaseline != null) { "areaBaseline must be provided for area charts" }
        if (areaBaseline is AreaBaseline.ArbitraryLine) {
            require(areaBaseline.values.size == data.size) {
                "baseline values must be the same size as the data"
            }
        }
    }

    GeneralLineChart(
        data,
        modifier,
        lineStyle,
        symbol,
        areaStyle,
        areaBaseline,
        animationSpec
    ) { points: List<Point<X, Y>> ->
        // val strokeWidthPx = lineStyle.strokeWidth.toPx()
        var lastPoint = points[0]
        var scaledLastPoint = scale(lastPoint)

        moveTo(scaledLastPoint)
        for (index in 1..points.lastIndex) {
            val midPoint = scale(Point(x = points[index].x, y = lastPoint.y))
            lineTo(midPoint)
            lastPoint = points[index]
            scaledLastPoint = scale(lastPoint)
            lineTo(scaledLastPoint)
        }
    }
}

/**
 * An XY Chart that draws series as points and lines.
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 * @param data Data series to plot.
 * @param lineStyle Style to use for the line that connects the data points. If null, no line is drawn.
 * @param symbol Composable for the symbol to be shown at each data point.
 * @param modifier Modifier for the chart.
 */
@Composable
private fun <X, Y> XYChartScope<X, Y>.GeneralLineChart(
    data: List<Point<X, Y>>,
    modifier: Modifier = Modifier,
    lineStyle: LineStyle? = null,
    symbol: (@Composable HoverableElementAreaScope.(Point<X, Y>) -> Unit)? = null,
    areaStyle: AreaStyle? = null,
    areaBaseline: AreaBaseline<X, Y>? = null,
    animationSpec: AnimationSpec<Float>,
    drawConnectorLine: Path.(points: List<Point<X, Y>>) -> Unit,
) {
    if (data.isEmpty()) return

    // Animation scale factor
    val beta = remember { Animatable(0f) }
    LaunchedEffect(null) { beta.animateTo(1f, animationSpec = animationSpec) }

    Layout(
        modifier = modifier.drawWithContent {
            clipRect(right = size.width * beta.value) { (this@drawWithContent).drawContent() }
        },
        content = {
            Canvas(modifier = Modifier.fillMaxSize()) {
                withTransform({
                    translate(top = size.height)
                    scale(1f, -1f, pivot = Offset(0f, 0f))
                    scale(size.width, size.height, pivot = Offset(0f, 0f))
                }) {
                    var mainLinePath: Path? = null
                    var areaPath: Path? = null
                    if (lineStyle != null) {
                        mainLinePath = Path().apply {
                            drawConnectorLine(data)
                        }

                        if (areaBaseline != null) {
                            areaPath = generateArea(areaBaseline, data, mainLinePath, drawConnectorLine)
                        }
                    }

                    areaStyle?.let {
                        areaPath?.let {
                            drawPath(
                                it,
                                brush = areaStyle.brush,
                                alpha = areaStyle.alpha,
                                style = Fill,
                                colorFilter = areaStyle.colorFilter,
                                blendMode = areaStyle.blendMode
                            )
                        }
                    }

                    lineStyle?.let {
                        // Density and hence toPx() doesn't change with DrawScope scaling, so this approximates it
                        val strokeScale = sqrt(size.width.pow(2) + size.height.pow(2))
                        mainLinePath?.let {
                            drawPath(
                                it,
                                brush = lineStyle.brush,
                                alpha = lineStyle.alpha,
                                style = Stroke(
                                    lineStyle.strokeWidth.toPx() / strokeScale,
                                    pathEffect = lineStyle.pathEffect
                                ),
                                colorFilter = lineStyle.colorFilter,
                                blendMode = lineStyle.blendMode
                            )
                        }
                    }
                }
            }
            Symbols(data, symbol)
        }
    ) { measurables: List<Measurable>, constraints: Constraints ->
        layout(constraints.maxWidth, constraints.maxHeight) {
            measurables.forEach {
                it.measure(constraints).place(0, 0)
            }
        }
    }
}

private fun <X, Y> XYChartScope<X, Y>.generateArea(
    areaBaseline: AreaBaseline<X, Y>,
    data: List<Point<X, Y>>,
    mainLinePath: Path,
    drawConnectorLine: Path.(points: List<Point<X, Y>>) -> Unit
): Path {
    return Path().apply {
        fillType = PathFillType.EvenOdd
        when (areaBaseline) {
            is AreaBaseline.ArbitraryLine -> {
                addPath(mainLinePath)

                // right edge of fill area
                lineTo(scale(Point(data.last().x, areaBaseline.values.last().y)))

                // draw baseline
                drawConnectorLine(areaBaseline.values.reversed())

                // draw left edge of fill area
                lineTo(scale(data.first()))

                close()
            }

            is AreaBaseline.ConstantLine -> {
                addPath(mainLinePath)

                // right edge
                lineTo(scale(Point(data.last().x, areaBaseline.value)))

                // baseline
                lineTo(scale(Point(data.first().x, areaBaseline.value)))

                // left edge
                lineTo(scale(data.first()))

                close()
            }
        }
    }
}

@Composable
private fun <X, Y, P : Point<X, Y>> XYChartScope<X, Y>.Symbols(
    data: List<P>,
    symbol: (@Composable HoverableElementAreaScope.(P) -> Unit)? = null,
) {
    if (symbol != null) {
        Layout(
            modifier = Modifier.fillMaxSize(),
            content = {
                data.indices.forEach {
                    symbol.invoke(this, data[it])
                }
            }
        ) { measurables: List<Measurable>, constraints: Constraints ->
            val size = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())

            layout(constraints.maxWidth, constraints.maxHeight) {
                data.indices.forEach {
                    val p = measurables[it].measure(constraints.copy(minWidth = 0, minHeight = 0))
                    var position = scale(data[it], size)
                    position -= Offset(p.width / 2f, p.height / 2f)
                    p.place(position.x.toInt(), position.y.toInt())
                }
            }
        }
    }
}

/**
 * Scales the [point] to an offset along the x and y axes.
 */
private fun <X, Y> XYChartScope<X, Y>.scale(point: Point<X, Y>): Offset {
    return Offset(xAxisModel.computeOffset(point.x), yAxisModel.computeOffset(point.y))
}

/**
 * Represents a set of points for a [StackedAreaChart].
 *
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 */
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
public data class DefaultMultiPoint<X>(override val x: X, override val y: List<Float>) : MultiPoint<X, Float> {
    override fun accumulateTo(series: Int): Point<X, Float> {
        var sum = 0f
        for (seriesIndex in 0..series) {
            sum += y[seriesIndex]
        }
        return Point(x, sum)
    }
}
