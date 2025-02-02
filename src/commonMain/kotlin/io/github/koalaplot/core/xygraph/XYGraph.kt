@file:Suppress("TooManyFunctions") // expected

package io.github.koalaplot.core.xygraph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.*
import androidx.compose.ui.unit.*
import io.github.koalaplot.core.style.KoalaPlotTheme
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.util.*
import kotlin.math.abs
import kotlin.math.max
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
 * @param panEnabled True if the plot can be panned, false to disable. Enabling panning may
 * interfere with scrolling a parent container if the drag point is on the plot.
 * @param zoomEnabled True if the plot can be zoomed, false to disable. Enabling zooming may
 * interfere with scrolling a parent container if the drag point is on the plot.
 * @param allowIndependentZoom True if the zoom can be either only on the X axis, or only on the Y axis,
 * or independently on the X and Y axes at the same time (the behavior depends on the target platform),
 * False if the total zoom factor must be used.
 *
 * Behavior for Android and iOS:
 * True does not mean getting independent zoom coefficients simultaneously for each axis,
 * if the zoom was initiated:
 * - horizontally - the zoom coefficient will change only for the X axis,
 * - vertically - the zoom coefficient will change only for the Y axis.
 *
 * Behavior for Desktop platforms: (EXPERIMENTAL!)
 * True means getting independent zoom coefficients simultaneously or separately for each axis,
 * if the zoom was initiated:
 * - horizontally - the zoom coefficient will change only for the X axis,
 * - vertically - the zoom coefficient will change only for the Y axis,
 * - diagonally - the zoom coefficient will change along the axes X and Y at the same time
 *
 * Behavior for JS and wasmJS: (EXPERIMENTAL!)
 * True means getting independent zoom coefficients simultaneously or separately for each axis,
 * if the zoom was initiated:
 * - horizontally - the zoom coefficient will change only for the X axis,
 * - vertically - the zoom coefficient will change only for the Y axis,
 * - diagonally - the zoom coefficient will change along the axes X and Y at the same time.
 *
 * JS and wasmJS have slight differences in response behavior (for example, zoom coefficients for the same gesture
 * will be interpreted with a difference of several tenths or hundredths), and zoom handling with the mouse wheel
 * scroll while pressing Ctrl/Cmd is not supported (a problem with browser scaling)
 * @param content The content to be displayed, which should include one plot for each series to be
 * plotted on this XYGraph.
 */
@Suppress("LongMethod") // expected
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
    panEnabled: Boolean = false,
    zoomEnabled: Boolean = false,
    allowIndependentZoom: Boolean = false,
    content: @Composable XYGraphScope<X, Y>.() -> Unit
) {
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
                (constraints.maxWidth - -(yAxisTitleMeasurable.maxIntrinsicWidth(constraints.maxHeight) ?: 0)).toDp()
            )
            val yAxis = AxisDelegate.createVerticalAxis(
                yAxisModel,
                yAxisStyle,
                (constraints.maxHeight - -(xAxisTitleMeasurable.maxIntrinsicHeight(constraints.maxWidth) ?: 0)).toDp()
            )

            val xAxisMeasurable = subcompose("xaxis") { Axis(xAxis) }[0]
            val yAxisMeasurable = subcompose("yaxis") { Axis(yAxis) }[0]

            val measurables = subcompose("gridAndLabels") {

                Grid(
                    xAxisState = xAxis,
                    yAxisState = yAxis,
                    horizontalMajorGridLineStyle,
                    horizontalMinorGridLineStyle,
                    verticalMajorGridLineStyle,
                    verticalMinorGridLineStyle
                )

                xAxis.majorTickValues.forEach {
                    Box(modifier = Modifier.rotate(-xAxisStyle.labelRotation.toFloat())) {
                        xAxisLabels(it)
                    }
                }

                yAxis.majorTickValues.forEach {
                    Box(modifier = Modifier.rotate(-yAxisStyle.labelRotation.toFloat())) {
                        yAxisLabels(it)
                    }
                }
            }

            // Predefine subcomposition with the name 'chart' (contents will be added below)
            val chartMeasurable = subcompose("chartBase") {
                // Placeholder Box that does not render anything
                // (Actual content will be added in the second subcompose)
                val panZoomModifier = if (panEnabled || zoomEnabled) {
                    Modifier.onGestureInput(
                        key1 = xAxisModel,
                        key2 = yAxisModel,
                        panLock = !panEnabled,
                        zoomLock = !zoomEnabled,
                        lockZoomRatio = !allowIndependentZoom,
                        onZoomChange = { size, centroid, zoomX, zoomY ->
                            val normalizedCentroid = centroid.normalizeCentroid(size)
                            zoomAxis(xAxisModel, size.width, normalizedCentroid.x, zoomX)
                            zoomAxis(yAxisModel, size.height, normalizedCentroid.y, zoomY)
                        },
                        onPanChange = { size, pan ->
                            val normalizedPan = pan.normalizePan()
                            panAxis(xAxisModel, size.width, normalizedPan.x)
                            panAxis(yAxisModel, size.height, normalizedPan.y)
                        }
                    )
                } else {
                    Modifier
                }

                Box(
                    modifier = Modifier
                        .clip(RectangleShape)
                        .then(panZoomModifier)
                )
            }[0]

            // 2) Measure and arrange the elements using a MeasurePolicy
            with(XYAxisMeasurePolicy(xAxis, yAxis)) {
                val (partialLayoutResult, chartAreas) = measure(
                    m = Measurables(
                        grid = measurables.getOrNull(0),
                        chart = chartMeasurable,
                        xAxis = xAxisMeasurable,
                        xAxisLabels = measurables.subList(1, xAxis.majorTickValues.size + 1),
                        xAxisTitle = xAxisTitleMeasurable,
                        yAxis = yAxisMeasurable,
                        yAxisLabels = measurables.subList(
                            xAxis.majorTickValues.size + 1,
                            xAxis.majorTickValues.size + 1 + yAxis.majorTickValues.size
                        ),
                        yAxisTitle = yAxisTitleMeasurable
                    ),
                    constraints = constraints
                )

                //
                // Based on the measurement results, calculate mouse coordinates and create Scope
                //
                val mouseAbsoluteOffset = getCurrentPointer() // Absolute screen coordinates (FractionalOffset)
                val mouseOffsetPx = chartAreas.copy(
                    xAxisTitleHeight = xAxisMeasurable?.maxIntrinsicHeight(chartAreas.graphSize.width) ?: 0,
                    yAxisTitleWidth = yAxisMeasurable?.maxIntrinsicWidth(chartAreas.graphSize.height) ?: 0
                ).getChartOffset(
                    mouseX = mouseAbsoluteOffset.x.toInt(),
                    mouseY = mouseAbsoluteOffset.y.toInt()
                )


                val scopeImpl = XYGraphScopeImpl(
                    xAxisModel = xAxisModel,
                    yAxisModel = yAxisModel,
                    xAxisState = xAxis,
                    yAxisState = yAxis,
                    hoverableElementAreaScope = this@HoverableElementArea,
                    mouseOffsetPx = mouseOffsetPx,
                    chartSizePx = chartAreas.graphSize
                )

                //
                // 3) Render the actual chart content using the second subcompose
                //    Although within the measure block, the 'subcompose(...)' safely executes @Composable lambdas
                //
                val chartContentPlaceables = subcompose("chartContent") {
                    // Safely call scopeImpl.content() here
                    scopeImpl.content()
                }.map {
                    // Measure 'chartContent' Composables
                    it.measure(Constraints.fixed(chartAreas.graphArea.width, chartAreas.graphArea.height))
                }

                //
                // 4) Return the final layout
                //    The partialLayoutResult already places grid/axes; just add chartContentPlaceables for the graph area
                //
                val finalLayout = layout(constraints.maxWidth, constraints.maxHeight) {
                    // Existing place operation
                    partialLayoutResult.placeChildren()
                    // Arrange chartContentPlaceables (Graph Area)
                    chartContentPlaceables.forEach { placeable ->
                        placeable.place(chartAreas.graphArea.left, chartAreas.graphArea.top)
                    }
                }

                finalLayout
            }
        }
    }
}

private fun <T> zoomAxis(
    axis: AxisModel<T>,
    length: Int,
    centroid: Float,
    zoom: Float
) {
    val pivot = centroid / length.toFloat()
    axis.zoom(zoom, pivot.coerceIn(0f, 1f))
}

private fun <T> panAxis(
    axis: AxisModel<T>,
    length: Int,
    pan: Float
) {
    axis.pan(pan / length.toFloat())
}

/**
 * Normalization of coordinates to the lower left point
 * Normalization is necessary because the scaling calculation is based on the fact that the origin point
 * of the coordinates is in the lower left corner
 */
private fun Offset.normalizeCentroid(size: IntSize): Offset = copy(
    y = abs(y - size.height),
)

private fun Offset.normalizePan(): Offset = copy(
    x = -x,
)

private data class Measurables(
    val grid: Measurable?,
    val chart: Measurable?,
    val xAxis: Measurable?,
    val xAxisLabels: List<Measurable>,
    val xAxisTitle: Measurable?,
    val yAxis: Measurable?,
    val yAxisLabels: List<Measurable>,
    val yAxisTitle: Measurable?,
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
            } else {
                val origin = Vector(0f, 0f)
                val p1 = Vector(0f, lastHeight.toFloat())
                val p2 = p1.rotate(-labelRotation.toFloat())
                val (distance, intersection) = lineDistance(
                    origin,
                    p2,
                    Vector(tickSpacing.toFloat(), 0f)
                )
                if (intersection.norm() < p2.norm()) {
                    distance.toInt()
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
    val yAxisTitleWidth: Int = 0,
    val yAxisLabelAreaWidth: Int = 0,
) {
    val graphSize: IntSize by lazy {
        IntSize(
            (constraints.maxWidth - (yAxisTitleWidth + yAxisLabelAreaWidth + yAxisOffset)).coerceAtLeast(0),
            (
                constraints.maxHeight - (xAxisTitleHeight + xAxisLabelAreaHeight + xAxisHeight - xAxisOffset)
                ).coerceAtLeast(0)
        )
    }

    /**
     * The location of the graph area within the chart.
     */
    val graphArea: IntRect by lazy {
        IntRect(IntOffset(yAxisTitleWidth + yAxisLabelAreaWidth + yAxisOffset, 0), graphSize)
    }

    /**
     * Calculates the x-axis tick spacing based on the width of the graph size and the provided [number] of ticks.
     */
    fun xTickSpacing(number: Int): Int {
        return graphSize.width / number.coerceAtLeast(1)
    }

    /**
     * Returns a copy of this ChartAreas after setting the [xAxisLabelAreaHeight],
     * based on the x-axis labels and their rotation angle.
     */
    fun withComputedXAxisLabelAreas(
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
        return copy(xAxisLabelAreaHeight = labelAreas.maxOfOrNull { it.height } ?: 0)
    }

    /**
     * Returns a copy of this ChartAreas after setting the [yAxisLabelAreaWidth]
     * based on the y-axis labels and their rotation angle.
     */
    fun withComputedYAxisLabelAreas(
        yAxisLabels: List<Measurable>,
        rotation: Int
    ): ChartAreas {
        val yAxisLabelAreas = yAxisLabels.map {
            val width = it.maxIntrinsicWidth(graphSize.height / yAxisLabels.size)
            RotatedComposableAreaDelegate(
                IntSize(width, it.maxIntrinsicHeight(width)),
                rotation.toFloat()
            )
        }
        return copy(yAxisLabelAreaWidth = yAxisLabelAreas.maxOfOrNull { it.width } ?: 0)
    }

    /**
     * Convert global screen coordinates (mouseX, mouseY) to chart-relative coordinates
     */
    fun getChartOffset(mouseX: Int, mouseY: Int): IntOffset {

        val chartAreaWidth = constraints.maxWidth - (yAxisLabelAreaWidth + yAxisTitleWidth + yAxisOffset)
        val chartAreaHeight = constraints.maxHeight -
            (xAxisTitleHeight + xAxisLabelAreaHeight + xAxisHeight - xAxisOffset)

        var chartXOffset = mouseX - (yAxisLabelAreaWidth + yAxisTitleWidth + yAxisOffset)
        var chartYOffset = mouseY
//        - (xAxisTitleHeight + xAxisLabelAreaHeight + xAxisHeight - xAxisOffset)


        // clamp
        chartXOffset = chartXOffset.coerceIn(0, chartAreaWidth)
        chartYOffset = chartYOffset.coerceIn(0, chartAreaHeight)

        return IntOffset(chartXOffset, chartYOffset)
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
            xAxisTitleHeight = m.xAxisTitle?.maxIntrinsicHeight(chartAreas.graphSize.width) ?: 0,
            yAxisTitleWidth = m.yAxisTitle?.maxIntrinsicWidth(chartAreas.graphSize.height) ?: 0
        )
        chartAreas = chartAreas.withComputedXAxisLabelAreas(m.xAxisLabels, xAxis.style.labelRotation)
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
    fun MeasureScope.measure(
        m: Measurables,
        constraints: Constraints
    ): Pair<MeasureResult, ChartAreas> {
        val chartAreas = optimizeGraphSize(constraints, m, xAxis, yAxis)

        val yAxisLabelPlaceableDelegates = createYAxisRotatedPlaceableDelegates(m, chartAreas)
        val xAxisPlaceable = m.xAxis?.measure(Constraints.fixedWidth(chartAreas.graphSize.width))
        val xAxisTitlePlaceable = m.xAxisTitle?.measure(Constraints(maxWidth = chartAreas.graphSize.width))
        val xAxisLabelPlaceableDelegates = createXAxisRotatedPlaceableDelegates(m, chartAreas)
        val yAxisTitlePlaceable = m.yAxisTitle?.measure(Constraints(maxHeight = chartAreas.graphSize.height))
        val yAxisPlaceable = m.yAxis?.measure(Constraints.fixedHeight(chartAreas.graphSize.height))
        val chartBasePlaceable = m.chart?.measure(
            Constraints.fixed(chartAreas.graphArea.width, chartAreas.graphArea.height)
        )

        val layoutResult = layout(constraints.maxWidth, constraints.maxHeight) {
            m.grid?.measure(
                Constraints.fixed(chartAreas.graphSize.width, chartAreas.graphSize.height)
            )?.place(chartAreas.graphArea.left, chartAreas.graphArea.top)

            xAxisTitlePlaceable?.place(
                chartAreas.graphArea.left + chartAreas.graphArea.width / 2 - (xAxisTitlePlaceable.width / 2),
                chartAreas.graphArea.bottom + (xAxisPlaceable?.height ?: 0) - xAxis.axisOffset.roundToPx()
                    + chartAreas.xAxisLabelAreaHeight
            )

            xAxisLabelPlaceableDelegates.forEachIndexed { index, placeable ->
                val anchor = if (xAxis.style.labelRotation == 0) {
                    AnchorPoint.TopCenter
                } else {
                    AnchorPoint.RightMiddle
                }
                with(placeable) {
                    place(
                        (chartAreas.graphArea.left + xAxis.majorTickOffsets[index] * chartAreas.graphArea.width).toInt(),
                        chartAreas.graphArea.bottom + (xAxisPlaceable?.height ?: 0) - xAxis.axisOffset.roundToPx(),
                        anchor
                    )
                }
            }

            yAxisTitlePlaceable?.place(
                chartAreas.graphArea.left - yAxis.axisOffset.roundToPx()
                    - chartAreas.yAxisLabelAreaWidth - yAxisTitlePlaceable.width,
                chartAreas.graphArea.top + chartAreas.graphArea.height / 2 - (yAxisTitlePlaceable.height / 2)
            )

            yAxisLabelPlaceableDelegates.forEachIndexed { index, placeable ->
                val anchor = if (yAxis.style.labelRotation == 90) {
                    AnchorPoint.BottomCenter
                } else {
                    AnchorPoint.RightMiddle
                }
                with(placeable) {
                    place(
                        chartAreas.graphArea.left - yAxis.axisOffset.roundToPx(),
                        (chartAreas.graphArea.bottom - yAxis.majorTickOffsets[index] * chartAreas.graphArea.height).toInt(),
                        anchor
                    )
                }
            }

            yAxisPlaceable?.place(
                chartAreas.graphArea.left - yAxis.axisOffset.roundToPx(),
                chartAreas.graphArea.top
            )

            xAxisPlaceable?.place(
                chartAreas.graphArea.left,
                chartAreas.graphArea.bottom - xAxis.axisOffset.roundToPx()
            )

            // chart Base (Empty Box)
            chartBasePlaceable?.place(
                chartAreas.graphArea.left,
                chartAreas.graphArea.top
            )
        }

        return layoutResult to chartAreas
    }

    private fun createYAxisRotatedPlaceableDelegates(m: Measurables, chartAreas: ChartAreas) =
        m.yAxisLabels.map {
            RotatedPlaceableDelegate(
                it.measure(Constraints(maxHeight = chartAreas.graphSize.height / m.yAxisLabels.size)),
                -yAxis.style.labelRotation.toFloat()
            )
        }

    private fun createXAxisRotatedPlaceableDelegates(m: Measurables, chartAreas: ChartAreas) =
        if (m.xAxisLabels.isNotEmpty()) {
            m.xAxisLabels.calcXAxisLabelWidthConstraints(
                xAxis.style.labelRotation,
                chartAreas.graphSize.width / m.xAxisLabels.size,
                chartAreas.xAxisLabelAreaHeight,
                { meas, w -> meas.measure(Constraints(maxWidth = w)) },
                { placeable -> placeable.height }
            ).map {
                RotatedPlaceableDelegate(it, -xAxis.style.labelRotation.toFloat())
            }
        } else {
            listOf()
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
     * Converts the mouse position within the chart (0..1 range) to data coordinates.
     * Returns null if the mouse is outside the chart.
     */
    public val pointerData: Point<X, Y>?

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
    val hoverableElementAreaScope: HoverableElementAreaScope,
    mouseOffsetPx: IntOffset,
    chartSizePx: IntSize
) : XYGraphScope<X, Y>, HoverableElementAreaScope by hoverableElementAreaScope {
    override val pointerData: Point<X, Y>? = run {
        if (mouseOffsetPx.x < 0 || mouseOffsetPx.y < 0 ||
            mouseOffsetPx.x > chartSizePx.width || mouseOffsetPx.y > chartSizePx.height
        ) {
            null
        } else {
            val nx = mouseOffsetPx.x.toFloat() / chartSizePx.width
            val ny = 1f - (mouseOffsetPx.y.toFloat() / chartSizePx.height)
            Point(
                x = xAxisModel.computeValue(nx),
                y = yAxisModel.computeValue(ny)
            )
        }
    }
}


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
    xOffsets: List<Float>,
    scale: Float,
    style: LineStyle?
) {
    if (style != null) {
        xOffsets.forEach {
            drawGridLine(
                style,
                Offset(it * scale, 0f),
                Offset(it * scale, size.height)
            )
        }
    }
}

private fun DrawScope.drawHorizontalGridLines(
    yOffsets: List<Float>,
    scale: Float,
    style: LineStyle?
) {
    if (style != null) {
        yOffsets.forEach {
            val yPos = scale - it * scale
            drawGridLine(style, Offset(0f, yPos), Offset(size.width, yPos))
        }
    }
}

private fun DrawScope.drawGridLine(
    gridLineStyle: LineStyle,
    start: Offset,
    end: Offset
) {
    with(gridLineStyle) {
        drawLine(
            brush = brush,
            start = start,
            end = end,
            strokeWidth = strokeWidth.toPx(),
            pathEffect = pathEffect,
            alpha = alpha,
            colorFilter = colorFilter,
            blendMode = blendMode
        )
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
 * @param panEnabled True if the plot can be panned, false to disable. Enabling panning may
 * interfere with scrolling a parent container if the drag point is on the plot.
 * @param zoomEnabled True if the plot can be zoomed, false to disable. Enabling zooming may
 * interfere with scrolling a parent container if the drag point is on the plot.
 * @param allowIndependentZoom `true` if the zoom can be either X-axis only or Y-axis only,
 * `false` if the total zoom factor must be used.
 * `true` does not mean that independent zoom coefficients are obtained simultaneously for each axis,
 * if the zoom was initiated horizontally - the zoom coefficient will change only for the X axis,
 * if the zoom was initiated vertically - the zoom coefficient will change only for the Y axis.
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
    panEnabled: Boolean = false,
    zoomEnabled: Boolean = false,
    allowIndependentZoom: Boolean = false,
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
                        modifier = Modifier
                            .rotateVertically(VerticalRotation.COUNTER_CLOCKWISE)
                            .padding(bottom = KoalaPlotTheme.sizes.gap),
                    )
                }
            }
        },
        horizontalMajorGridLineStyle,
        horizontalMinorGridLineStyle,
        verticalMajorGridLineStyle,
        verticalMinorGridLineStyle,
        panEnabled,
        zoomEnabled,
        allowIndependentZoom,
        content
    )
}