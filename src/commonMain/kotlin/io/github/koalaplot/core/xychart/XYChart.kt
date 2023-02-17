package io.github.koalaplot.core.xychart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultBlendMode
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.koalaplot.core.theme.KoalaPlotTheme
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.util.HoverableElementArea
import io.github.koalaplot.core.util.HoverableElementAreaScope
import io.github.koalaplot.core.util.VerticalRotation
import io.github.koalaplot.core.util.length
import io.github.koalaplot.core.util.rotateVertically
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Provides styling for lines.
 *
 * brush - the color or fill to be applied to the line
 * strokeWidth - stroke width to apply to the line
 * pathEffect - optional effect or pattern to apply to the line
 * alpha - opacity to be applied to the brush from 0.0f to 1.0f representing fully transparent to
 * fully opaque respectively
 * colorFilter - ColorFilter to apply to the brush when drawn into the destination
 * blendMode - the blending algorithm to apply to the brush
 */
public data class LineStyle(
    val brush: Brush,
    val strokeWidth: Dp = 0.dp,
    val pathEffect: PathEffect? = null,
    val alpha: Float = 1.0f,
    val colorFilter: ColorFilter? = null,
    val blendMode: BlendMode = DefaultBlendMode
)

/**
 * Provides a set of X-Y axes and grid for displaying an X-Y plot.
 *
 * @param X The data type for the x-axis
 * @param Y The data type for the y-axis
 * @param xAxisModel x-axis state controlling the display of the axis and coordinate transformation
 * @param yAxisModel y-axis state controlling the display of the axis and coordinate transformation
 * @param xAxisStyle Style for the x-axis
 * @param xAxisLabels Composable to display labels for specific x-axis values
 * @param xAxisTitle Title for the X-axis
 * @param yAxisStyle Style for the y-axis
 * @param yAxisLabels Composable to display labels for specific y-axis values
 * @param yAxisTitle Title for the y-axis
 * @param content The XY Chart content to be displayed, which should include one chart for each
 * series type to be displayed.
 */
@Composable
public fun <X, Y> XYChart(
    xAxisModel: AxisModel<X>,
    yAxisModel: AxisModel<Y>,
    modifier: Modifier = Modifier,
    xAxisStyle: AxisStyle = rememberAxisStyle(),
    xAxisLabels: @Composable (X) -> Unit,
    xAxisTitle: @Composable () -> Unit = {},
    yAxisStyle: AxisStyle = rememberAxisStyle(),
    yAxisLabels: @Composable (Y) -> Unit,
    yAxisTitle: @Composable () -> Unit = {},
    horizontalMajorGridLineStyle: LineStyle? = KoalaPlotTheme.axis.majorGridlineStyle,
    horizontalMinorGridLineStyle: LineStyle? = KoalaPlotTheme.axis.minorGridlineStyle,
    verticalMajorGridLineStyle: LineStyle? = KoalaPlotTheme.axis.majorGridlineStyle,
    verticalMinorGridLineStyle: LineStyle? = KoalaPlotTheme.axis.minorGridlineStyle,
    content: @Composable XYChartScope<X, Y>.() -> Unit
) {
    HoverableElementArea {
        SubcomposeLayout(modifier = modifier) { constraints ->
            val xAxisTitleMeasurable = subcompose("xaxistitle") {
                Box { xAxisTitle() }
            }[0]

            val yAxisTitleMeasurable = subcompose("yaxistitle") {
                Box { yAxisTitle() }
            }[0]

            // Computing the tick values isn't exact because composables can't be measured twice
            // and there's no way to compute the height/width of the axis tick labels before knowing
            // how many tick labels there should be. So we leave them out of the calculation for
            // the axis length constraints, meaning the actual min distance of the axis tick labels
            // may be closer than specified because the actual axis lengths computed later, which
            // take into account the axis label sizes, will be smaller than the lengths computed here.
            val xAxis = AxisDelegate.createHorizontalAxis(
                xAxisModel,
                xAxisStyle,
                (constraints.maxWidth - -yAxisTitleMeasurable.maxIntrinsicWidth(constraints.maxHeight)).toDp()
            )

            val yAxis = AxisDelegate.createVerticalAxis(
                yAxisModel,
                yAxisStyle,
                (constraints.maxHeight - -xAxisTitleMeasurable.maxIntrinsicHeight(constraints.maxWidth)).toDp()
            )

            val xAxisMeasurable = subcompose("xaxis") { Axis(xAxis) }[0]
            val yAxisMeasurable = subcompose("yaxis") { Axis(yAxis) }[0]

            val chartScope = XYChartScopeImpl(xAxisModel, yAxisModel, xAxis, yAxis, this@HoverableElementArea)

            val chartMeasurable = subcompose("chart") {
                Box(
                    modifier = Modifier.clip(RectangleShape)
                        .pointerInput(Unit) {
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                transformAxis(yAxisModel, size.height, centroid.y, pan.y, zoom)
                                transformAxis(xAxisModel, size.width, centroid.x, -pan.x, zoom)
                            }
                        }
                ) { chartScope.content() }
            }[0]

            val measurables = subcompose(Unit) {
                Grid(
                    xAxis,
                    yAxis,
                    horizontalMajorGridLineStyle,
                    horizontalMinorGridLineStyle,
                    verticalMajorGridLineStyle,
                    verticalMinorGridLineStyle
                )

                xAxis.majorTickValues.forEach { Box { xAxisLabels(it) } }
                yAxis.majorTickValues.forEach { Box { yAxisLabels(it) } }
            }

            val measurablesMap = Measurables(
                measurables[0],
                chartMeasurable,
                xAxisMeasurable,
                measurables.subList(1, xAxis.majorTickValues.size + 1),
                xAxisTitleMeasurable,
                yAxisMeasurable,
                measurables.subList(
                    xAxis.majorTickValues.size + 1,
                    xAxis.majorTickValues.size + 1 + yAxis.majorTickValues.size
                ),
                yAxisTitleMeasurable
            )

            with(XYAxisMeasurePolicy(xAxis, yAxis)) {
                measure(measurablesMap, constraints)
            }
        }
    }
}

private fun <T> transformAxis(
    axis: AxisModel<T>,
    length: Int,
    centroid: Float,
    pan: Float,
    zoom: Float
) {
    val pivot = (length.toFloat() - centroid) / length.toFloat()
    axis.zoom(zoom, pivot.coerceIn(0f, 1f))
    axis.pan(pan / length.toFloat())
}

private data class Measurables(
    val grid: Measurable,
    val chart: Measurable,
    val xAxis: Measurable,
    val xAxisLabels: List<Measurable>,
    val xAxisTitle: Measurable,
    val yAxis: Measurable,
    val yAxisLabels: List<Measurable>,
    val yAxisTitle: Measurable,
)

private const val IterationLimit = 10
private const val ChangeThreshold = 0.05

private fun <X, Y> Density.optimizeGraphSize(
    constraints: Constraints,
    m: Measurables,
    xAxis: AxisDelegate<X>,
    yAxis: AxisDelegate<Y>
): IntSize {
    var graphSize = IntSize(constraints.maxWidth, constraints.maxHeight)
    var iterations = 0
    var oldSize: IntSize

    val xAxisHeight = xAxis.thicknessDp.roundToPx()
    val xAxisOffset = xAxis.axisOffset.roundToPx()
    val yAxisOffset = yAxis.axisOffset.roundToPx()

    do {
        oldSize = graphSize

        // Compute x-axis height given graphSize
        val xAxisLabelsHeight = m.xAxisLabels.map {
            it.maxIntrinsicHeight((graphSize.width / m.xAxisLabels.size))
        }.maxOfOrNull { it } ?: 0
        val xAxisTitleHeight = m.xAxisTitle.maxIntrinsicHeight(graphSize.width)
        val yAxisTitleWidth = m.yAxisTitle.maxIntrinsicWidth(graphSize.height)
        val lastYAxisTickLabelHeight = m.yAxisLabels.lastOrNull()?.maxIntrinsicHeight(
            max(0, constraints.maxWidth - graphSize.width - yAxisOffset - yAxisTitleWidth)
        ) ?: 0
        val firstYAxisTickLabelHeight = m.yAxisLabels.firstOrNull()?.maxIntrinsicHeight(
            max(0, constraints.maxWidth - graphSize.width - yAxisOffset - yAxisTitleWidth)
        ) ?: 0

        // Update graphSize height
        graphSize = IntSize(
            width = graphSize.width,
            height = max(
                constraints.maxHeight -
                    max(
                        xAxisHeight - xAxisOffset + xAxisLabelsHeight + xAxisTitleHeight,
                        firstYAxisTickLabelHeight / 2
                    ) - lastYAxisTickLabelHeight / 2,
                0
            )
        )

        // Compute y-axis width given graphSize
        val yAxisLabelsWidth = m.yAxisLabels.map {
            it.maxIntrinsicWidth(graphSize.height / m.yAxisLabels.size)
        }.maxOfOrNull { it } ?: 0
        val lastXAxisTickLabelWidth = m.xAxisLabels.lastOrNull()?.maxIntrinsicWidth(
            max(
                0,
                constraints.maxHeight - graphSize.height - xAxisHeight + xAxisOffset - xAxisTitleHeight
            )
        ) ?: 0
        val firstXAxisTickLabelWidth = m.xAxisLabels.firstOrNull()?.maxIntrinsicWidth(
            max(
                0,
                constraints.maxHeight - graphSize.height - xAxisHeight + xAxisOffset - xAxisTitleHeight
            )
        ) ?: 0

        // Update graphSize width
        graphSize = IntSize(
            width = constraints.maxWidth - max(
                yAxisOffset + yAxisLabelsWidth + yAxisTitleWidth,
                firstXAxisTickLabelWidth / 2
            ) - lastXAxisTickLabelWidth / 2,
            height = graphSize.height
        )

        iterations++
    } while (iterations < IterationLimit &&
        abs(graphSize.length() - oldSize.length()) / oldSize.length() > ChangeThreshold
    )

    return graphSize
}

private class XYAxisMeasurePolicy<X, Y>(
    val xAxis: AxisDelegate<X>,
    val yAxis: AxisDelegate<Y>
) {
    fun MeasureScope.measure(
        m: Measurables,
        constraints: Constraints
    ): MeasureResult {
        val graphSize = optimizeGraphSize(constraints, m, xAxis, yAxis)

        val xAxisPlaceable = m.xAxis.measure(Constraints.fixedWidth(graphSize.width))
        val xAxisLabelPlaceables = m.xAxisLabels.map {
            it.measure(Constraints(maxWidth = graphSize.width / m.xAxisLabels.size))
        }
        val xAxisTitlePlaceable = m.xAxisTitle.measure(Constraints(maxWidth = graphSize.width))
        val xAxisLabelHeight = xAxisLabelPlaceables.maxOfOrNull { it.height } ?: 0

        val yAxisPlaceable = m.yAxis.measure(Constraints.fixedHeight(graphSize.height))
        val yAxisLabelPlaceables = m.yAxisLabels.map {
            it.measure(Constraints(maxHeight = graphSize.height / m.yAxisLabels.size))
        }
        val yAxisTitlePlaceable = m.yAxisTitle.measure(Constraints(maxHeight = graphSize.height))
        val yAxisLabelWidth = yAxisLabelPlaceables.maxOfOrNull { it.width } ?: 0

        var plotArea = IntRect(
            left = yAxisTitlePlaceable.width + yAxisLabelWidth + yAxis.axisOffset.roundToPx(),
            right = 0,
            top = (yAxisLabelPlaceables.lastOrNull()?.height ?: 0) / 2,
            bottom = 0
        )
        plotArea = plotArea.copy(
            right = plotArea.left + graphSize.width,
            bottom = plotArea.top + graphSize.height
        )

        val gridPlaceable = m.grid.measure(Constraints.fixed(plotArea.width, plotArea.height))
        val chartPlaceable = m.chart.measure(Constraints.fixed(plotArea.width, plotArea.height))

        return layout(constraints.maxWidth, constraints.maxHeight) {
            gridPlaceable.place(plotArea.left, plotArea.top)

            xAxisTitlePlaceable.place(
                plotArea.left + plotArea.width / 2 - xAxisTitlePlaceable.width / 2,
                plotArea.bottom + xAxisPlaceable.height - xAxis.axisOffset.roundToPx() +
                    xAxisLabelHeight
            )

            xAxisLabelPlaceables.forEachIndexed { index, placeable ->
                placeable.place(
                    (
                        plotArea.left +
                            xAxis.majorTickOffsets[index] * plotArea.width -
                            placeable.width / 2.0
                        ).roundToInt(),
                    plotArea.bottom + xAxisPlaceable.height - xAxis.axisOffset.roundToPx()
                )
            }

            yAxisTitlePlaceable.place(
                0,
                plotArea.top + plotArea.height / 2 - yAxisTitlePlaceable.height / 2
            )

            yAxisLabelPlaceables.forEachIndexed { index, placeable ->
                placeable.place(
                    plotArea.left - yAxis.axisOffset.roundToPx() - placeable.width,
                    (
                        plotArea.bottom -
                            yAxis.majorTickOffsets[index] * plotArea.height -
                            placeable.height / 2
                        ).toInt()
                )
            }

            chartPlaceable.place(plotArea.left, plotArea.top)

            yAxisPlaceable.place(
                plotArea.left - yAxis.axisOffset.roundToPx(),
                plotArea.top
            )
            xAxisPlaceable.place(
                plotArea.left,
                plotArea.bottom - xAxis.axisOffset.roundToPx()
            )
        }
    }
}

/**
 * A scope for XY plots providing axis and state context.
 */
public interface XYChartScope<X, Y> : HoverableElementAreaScope {
    public val xAxisModel: AxisModel<X>
    public val yAxisModel: AxisModel<Y>
    public val xAxisState: AxisState
    public val yAxisState: AxisState
}

private class XYChartScopeImpl<X, Y>(
    override val xAxisModel: AxisModel<X>,
    override val yAxisModel: AxisModel<Y>,
    override val xAxisState: AxisState,
    override val yAxisState: AxisState,
    val hoverableElementAreaScope: HoverableElementAreaScope
) : XYChartScope<X, Y>, HoverableElementAreaScope by hoverableElementAreaScope

@Composable
private fun Grid(
    xAxisState: AxisState,
    yAxisState: AxisState,
    horizontalMajorGridLineStyle: LineStyle? = null,
    horizontalMinorGridLineStyle: LineStyle? = null,
    verticalMajorGridLineStyle: LineStyle? = null,
    verticalMinorGridLineStyle: LineStyle? = null,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawVerticalGridLines(
            xAxisState.majorTickOffsets,
            size.width,
            verticalMajorGridLineStyle
        )

        drawVerticalGridLines(
            xAxisState.minorTickOffsets,
            size.width,
            verticalMinorGridLineStyle
        )

        drawHorizontalGridLines(
            yAxisState.majorTickOffsets,
            size.height,
            horizontalMajorGridLineStyle
        )

        drawHorizontalGridLines(
            yAxisState.minorTickOffsets,
            size.height,
            horizontalMinorGridLineStyle
        )
    }
}

private fun DrawScope.drawVerticalGridLines(
    values: List<Float>,
    scale: Float,
    style: LineStyle?
) {
    if (style != null) {
        values.forEach {
            drawGridLine(
                style,
                start = Offset(it * scale, 0f),
                end = Offset(it * scale, size.height)
            )
        }
    }
}

private fun DrawScope.drawHorizontalGridLines(
    values: List<Float>,
    scale: Float,
    style: LineStyle?
) {
    if (style != null) {
        values.forEach {
            drawGridLine(
                style,
                start = Offset(0f, scale - it * scale),
                end = Offset(size.width, scale - it * scale)
            )
        }
    }
}

private fun DrawScope.drawGridLine(gridLineStyle: LineStyle?, start: Offset, end: Offset) {
    if (gridLineStyle != null) {
        with(gridLineStyle) {
            drawLine(
                start = start,
                end = end,
                brush = brush,
                strokeWidth = strokeWidth.toPx(),
                pathEffect = pathEffect,
                alpha = alpha,
                colorFilter = colorFilter,
                blendMode = blendMode
            )
        }
    }
}

/**
 * An XYChart overload that takes Strings for axis labels and titles instead of Composables for use cases where
 * custom styling is not required.
 *
 * Provides a set of X-Y axes and grid for displaying an X-Y plot.
 *
 * @param X The data type for the x-axis
 * @param Y The data type for the y-axis
 * @param xAxisModel x-axis state controlling the display of the axis and coordinate transformation
 * @param yAxisModel y-axis state controlling the display of the axis and coordinate transformation
 * @param xAxisStyle Style for the x-axis
 * @param xAxisLabels String factory of x-axis label Strings
 * @param xAxisTitle Title for the X-axis
 * @param yAxisStyle Style for the y-axis
 * @param yAxisLabels String factory of y-axis label Strings
 * @param yAxisTitle Title for the y-axis
 * @param content The XY Chart content to be displayed, which should include one chart for each
 * series type to be displayed.
 */
@ExperimentalKoalaPlotApi
@Composable
public fun <X, Y> XYChart(
    xAxisModel: AxisModel<X>,
    yAxisModel: AxisModel<Y>,
    modifier: Modifier = Modifier,
    xAxisStyle: AxisStyle = rememberAxisStyle(),
    xAxisLabels: (X) -> String = { it.toString() },
    xAxisTitle: String = "",
    yAxisStyle: AxisStyle = rememberAxisStyle(),
    yAxisLabels: (Y) -> String = { it.toString() },
    yAxisTitle: String = "",
    horizontalMajorGridLineStyle: LineStyle? = KoalaPlotTheme.axis.majorGridlineStyle,
    horizontalMinorGridLineStyle: LineStyle? = KoalaPlotTheme.axis.minorGridlineStyle,
    verticalMajorGridLineStyle: LineStyle? = KoalaPlotTheme.axis.majorGridlineStyle,
    verticalMinorGridLineStyle: LineStyle? = KoalaPlotTheme.axis.minorGridlineStyle,
    content: @Composable XYChartScope<X, Y>.() -> Unit
) {
    XYChart(
        xAxisModel,
        yAxisModel,
        modifier,
        xAxisStyle,
        xAxisLabels = {
            Text(
                xAxisLabels(it),
                color = xAxisStyle.color,
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(top = 2.dp)
            )
        },
        xAxisTitle = {
            Text(
                xAxisTitle,
                color = xAxisStyle.color,
                style = MaterialTheme.typography.subtitle1
            )
        },
        yAxisStyle,
        yAxisLabels = {
            Text(
                yAxisLabels(it),
                color = yAxisStyle.color,
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(top = 2.dp)
            )
        },
        yAxisTitle = {
            Text(
                yAxisTitle,
                color = yAxisStyle.color,
                style = MaterialTheme.typography.subtitle1,
                modifier = Modifier.rotateVertically(VerticalRotation.COUNTER_CLOCKWISE)
                    .padding(bottom = KoalaPlotTheme.sizes.gap),
            )
        },
        horizontalMajorGridLineStyle,
        horizontalMinorGridLineStyle,
        verticalMajorGridLineStyle,
        verticalMinorGridLineStyle,
        content
    )
}
