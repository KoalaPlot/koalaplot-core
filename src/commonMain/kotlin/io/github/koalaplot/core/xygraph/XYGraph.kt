package io.github.koalaplot.core.xygraph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.koalaplot.core.style.KoalaPlotTheme
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.util.Deg2Rad
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.util.HoverableElementArea
import io.github.koalaplot.core.util.HoverableElementAreaScope
import io.github.koalaplot.core.util.Vector
import io.github.koalaplot.core.util.VerticalRotation
import io.github.koalaplot.core.util.length
import io.github.koalaplot.core.util.lineDistance
import io.github.koalaplot.core.util.rotate
import io.github.koalaplot.core.util.rotateVertically
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Provides a set of X-Y axes and grid for displaying X-Y plots.
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
 * @param panZoomEnabled True if the plot can be panned and zoomed, false to disable. Enabling panning and zooming may
 * interfere with scrolling a parent container if the drag point is on the plot.
 * @param content The content to be displayed, which should include one plot for each series to be
 * plotted on this XYGraph.
 */
@Composable
public fun <X, Y> XYGraph(
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
    panZoomEnabled: Boolean = true,
    content: @Composable XYGraphScope<X, Y>.() -> Unit
) {
    val panZoomModifier = if (panZoomEnabled) {
        Modifier.pointerInput(xAxisModel, yAxisModel) {
            detectTransformGestures { centroid, pan, zoom, _ ->
                transformAxis(yAxisModel, size.height, centroid.y, pan.y, zoom)
                transformAxis(xAxisModel, size.width, centroid.x, -pan.x, zoom)
            }
        }
    } else {
        Modifier
    }

    HoverableElementArea(modifier = modifier) {
        SubcomposeLayout { constraints ->
            val xAxisTitleMeasurable = subcompose("xaxistitle") {
                Box(modifier = Modifier.fillMaxWidth()) { xAxisTitle() }
            }[0]

            val yAxisTitleMeasurable = subcompose("yaxistitle") {
                Box(modifier = Modifier.fillMaxHeight()) { yAxisTitle() }
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

            val chartScope = XYGraphScopeImpl(xAxisModel, yAxisModel, xAxis, yAxis, this@HoverableElementArea)

            val chartMeasurable = subcompose("chart") {
                Box(
                    modifier = Modifier.clip(RectangleShape).then(panZoomModifier)
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

                xAxis.majorTickValues.forEach {
                    Box(modifier = Modifier.rotate(-xAxisStyle.labelRotation.toFloat())) { xAxisLabels(it) }
                }
                yAxis.majorTickValues.forEach {
                    Box(modifier = Modifier.rotate(-yAxisStyle.labelRotation.toFloat())) { yAxisLabels(it) }
                }
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
private const val ChangeThreshold = 0.01 // Threshold for relative size changes to stop iterating

/**
 * Calculates the heights of the x-axis labels constrained by the space available to them.
 * @receiver The measurables for the labels
 * @param labelRotation The angle, in degrees, at which the labels will be drawn
 * @param tickSpacing The spacing between adjacent ticks on the axis
 * @param verticalSpace The amount of vertical space available for the labels.
 * @param measure Measures the measurable given the width constraint and returns an object that can be used to get
 * the height for the measurable using the [getHeight] function.
 * @param getHeight Gets the height for the object returned by [measure]
 */
private fun <T> List<Measurable>.calcXAxisLabelWidthConstraints(
    labelRotation: Int,
    tickSpacing: Int,
    verticalSpace: Int,
    measure: (m: Measurable, width: Int) -> T,
    getHeight: (T) -> Int
): List<T> = buildList {
    var lastHeight = 0
    this@calcXAxisLabelWidthConstraints.forEachIndexed { index, label ->
        val w = if (labelRotation == 0) {
            tickSpacing
        } else {
            if (index == 0) {
                max((verticalSpace / sin(labelRotation * Deg2Rad)).roundToInt(), 0)
            } else { // potentially constrain the width so the label does not overlap the previous label
                val origin = Vector(0f, 0f)
                val p1 = Vector(0f, lastHeight.toFloat())
                val p2 = p1.rotate(-labelRotation.toFloat())
                val (distance, intersection) = lineDistance(
                    origin,
                    p2,
                    Vector(tickSpacing.toFloat(), 0f)
                )

                // Check if the intersection point interferes with the previous label
                // The origin, p2, and intersection are on the same line
                // So check if intersection is closer to p1 than p2
                if (intersection.norm() < p2.norm()) {
                    distance.toInt() // limit allowed width to position of previous label
                } else {
                    max((verticalSpace / sin(labelRotation * Deg2Rad)).roundToInt(), 0)
                }
            }
        }

        val t = measure(label, w)
        add(t)
        lastHeight = getHeight(t)
    }
}

/**
 * Class that holds the dimensions of different areas of the overall Chart to be used for computing the graph area,
 * with helper functions for recomputing the areas.
 */
private data class ChartAreas(
    private val constraints: Constraints,
    val yAxisOffset: Int = 0,
    val xAxisHeight: Int = 0,
    val xAxisOffset: Int = 0,
    val xAxisTitleHeight: Int = 0,
    val xAxisLabelAreaHeight: Int = 0,
    val xAxisFirstLabelExtensionWidth: Int = 0,
    val xAxisLastLabelExtensionWidth: Int = 0,
    val yAxisTitleWidth: Int = 0,
    val yAxisLabelAreaWidth: Int = 0,
    val yAxisFirstLabelExtensionHeight: Int = 0,
    val yAxisLastLabelExtensionHeight: Int = 0
) {
    val graphSize: IntSize by lazy {
        IntSize(
            (
                constraints.maxWidth -
                    max(yAxisTitleWidth + yAxisLabelAreaWidth + yAxisOffset, xAxisFirstLabelExtensionWidth) -
                    xAxisLastLabelExtensionWidth
                ).coerceAtLeast(0),
            (
                constraints.maxHeight -
                    max(
                        xAxisTitleHeight + xAxisLabelAreaHeight + xAxisHeight - xAxisOffset,
                        yAxisFirstLabelExtensionHeight
                    ) -
                    yAxisLastLabelExtensionHeight
                ).coerceAtLeast(0)
        )
    }

    /**
     * The location of the graph area within the chart.
     */
    val graphArea: IntRect by lazy {
        IntRect(
            IntOffset(
                max(yAxisTitleWidth + yAxisLabelAreaWidth + yAxisOffset, xAxisFirstLabelExtensionWidth),
                yAxisLastLabelExtensionHeight
            ),
            graphSize
        )
    }

    /**
     * Calculates the x-axis tick spacing based on the width of the graph size and the provided [number] of ticks.
     */
    fun xTickSpacing(number: Int): Int {
        return graphSize.width / number.coerceAtLeast(1)
    }

    private fun maxFirstLabelWidth(majorTickOffsets: List<Float>): Int {
        return if (majorTickOffsets.size > 1) {
            (graphSize.width * (majorTickOffsets[1] - majorTickOffsets[0])).toInt()
        } else {
            graphSize.width
        }
    }

    private fun maxLastLabelWidth(majorTickOffsets: List<Float>): Int {
        return if (majorTickOffsets.size > 1) {
            (
                graphSize.width * (majorTickOffsets.last() - majorTickOffsets[majorTickOffsets.lastIndex - 1])
                ).toInt()
        } else {
            graphSize.width
        }
    }

    /**
     * Returns a copy of this ChartAreas after setting the [xAxisLabelAreaHeight],
     * [xAxisFirstLabelExtensionWidth], and [xAxisLastLabelExtensionWidth] based on the
     * x-axis labels and their rotation angle.
     */
    fun withComputedXAxisLabelAreas(
        majorTickOffsets: List<Float>,
        xAxisLabels: List<Measurable>,
        rotation: Int
    ): ChartAreas {
        val xAxisLabelHeights = xAxisLabels.calcXAxisLabelWidthConstraints(
            rotation,
            xTickSpacing(xAxisLabels.size),
            constraints.maxHeight - graphSize.height - xAxisHeight - xAxisTitleHeight + xAxisOffset,
            { meas, w -> meas.maxIntrinsicHeight(w) },
            { h -> h }
        )

        val labelAreas = xAxisLabels.mapIndexed { index, label ->
            RotatedComposableAreaDelegate(
                IntSize(label.maxIntrinsicWidth(xAxisLabelHeights[index]), xAxisLabelHeights[index]),
                rotation.toFloat()
            )
        }

        fun firstTickOffset(): Int {
            return if (majorTickOffsets.isNotEmpty()) {
                (graphSize.width * majorTickOffsets[0]).toInt()
            } else {
                graphSize.width / 2
            }
        }

        fun lastTickOffset(): Int {
            return if (majorTickOffsets.isNotEmpty()) {
                (graphSize.width * (1 - majorTickOffsets.last())).toInt()
            } else {
                graphSize.width / 2
            }
        }

        return copy(
            xAxisLabelAreaHeight = labelAreas.maxOfOrNull { it.height } ?: 0,
            xAxisFirstLabelExtensionWidth = if (rotation == 0) {
                // If the major ticks are closer together than label width the actual space provided for
                // the label will be less (it will overflow or clip).
                // The allocated space is approximated by maxFirstLabelWidth().
                (
                    min(
                        maxFirstLabelWidth(majorTickOffsets) / 2,
                        labelAreas.firstOrNull()?.widthLeft(AnchorPoint.TopCenter) ?: 0
                    ) - firstTickOffset()
                    ).coerceAtLeast(0)
            } else {
                // 2nd (or subsequent) label may possibly extend further left than the first label due to the angle
                majorTickOffsets.mapIndexed { index, fl ->
                    (labelAreas[index].widthLeft(AnchorPoint.RightMiddle) - (graphSize.width * fl).toInt())
                        .coerceAtLeast(0)
                }.maxOrNull() ?: 0
            },
            xAxisLastLabelExtensionWidth = if (rotation == 0) {
                (
                    min(
                        maxLastLabelWidth(majorTickOffsets) / 2,
                        labelAreas.lastOrNull()?.widthRight(AnchorPoint.TopCenter) ?: 0
                    ) - lastTickOffset()
                    ).coerceAtLeast(0)
            } else {
                (
                    (labelAreas.lastOrNull()?.widthRight(AnchorPoint.RightMiddle) ?: 0) - lastTickOffset()
                    ).coerceAtLeast(0)
            }
        )
    }

    /**
     * Returns a copy of this ChartAreas after setting the [yAxisLabelAreaWidth],
     * [yAxisFirstLabelExtensionHeight], and [yAxisLastLabelExtensionHeight] based on the
     * y-axis labels and their rotation angle.
     */
    fun withComputedYAxisLabelAreas(yAxisLabels: List<Measurable>, rotation: Int): ChartAreas {
        val yAxisLabelAreas = yAxisLabels.map {
            val width = it.maxIntrinsicWidth(graphSize.height / yAxisLabels.size)

            RotatedComposableAreaDelegate(
                IntSize(width, it.maxIntrinsicHeight(width)),
                rotation.toFloat()
            )
        }

        return copy(
            yAxisLabelAreaWidth = yAxisLabelAreas.maxOfOrNull { it.width } ?: 0,
            yAxisFirstLabelExtensionHeight = if (rotation == 90) {
                yAxisLabelAreas.firstOrNull()?.heightBelow(AnchorPoint.BottomCenter) ?: 0
            } else {
                yAxisLabelAreas.firstOrNull()?.heightBelow(AnchorPoint.RightMiddle) ?: 0
            },
            yAxisLastLabelExtensionHeight = if (rotation == 90) {
                yAxisLabelAreas.lastOrNull()?.heightAbove(AnchorPoint.BottomCenter) ?: 0
            } else {
                yAxisLabelAreas.lastOrNull()?.heightAbove(AnchorPoint.RightMiddle) ?: 0
            }
        )
    }
}

private fun <X, Y> Density.optimizeGraphSize(
    constraints: Constraints,
    m: Measurables,
    xAxis: AxisDelegate<X>,
    yAxis: AxisDelegate<Y>
): ChartAreas {
    var iterations = 0
    var oldSize: IntSize

    val xAxisHeight = xAxis.thicknessDp.roundToPx()
    val xAxisOffset = xAxis.axisOffset.roundToPx()
    val yAxisOffset = yAxis.axisOffset.roundToPx()
    var chartAreas = ChartAreas(constraints, yAxisOffset, xAxisHeight, xAxisOffset)

    do {
        oldSize = chartAreas.graphSize

        chartAreas = chartAreas.copy(
            xAxisTitleHeight = m.xAxisTitle.maxIntrinsicHeight(chartAreas.graphSize.width),
            yAxisTitleWidth = m.yAxisTitle.maxIntrinsicWidth(chartAreas.graphSize.height)
        )

        chartAreas =
            chartAreas.withComputedXAxisLabelAreas(xAxis.majorTickOffsets, m.xAxisLabels, xAxis.style.labelRotation)
        chartAreas = chartAreas.withComputedYAxisLabelAreas(m.yAxisLabels, yAxis.style.labelRotation)

        iterations++
    } while (iterations < IterationLimit &&
        abs(chartAreas.graphSize.length() - oldSize.length()) / oldSize.length() > ChangeThreshold
    )

    return chartAreas
}

private class XYAxisMeasurePolicy<X, Y>(
    val xAxis: AxisDelegate<X>,
    val yAxis: AxisDelegate<Y>
) {
    fun MeasureScope.measure(m: Measurables, constraints: Constraints): MeasureResult {
        val chartAreas = optimizeGraphSize(constraints, m, xAxis, yAxis)

        val yAxisLabelPlaceableDelegates: List<RotatedPlaceableDelegate> = m.yAxisLabels.map {
            RotatedPlaceableDelegate(
                it.measure(Constraints(maxHeight = chartAreas.graphSize.height / m.yAxisLabels.size)),
                -yAxis.style.labelRotation.toFloat()
            )
        }

        val xAxisPlaceable = m.xAxis.measure(Constraints.fixedWidth(chartAreas.graphSize.width))
        val xAxisTitlePlaceable = m.xAxisTitle.measure(Constraints(maxWidth = chartAreas.graphSize.width))

        val xAxisLabelPlaceableDelegates = m.xAxisLabels.calcXAxisLabelWidthConstraints(
            xAxis.style.labelRotation,
            chartAreas.graphSize.width / m.xAxisLabels.size,
            chartAreas.xAxisLabelAreaHeight,
            { meas, w -> meas.measure(Constraints(maxWidth = w)) },
            { placeable -> placeable.height }
        ).map {
            RotatedPlaceableDelegate(it, -xAxis.style.labelRotation.toFloat())
        }

        val yAxisTitlePlaceable = m.yAxisTitle.measure(Constraints(maxHeight = chartAreas.graphSize.height))
        val yAxisPlaceable = m.yAxis.measure(Constraints.fixedHeight(chartAreas.graphSize.height))

        return layout(constraints.maxWidth, constraints.maxHeight) {
            m.grid.measure(Constraints.fixed(chartAreas.graphSize.width, chartAreas.graphSize.height)).place(
                chartAreas.graphArea.left,
                chartAreas.graphArea.top
            )

            xAxisTitlePlaceable.place(
                chartAreas.graphArea.left + chartAreas.graphArea.width / 2 - xAxisTitlePlaceable.width / 2,
                chartAreas.graphArea.bottom + xAxisPlaceable.height - xAxis.axisOffset.roundToPx() +
                    chartAreas.xAxisLabelAreaHeight
            )

            xAxisLabelPlaceableDelegates.forEachIndexed { index, placeable ->
                val anchor = if (xAxis.style.labelRotation == 0) {
                    AnchorPoint.TopCenter
                } else {
                    AnchorPoint.RightMiddle
                }
                with(placeable) {
                    place(
                        (chartAreas.graphArea.left + xAxis.majorTickOffsets[index] * chartAreas.graphArea.width)
                            .toInt(),
                        chartAreas.graphArea.bottom + xAxisPlaceable.height - xAxis.axisOffset.roundToPx(),
                        anchor
                    )
                }
            }

            yAxisTitlePlaceable.place(
                chartAreas.graphArea.left - yAxis.axisOffset.roundToPx() -
                    chartAreas.yAxisLabelAreaWidth - yAxisTitlePlaceable.width,
                chartAreas.graphArea.top + chartAreas.graphArea.height / 2 - yAxisTitlePlaceable.height / 2
            )

            yAxisLabelPlaceableDelegates.forEachIndexed { index, placeable ->
                @Suppress("MagicNumber")
                val anchor = if (yAxis.style.labelRotation == 90) {
                    AnchorPoint.BottomCenter
                } else {
                    AnchorPoint.RightMiddle
                }

                with(placeable) {
                    place(
                        chartAreas.graphArea.left - yAxis.axisOffset.roundToPx(),
                        (
                            chartAreas.graphArea.bottom - yAxis.majorTickOffsets[index] * chartAreas.graphArea.height
                            ).toInt(),
                        anchor
                    )
                }
            }

            m.chart.measure(Constraints.fixed(chartAreas.graphArea.width, chartAreas.graphArea.height))
                .place(chartAreas.graphArea.left, chartAreas.graphArea.top)

            yAxisPlaceable.place(chartAreas.graphArea.left - yAxis.axisOffset.roundToPx(), chartAreas.graphArea.top)
            xAxisPlaceable.place(chartAreas.graphArea.left, chartAreas.graphArea.bottom - xAxis.axisOffset.roundToPx())
        }
    }
}

/**
 * A scope for XY plots providing axis and state context.
 */
public interface XYGraphScope<X, Y> : HoverableElementAreaScope {
    public val xAxisModel: AxisModel<X>
    public val yAxisModel: AxisModel<Y>
    public val xAxisState: AxisState
    public val yAxisState: AxisState

    /**
     * Transforms [point] from [AxisModel] space to display coordinates provided a plot area [size].
     */
    public fun scale(
        point: Point<X, Y>,
        size: Size
    ): Offset {
        return Offset(
            xAxisModel.computeOffset(point.x) * size.width,
            size.height - yAxisModel.computeOffset(point.y) * size.height
        )
    }
}

private class XYGraphScopeImpl<X, Y>(
    override val xAxisModel: AxisModel<X>,
    override val yAxisModel: AxisModel<Y>,
    override val xAxisState: AxisState,
    override val yAxisState: AxisState,
    val hoverableElementAreaScope: HoverableElementAreaScope
) : XYGraphScope<X, Y>, HoverableElementAreaScope by hoverableElementAreaScope

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
 * An XYGraph overload that takes Strings for axis labels and titles instead of Composables for use cases where
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
 * @param panZoomEnabled True if the plot can be panned and zoomed, false to disable. Enabling panning and zooming may
 * interfere with scrolling a parent container if the drag point is on the plot.
 * @param content The content to be displayed within this graph, which should include one plot for each
 * data series to be plotted.
 */
@ExperimentalKoalaPlotApi
@Composable
public fun <X, Y> XYGraph(
    xAxisModel: AxisModel<X>,
    yAxisModel: AxisModel<Y>,
    modifier: Modifier = Modifier,
    xAxisStyle: AxisStyle = rememberAxisStyle(),
    xAxisLabels: (X) -> String = { it.toString() },
    xAxisTitle: String? = null,
    yAxisStyle: AxisStyle = rememberAxisStyle(),
    yAxisLabels: (Y) -> String = { it.toString() },
    yAxisTitle: String? = null,
    horizontalMajorGridLineStyle: LineStyle? = KoalaPlotTheme.axis.majorGridlineStyle,
    horizontalMinorGridLineStyle: LineStyle? = KoalaPlotTheme.axis.minorGridlineStyle,
    verticalMajorGridLineStyle: LineStyle? = KoalaPlotTheme.axis.majorGridlineStyle,
    verticalMinorGridLineStyle: LineStyle? = KoalaPlotTheme.axis.minorGridlineStyle,
    panZoomEnabled: Boolean = true,
    content: @Composable XYGraphScope<X, Y>.() -> Unit
) {
    XYGraph(
        xAxisModel,
        yAxisModel,
        modifier,
        xAxisStyle,
        xAxisLabels = {
            Text(
                xAxisLabels(it),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp)
            )
        },
        xAxisTitle = {
            if (xAxisTitle != null) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        xAxisTitle,
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        },
        yAxisStyle,
        yAxisLabels = {
            Text(
                yAxisLabels(it),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp)
            )
        },
        yAxisTitle = {
            if (yAxisTitle != null) {
                Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text(
                        yAxisTitle,
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.rotateVertically(VerticalRotation.COUNTER_CLOCKWISE)
                            .padding(bottom = KoalaPlotTheme.sizes.gap),
                    )
                }
            }
        },
        horizontalMajorGridLineStyle,
        horizontalMinorGridLineStyle,
        verticalMajorGridLineStyle,
        verticalMinorGridLineStyle,
        panZoomEnabled,
        content
    )
}
