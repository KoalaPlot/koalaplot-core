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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.unit.Constraints
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
 * A Stacked Area Plot is like a line plot but with filled areas between lines, and where each successive line
 * is added to all of the lines before it, so they stack.
 *
 * @param X Data type of x-axis values
 * @param Y Data type of y-axis values
 * @param data List of [MultiPoint] data items for the plot. Each MultiPoint must hold the same number of
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
    modifier: Modifier = Modifier,
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
 * A line plot that draws data as points and lines on an XYGraph.
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 * @param data Data series to plot.
 * @param lineStyle Style to use for the line that connects the data points. If null, no line is drawn.
 * @param symbol Composable for the symbol to be shown at each data point.
 * @param modifier Modifier for the plot.
 */
@Composable
public fun <X, Y> XYGraphScope<X, Y>.LinePlot(
    data: List<Point<X, Y>>,
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
        null,
        null,
        animationSpec
    ) { points: List<Point<X, Y>>, size: Size ->
        moveTo(scale(points[0], size))
        for (index in 1..points.lastIndex) {
            lineTo(scale(points[index], size))
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
public fun <X, Y> XYGraphScope<X, Y>.StairstepPlot(
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

    GeneralLinePlot(
        data,
        modifier,
        lineStyle,
        symbol,
        areaStyle,
        areaBaseline,
        animationSpec
    ) { points: List<Point<X, Y>>, size: Size ->
        var lastPoint = points[0]
        var scaledLastPoint = scale(lastPoint, size)

        moveTo(scaledLastPoint)
        for (index in 1..points.lastIndex) {
            val midPoint = scale(Point(x = points[index].x, y = lastPoint.y), size)
            lineTo(midPoint)
            lastPoint = points[index]
            scaledLastPoint = scale(lastPoint, size)
            lineTo(scaledLastPoint)
        }
    }
}

/**
 * A [StairstepPlot] that differentiate [lineStyle] at each [Y]-values based on [levelLineStyle].
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 * @param data Data series to plot.
 * @param lineStyle Style to use for the line that connects the data points.
 * @param levelLineStyle Style to use for emphasizing the y-axis values. (Used for line that connects same-level
 *  data points, data that have same value ([Y]) should have the same style).
 * @param overlayDrawing If false, each segment is separately drawn, otherwise a general [Path] will be drawn first,
 *  then [levelLineStyle] will be applied over, enabling this give an _antialiasing_ effect, but if [lineStyle] is wider
 *  than [levelLineStyle], the wider part of lineStyle will be visible.
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
public fun <X, Y> XYGraphScope<X, Y>.StairstepPlot(
    data: List<Point<X, Y>>,
    lineStyle: LineStyle,
    levelLineStyle: (Y) -> LineStyle,
    overlayDrawing: Boolean = false,
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

    // Modified version of [GeneralLinePlot].
    fun scaledPointsVisitor(
        points: List<Point<X, Y>>, size: Size,
        onFirstPoint: (Offset, Y) -> Unit,
        onMidPoint: (lastPoint: Offset, Offset, Y) -> Unit,
        onNextPoint: (midPoint: Offset, Offset, Y) -> Unit,
    ) {
        var lastPoint = points[0]
        var scaledLastPoint = scale(lastPoint, size)

        onFirstPoint(scaledLastPoint, lastPoint.y)
        for (index in 1..points.lastIndex) {
            val midPoint = scale(Point(x = points[index].x, y = lastPoint.y), size)
            onMidPoint(scaledLastPoint, midPoint, lastPoint.y)
            lastPoint = points[index]
            scaledLastPoint = scale(lastPoint, size)
            onNextPoint(midPoint, scaledLastPoint, lastPoint.y)
        }
    }
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
                val drawConnectorLine: Path.(points: List<Point<X, Y>>, size: Size) -> Unit = { points: List<Point<X, Y>>, size: Size ->
                    scaledPointsVisitor(
                        points, size,
                        onFirstPoint = { p, _ -> moveTo(p) },
                        onMidPoint = { _, p, _ -> lineTo(p) },
                        onNextPoint = { _, p, _ -> lineTo(p) }
                    )
                }
                val mainLinePath = Path().apply {
                    drawConnectorLine(data, size)
                }
                if (areaBaseline != null && areaStyle != null) {
                    val areaPath = generateArea(
                        areaBaseline, data, mainLinePath, size
                    ) { points, size ->
                        drawConnectorLine(points, size)
                    }
                    drawPath(
                        areaPath,
                        brush = areaStyle.brush,
                        alpha = areaStyle.alpha,
                        style = Fill,
                        colorFilter = areaStyle.colorFilter,
                        blendMode = areaStyle.blendMode
                    )
                }

                if (overlayDrawing) {
                    drawPath(
                        mainLinePath,
                        brush = lineStyle.brush,
                        alpha = lineStyle.alpha,
                        style = Stroke(lineStyle.strokeWidth.toPx(), pathEffect = lineStyle.pathEffect),
                        colorFilter = lineStyle.colorFilter,
                        blendMode = lineStyle.blendMode
                    )
                }
                // drawLevelLines
                scaledPointsVisitor(
                    data, size,
                    onFirstPoint = { _, _ -> },
                    onMidPoint = { lastPoint, p, y ->
                        val lvlLineStyle = levelLineStyle(y)
                        drawLine(lvlLineStyle.brush, lastPoint, p, lvlLineStyle.strokeWidth.toPx())
                    },
                    onNextPoint = { midPoint, p, _ ->
                        if (!overlayDrawing) // draw vertical lines since mainPath has not been drawn.
                            drawLine(lineStyle.brush, midPoint, p, lineStyle.strokeWidth.toPx())
                    }
                )
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

/**
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 * @param data Data series to plot.
 * @param lineStyle Style to use for the line that connects the data points. If null, no line is drawn.
 * @param symbol Composable for the symbol to be shown at each data point.
 * @param modifier Modifier for the chart.
 */
@Composable
internal fun <X, Y> XYGraphScope<X, Y>.GeneralLinePlot(
    data: List<Point<X, Y>>,
    modifier: Modifier = Modifier,
    lineStyle: LineStyle? = null,
    symbol: (@Composable HoverableElementAreaScope.(Point<X, Y>) -> Unit)? = null,
    areaStyle: AreaStyle? = null,
    areaBaseline: AreaBaseline<X, Y>? = null,
    animationSpec: AnimationSpec<Float>,
    drawConnectorLine: Path.(points: List<Point<X, Y>>, size: Size) -> Unit,
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
                val mainLinePath = Path().apply {
                    drawConnectorLine(data, size)
                }

                if (areaBaseline != null && areaStyle != null) {
                    val areaPath = generateArea(areaBaseline, data, mainLinePath, size, drawConnectorLine)
                    drawPath(
                        areaPath,
                        brush = areaStyle.brush,
                        alpha = areaStyle.alpha,
                        style = Fill,
                        colorFilter = areaStyle.colorFilter,
                        blendMode = areaStyle.blendMode
                    )
                }

                lineStyle?.let {
                    drawPath(
                        mainLinePath,
                        brush = lineStyle.brush,
                        alpha = lineStyle.alpha,
                        style = Stroke(lineStyle.strokeWidth.toPx(), pathEffect = lineStyle.pathEffect),
                        colorFilter = lineStyle.colorFilter,
                        blendMode = lineStyle.blendMode
                    )
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

private fun <X, Y> XYGraphScope<X, Y>.generateArea(
    areaBaseline: AreaBaseline<X, Y>,
    data: List<Point<X, Y>>,
    mainLinePath: Path,
    size: Size,
    drawConnectorLine: Path.(points: List<Point<X, Y>>, size: Size) -> Unit
): Path {
    return Path().apply {
        fillType = PathFillType.EvenOdd
        when (areaBaseline) {
            is AreaBaseline.ArbitraryLine -> {
                addPath(mainLinePath)

                // right edge of fill area
                lineTo(scale(areaBaseline.values.last(), size))

                // draw baseline
                drawConnectorLine(areaBaseline.values.reversed(), size)

                // draw left edge of fill area
                lineTo(scale(data.first(), size))

                close()
            }

            is AreaBaseline.ConstantLine -> {
                addPath(mainLinePath)

                // right edge
                lineTo(scale(Point(data.last().x, areaBaseline.value), size))

                // baseline
                lineTo(scale(Point(data.first().x, areaBaseline.value), size))

                // left edge
                lineTo(scale(data.first(), size))

                close()
            }
        }
    }
}

@Composable
private fun <X, Y, P : Point<X, Y>> XYGraphScope<X, Y>.Symbols(
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
