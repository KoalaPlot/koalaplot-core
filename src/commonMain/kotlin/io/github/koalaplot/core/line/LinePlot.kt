@file:Suppress("TooManyFunctions") // TODO Remove once Deprecated functions removed

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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import kotlin.math.min

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
public fun <X, Y> XYGraphScope<X, Y>.LinePlot2(
    data: List<Point<X, Y>>,
    modifier: Modifier = Modifier,
    lineStyle: LineStyle? = null,
    symbol: (@Composable (Point<X, Y>) -> Unit)? = null,
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec,
) {
    LinePlot(data, modifier, lineStyle, { symbol?.invoke(it) }, animationSpec)
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
@Deprecated("Use LinePlot2 instead", replaceWith = ReplaceWith("LinePlot2"))
@Composable
public fun <X, Y> XYGraphScope<X, Y>.LinePlot(
    data: List<Point<X, Y>>,
    modifier: Modifier = Modifier,
    lineStyle: LineStyle? = null,
    symbol: (@Composable HoverableElementAreaScope.(Point<X, Y>) -> Unit)? = null,
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec,
) {
    if (data.isEmpty()) return

    GeneralLinePlot(
        data,
        modifier,
        lineStyle,
        symbol,
        null,
        null,
        animationSpec,
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
public fun <X, Y> XYGraphScope<X, Y>.StairstepPlot2(
    data: List<Point<X, Y>>,
    lineStyle: LineStyle,
    modifier: Modifier = Modifier,
    symbol: (@Composable (Point<X, Y>) -> Unit)? = null,
    areaStyle: AreaStyle? = null,
    areaBaseline: AreaBaseline<X, Y>? = null,
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec,
) {
    StairstepPlot(data, lineStyle, modifier, { symbol?.invoke(it) }, areaStyle, areaBaseline, animationSpec)
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
@Deprecated("Use StairstepPlot2 instead", replaceWith = ReplaceWith("StairstepPlot2"))
@Composable
public fun <X, Y> XYGraphScope<X, Y>.StairstepPlot(
    data: List<Point<X, Y>>,
    lineStyle: LineStyle,
    modifier: Modifier = Modifier,
    symbol: (@Composable HoverableElementAreaScope.(Point<X, Y>) -> Unit)? = null,
    areaStyle: AreaStyle? = null,
    areaBaseline: AreaBaseline<X, Y>? = null,
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec,
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
        animationSpec,
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
 * A [StairstepPlot] that differentiate [lineStyle] & [areaBaseline] at each [Y]-values based on [levelLineStyle].
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 * @param data Data series to plot.
 * @param lineStyle Style to use for the line that connects the data points.
 * @param levelLineStyle Style to use for emphasizing the y-axis values. (Used for line that connects same-level
 *  data points, data that have same value ([Y]) should have the same style).
 * @param cap Choose the [StrokeCap] used for level lines ending.
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
@Suppress("ktlint:compose:param-order-check")
public fun <X, Y> XYGraphScope<X, Y>.StairstepPlot3(
    data: List<Point<X, Y>>,
    lineStyle: LineStyle,
    levelLineStyle: (Y) -> LineStyle,
    modifier: Modifier = Modifier,
    cap: StrokeCap = StrokeCap.Square,
    symbol: @Composable ((Point<X, Y>) -> Unit)? = null,
    areaStyle: ((Y) -> AreaStyle)? = null,
    areaBaseline: AreaBaseline<X, Y>? = null,
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec,
) {
    StairstepPlot(
        data,
        lineStyle,
        levelLineStyle,
        cap,
        modifier,
        { symbol?.invoke(it) },
        areaStyle,
        areaBaseline,
        animationSpec,
    )
}

/**
 * A [StairstepPlot] that differentiate [lineStyle] & [areaBaseline] at each [Y]-values based on [levelLineStyle].
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 * @param data Data series to plot.
 * @param lineStyle Style to use for the line that connects the data points.
 * @param levelLineStyle Style to use for emphasizing the y-axis values. (Used for line that connects same-level
 *  data points, data that have same value ([Y]) should have the same style).
 * @param cap Choose the [StrokeCap] used for level lines ending.
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
@Suppress("ktlint:compose:param-order-check")
@Deprecated("Use StairstepPlot3 instead", replaceWith = ReplaceWith("StairstepPlot3"))
public fun <X, Y> XYGraphScope<X, Y>.StairstepPlot2(
    data: List<Point<X, Y>>,
    lineStyle: LineStyle,
    levelLineStyle: (Y) -> LineStyle,
    cap: StrokeCap = StrokeCap.Square,
    modifier: Modifier = Modifier,
    symbol: @Composable ((Point<X, Y>) -> Unit)? = null,
    areaStyle: ((Y) -> AreaStyle)? = null,
    areaBaseline: AreaBaseline<X, Y>? = null,
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec,
) {
    StairstepPlot(
        data,
        lineStyle,
        levelLineStyle,
        cap,
        modifier,
        { symbol?.invoke(it) },
        areaStyle,
        areaBaseline,
        animationSpec,
    )
}

/**
 * A [StairstepPlot] that differentiate [lineStyle] & [areaBaseline] at each [Y]-values based on [levelLineStyle].
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 * @param data Data series to plot.
 * @param lineStyle Style to use for the line that connects the data points.
 * @param levelLineStyle Style to use for emphasizing the y-axis values. (Used for line that connects same-level
 *  data points, data that have same value ([Y]) should have the same style).
 * @param cap Choose the [StrokeCap] used for level lines ending.
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
@Deprecated("Use StairstepPlot2 instead", replaceWith = ReplaceWith("StairstepPlot2"))
@Composable
@Suppress("LongMethod")
public fun <X, Y> XYGraphScope<X, Y>.StairstepPlot(
    data: List<Point<X, Y>>,
    lineStyle: LineStyle,
    levelLineStyle: (Y) -> LineStyle,
    cap: StrokeCap = StrokeCap.Square,
    modifier: Modifier = Modifier,
    symbol: @Composable (HoverableElementAreaScope.(Point<X, Y>) -> Unit)? = null,
    areaStyle: ((Y) -> AreaStyle)? = null,
    areaBaseline: AreaBaseline<X, Y>? = null,
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec,
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

    // Animation scale factor
    val beta = remember { Animatable(0f) }
    LaunchedEffect(null) { beta.animateTo(1f, animationSpec = animationSpec) }

    Layout(
        modifier = modifier.drawWithContent {
            clipRect(right = size.width * beta.value) { (this@drawWithContent).drawContent() }
        },
        content = {
            Canvas(modifier = Modifier.fillMaxSize()) {
                data class OffsetPoint(
                    val offset: Offset,
                    val point: Point<X, Y>,
                )

                // Order of executing: [onFirstPoint] -> [onMidPoint] -> [onNextPoint] -> [onMidPoint] ...,
                // so `nextPoint` of [onNextPoint] will becomes `lastPoint` of [onMidPoint].
                fun scaledPointsVisitor(
                    points: List<Point<X, Y>>,
                    onFirstPoint: (OffsetPoint) -> Unit = { _ -> },
                    onMidPoint: (lastPoint: OffsetPoint, midPoint: OffsetPoint) -> Unit = { _, _ -> },
                    onNextPoint: (midPoint: OffsetPoint, nextPoint: OffsetPoint) -> Unit = { _, _ -> },
                ) {
                    var lastPoint = points[0]
                    var scaledLastPoint = scale(lastPoint, size)
                    var offsetLastPoint = OffsetPoint(scaledLastPoint, lastPoint)

                    onFirstPoint(offsetLastPoint)
                    for (index in 1..points.lastIndex) {
                        val scaledMidPoint = scale(Point(x = points[index].x, y = lastPoint.y), size)
                        val midPoint = OffsetPoint(scaledMidPoint, lastPoint)
                        onMidPoint(offsetLastPoint, midPoint)
                        lastPoint = points[index]
                        scaledLastPoint = scale(lastPoint, size)
                        offsetLastPoint = OffsetPoint(scaledLastPoint, lastPoint)
                        onNextPoint(midPoint, offsetLastPoint)
                    }
                }
                if (areaBaseline != null && areaStyle != null) {
                    var i = 0
                    var lastPoint: OffsetPoint? = null
                    scaledPointsVisitor(
                        data,
                        onMidPoint = { lp, _ -> lastPoint = lp },
                        onNextPoint = { midPoint, nextPoint ->
                            fillRectangle(
                                leftTop = lastPoint!!.offset,
                                rightBottom = scale(
                                    Point(
                                        nextPoint.point.x,
                                        when (areaBaseline) {
                                            is AreaBaseline.ConstantLine -> areaBaseline.value
                                            is AreaBaseline.ArbitraryLine -> areaBaseline.values[i].y
                                        },
                                    ),
                                    size,
                                ),
                                areaStyle = areaStyle(midPoint.point.y),
                            )
                            i++
                        },
                    )
                }

                // draw vertical lines using lineStyle
                scaledPointsVisitor(
                    data,
                    onNextPoint = { midPoint, p ->
                        with(lineStyle) {
                            drawLine(
                                brush,
                                midPoint.offset,
                                p.offset,
                                strokeWidth.toPx(),
                                Stroke.DefaultCap,
                                pathEffect,
                                alpha,
                                colorFilter,
                                blendMode,
                            )
                        }
                    },
                )
                // draw horizontal lines using levelLineStyle()
                scaledPointsVisitor(
                    data,
                    onMidPoint = { lastPoint, p ->
                        with(levelLineStyle(p.point.y)) {
                            drawLine(brush, lastPoint.offset, p.offset, strokeWidth.toPx(), cap, pathEffect, alpha)
                        }
                    },
                )
            }
            Symbols(data, symbol)
        },
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
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec,
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
                        blendMode = areaStyle.blendMode,
                    )
                }

                lineStyle?.let {
                    drawPath(
                        mainLinePath,
                        brush = lineStyle.brush,
                        alpha = lineStyle.alpha,
                        style = Stroke(lineStyle.strokeWidth.toPx(), pathEffect = lineStyle.pathEffect),
                        colorFilter = lineStyle.colorFilter,
                        blendMode = lineStyle.blendMode,
                    )
                }
            }
            Symbols(data, symbol)
        },
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
    drawConnectorLine: Path.(points: List<Point<X, Y>>, size: Size) -> Unit,
): Path = Path().apply {
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

private fun DrawScope.fillRectangle(
    leftTop: Offset,
    rightBottom: Offset,
    areaStyle: AreaStyle,
) {
    drawRect(
        brush = areaStyle.brush,
        topLeft = leftTop,
        size = (rightBottom - leftTop).run { Size(x, y) },
        alpha = areaStyle.alpha,
        style = Fill,
        colorFilter = areaStyle.colorFilter,
        blendMode = areaStyle.blendMode,
    )
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
            },
        ) { measurables: List<Measurable>, constraints: Constraints ->
            val size = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())

            layout(constraints.maxWidth, constraints.maxHeight) {
                for (index in 0 until min(data.size, measurables.size)) {
                    val p = measurables[index].measure(constraints.copy(minWidth = 0, minHeight = 0))
                    var position = scale(data[index], size)
                    position -= Offset(p.width / 2f, p.height / 2f)
                    p.place(position.x.toInt(), position.y.toInt())
                }
            }
        }
    }
}
