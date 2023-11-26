package io.github.koalaplot.core.polar

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.MultiContentMeasurePolicy
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import io.github.koalaplot.core.polar.RadialGridType.CIRCLES
import io.github.koalaplot.core.polar.RadialGridType.LINES
import io.github.koalaplot.core.style.AreaStyle
import io.github.koalaplot.core.style.KoalaPlotTheme
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.util.AngularValue
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.util.HoverableElementArea
import io.github.koalaplot.core.util.HoverableElementAreaScope
import io.github.koalaplot.core.util.cos
import io.github.koalaplot.core.util.deg
import io.github.koalaplot.core.util.maximize
import io.github.koalaplot.core.util.polarToCartesian
import io.github.koalaplot.core.util.sin
import io.github.koalaplot.core.util.toDegrees
import io.github.koalaplot.core.util.toRadians
import kotlin.jvm.JvmName
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Scope for plot series content placed on a [PolarGraph].
 */
public interface PolarGraphScope<T> : HoverableElementAreaScope {
    /**
     * Provides the [FloatRadialAxisModel] for the plot to scale coordinates.
     */
    public val radialAxisModel: FloatRadialAxisModel

    /**
     * Provides the [AngularAxisModel] for the plot to scale coordinates.
     */
    public val angularAxisModel: AngularAxisModel<T>

    /**
     * Transforms the provided [point] from polar plot axis coordinates to cartesian coordinates for on-screen
     * drawing, provided a [size] of the drawing area.
     */
    public fun polarToCartesian(point: PolarPoint<Float, T>, size: Size): Offset
}

/**
 * An enum class to specify how the radial axis grid lines are drawn. [CIRCLES] will draw the grid lines
 * as concentric circles of different radii, whereas [LINES] will draw them as straight lines between consecutive
 * angular axis grid lines.
 */
public enum class RadialGridType {
    CIRCLES,
    LINES
}

/**
 * Properties used to customize the visuals and layout of a [PolarGraph].
 * @param radialGridType The type of radial grid lines to use - circles or line segments between radial axis line.
 * @param radialAxisGridLineStyle Styling for the radial axis grid lines
 * @param angularAxisGridLineStyle Styling for the angular axis grid lines
 * @param angularLabelGap Distance of the angular axis labels from the plot's outermost grid line
 * @param radialLabelGap Distance between the radial labels and the axis line
 * @param background The style to apply to the background of the plot area that contains the gridlines.
 */
public data class PolarGraphProperties(
    val radialGridType: RadialGridType,
    val radialAxisGridLineStyle: LineStyle?,
    val angularAxisGridLineStyle: LineStyle?,
    val angularLabelGap: Dp,
    val radialLabelGap: Dp,
    val background: AreaStyle?
)

/**
 * Contains default values for [PolarGraph]s.
 */
public object PolarGraphDefaults {
    /**
     * Default values for [PolarGraphProperties].
     */
    @Composable
    public fun PolarGraphPropertyDefaults(): PolarGraphProperties = PolarGraphProperties(
        CIRCLES,
        KoalaPlotTheme.axis.majorGridlineStyle,
        KoalaPlotTheme.axis.majorGridlineStyle,
        KoalaPlotTheme.sizes.gap,
        KoalaPlotTheme.sizes.gap,
        null
    )
}

/**
 * A Graph using polar coordinates - a radial axis and an angular axis.
 * Multiple series of data can be plotted on a polar graph as lines and/or shaded regions with or without
 * symbols at each plotted point.
 *
 * @param T The data type for the angular axis
 * @param radialAxisModel Provides the radial axis coordinate system
 * @param angularAxisModel An [AngularAxisModel] providing the angular axis coordinate system
 * @param radialAxisLabels [Composable] providing radial axis labels.
 * @param angularAxisLabels [Composable] providing angular axis labels.
 * @param polarGraphProperties Properties to customize plot styling.
 * @param content Content to display on the plot, see [PolarPlotSeries].
 */
@ExperimentalKoalaPlotApi
@Composable
public fun <T> PolarGraph(
    radialAxisModel: FloatRadialAxisModel,
    angularAxisModel: AngularAxisModel<T>,
    radialAxisLabels: @Composable (Float) -> Unit,
    angularAxisLabels: @Composable (T) -> Unit,
    modifier: Modifier = Modifier,
    polarGraphProperties: PolarGraphProperties = PolarGraphDefaults.PolarGraphPropertyDefaults(),
    content: @Composable PolarGraphScope<T>.() -> Unit
) {
    HoverableElementArea(modifier = modifier) {
        val scope = object : PolarGraphScope<T>, HoverableElementAreaScope by this {
            override val radialAxisModel = radialAxisModel
            override val angularAxisModel = angularAxisModel
            override fun polarToCartesian(point: PolarPoint<Float, T>, size: Size): Offset {
                return polarToCartesianPlot(point, angularAxisModel, radialAxisModel, size)
            }
        }

        Layout(
            contents = buildList {
                add { scope.Grid(polarGraphProperties) }
                add {
                    radialAxisModel.tickValues.forEach {
                        Box { radialAxisLabels(it) }
                    }
                }
                add {
                    angularAxisModel.getTickValues().forEach {
                        Box { angularAxisLabels(it) }
                    }
                }
                add {
                    Box(
                        modifier = Modifier.drawWithContent {
                            val clipPath = scope.generateGridBoundaryPath(size, polarGraphProperties.radialGridType)
                            clipPath(clipPath) {
                                this@drawWithContent.drawContent()
                            }
                        }
                    ) { scope.content() }
                }
            },
            measurePolicy = PolarGraphMeasurePolicy(angularAxisModel, radialAxisModel, polarGraphProperties)
        )
    }
}

/**
 * Transforms [inputAngle] to start at 3 O'Clock and increment counter-clockwise like
 * the normal mathematical convention.
 */
private fun <T> AngularAxisModel<T>.toPolarAngle(inputAngle: AngularValue): AngularValue {
    val sign = if (angleDirection == AngularAxisModel.AngleDirection.CLOCKWISE) {
        1.0
    } else {
        -1.0
    }

    val originOffset = when (angleZero) {
        AngularAxisModel.AngleZero.THREE_OCLOCK -> 0.0
        AngularAxisModel.AngleZero.SIX_OCLOCK -> PI / 2.0
        AngularAxisModel.AngleZero.NINE_OCLOCK -> PI
        AngularAxisModel.AngleZero.TWELVE_OCLOCK -> -PI / 2.0
    }

    // return (inputAngle.toRadians().value - PI / 2.0).toRadians()
    // return (-inputAngle.toRadians().value).toRadians()
    return (sign * inputAngle.toRadians().value + originOffset).toRadians()
}

private fun <T> polarToCartesianPlot(
    point: PolarPoint<Float, T>,
    angularAxisModel: AngularAxisModel<T>,
    radialAxisModel: FloatRadialAxisModel,
    size: Size
): Offset {
    // Transform the angle to where 0 is at the 12 O'Clock position.
    val theta = angularAxisModel.toPolarAngle(angularAxisModel.computeOffset(point.theta))

    val r = min(size.width / 2, size.height / 2) * radialAxisModel.computeOffset(point.r)
    return polarToCartesian(r, theta)
}

private class PolarGraphMeasurePolicy<T>(
    private val angularAxisModel: AngularAxisModel<T>,
    private val radialAxisModel: FloatRadialAxisModel,
    private val plotProperties: PolarGraphProperties
) : MultiContentMeasurePolicy {
    private val gridIndex = 0
    private val radialAxisIndex = 1
    private val angularAxisIndex = 2
    private val contentIndex = 3
    override fun MeasureScope.measure(
        measurables: List<List<Measurable>>,
        constraints: Constraints
    ): MeasureResult {
        val grid = measurables[gridIndex][0]
        val radialAxisLabelMeasurables = measurables[radialAxisIndex]
        val angularAxisLabelMeasurables = measurables[angularAxisIndex]
        val contentMeasurable = measurables[contentIndex][0]

        // Compute the chart radius that allows all labels to fit using approximate "intrinsic" measurements
        var plotRadius =
            calculatePlotRadius(plotProperties.angularLabelGap.toPx(), constraints, angularAxisLabelMeasurables)

        // Measure all the angularAxis labels
        val angularAxisLabelPlaceables = angularAxisModel.getTickValues().mapIndexed { index, t ->
            val offset = angularAxisModel.toPolarAngle(angularAxisModel.computeOffset(t))

            angularAxisLabelMeasurables[index].measure(
                // constraints,
                Constraints(
                    0,
                    (
                        constraints.maxWidth / 2.0 -
                            (plotRadius + plotProperties.angularLabelGap.toPx()) * abs(cos(offset))
                        ).toInt(),
                    0,
                    (
                        constraints.maxHeight / 2.0 -
                            (plotRadius + plotProperties.angularLabelGap.toPx()) * abs(sin(offset))
                        ).toInt()
                )
            )
        }

        // Recompute the chart radius using exact placeable measurements
        plotRadius = calculatePlotRadius(plotProperties.angularLabelGap.toPx(), constraints, angularAxisLabelPlaceables)

        val gridPlaceable = grid.measure(Constraints.fixed((plotRadius * 2.0f).toInt(), (plotRadius * 2.0f).toInt()))
        val contentPlaceable =
            contentMeasurable.measure(Constraints.fixed((plotRadius * 2.0f).toInt(), (plotRadius * 2.0f).toInt()))

        val radialAxisLabelPlaceables = radialAxisLabelMeasurables.map {
            it.measure(
                Constraints(0, plotRadius.toInt(), 0, (plotRadius / radialAxisLabelMeasurables.size).toInt())
            )
        }

        val plotSize = calculatePlotSize(
            plotRadius + plotProperties.angularLabelGap.toPx(),
            angularAxisLabelPlaceables.map { Size(it.width.toFloat(), it.height.toFloat()) }
        )

        return layout(plotSize.width.roundToInt(), plotSize.height.roundToInt()) {
            gridPlaceable.place((plotSize.width / 2).roundToInt(), (plotSize.height / 2).roundToInt())
            contentPlaceable.place((plotSize.width / 2).roundToInt(), (plotSize.height / 2).roundToInt())

            angularAxisModel.getTickValues().forEachIndexed { index, t ->
                val angle = angularAxisModel.toPolarAngle(angularAxisModel.computeOffset(t))
                val label = angularAxisLabelPlaceables[index]

                val labelOffset = Offset(
                    (
                        plotSize.width / 2.0f + (plotRadius + plotProperties.angularLabelGap.toPx()) * cos(angle)
                        ).toFloat(),
                    (
                        plotSize.height / 2.0f + (plotRadius + plotProperties.angularLabelGap.toPx()) * sin(angle)
                        ).toFloat()
                ) + computeLabelPositionOffset(angle, label)

                angularAxisLabelPlaceables[index].place(labelOffset.x.toInt(), labelOffset.y.toInt())
            }

            radialAxisLabelPlaceables.forEachIndexed { index, placeable ->
                val radius = radialAxisModel.computeOffset(radialAxisModel.tickValues[index]) * plotRadius
                placeable.place(
                    (plotSize.width / 2f + plotProperties.radialLabelGap.toPx()).toInt(),
                    ((plotSize.height - placeable.height) / 2f - radius).toInt()
                )
            }
        }
    }

    /**
     * Computes the required plot size for placing the grid pole in the center of the plot area with angular
     * axis labels around the periphery.
     *
     * @param plotRadius The radius of the outside edge of the plot grid + label gap.
     */
    private fun calculatePlotSize(plotRadius: Float, labelSizes: List<Size>): Size {
        val rects = doRadialLabelLayout(
            plotRadius,
            angularAxisModel.getTickValues().map {
                angularAxisModel.toPolarAngle(angularAxisModel.computeOffset(it))
            },
            labelSizes
        )

        var maxX = plotRadius
        var minX = -plotRadius
        var maxY = plotRadius
        var minY = -plotRadius

        // compute the max x and y extents of the label rects and ensure Size is large enough to accommodate it
        rects.forEach { rect ->
            maxX = max(maxX, rect.right)
            minX = min(minX, rect.left)
            maxY = max(maxY, rect.bottom)
            minY = min(minY, rect.top)
        }

        // Want the pole of the plot to be centered, so dimension needs to be 2x the maximum extent of any label
        val width = 2f * max(abs(maxX), abs(minX))
        val height = 2f * max(abs(maxY), abs(minY))
        val sideLength = max(width, height).coerceAtLeast(0f)

        return Size(sideLength, sideLength)
    }

    /**
     * Calculates the size of the plot grid radius that allows the labels to fit around the perimeter and
     * within the constraints using Intrinsic measurements.
     */
    @JvmName("calculatePlotRadiusFromMeasurables")
    private fun calculatePlotRadius(
        labelGap: Float, // Label gap in pixels
        constraints: Constraints,
        angularAxisLabelMeasurables: List<Measurable>
    ): Float = calculatePlotRadius(
        labelGap,
        constraints,
        angularAxisLabelMeasurables.map {
            Size(
                it.maxIntrinsicWidth(constraints.maxHeight / 2).toFloat(),
                it.maxIntrinsicHeight(constraints.maxWidth / 2).toFloat()
            )
        }
    )

    /**
     * Calculates the size of the plot grid radius that allows the labels to fit around the perimeter and
     * within the constraints using actual placeable sizes.
     */
    @JvmName("calculatePlotRadiusFromPlaceables")
    private fun calculatePlotRadius(
        labelGap: Float, // Label gap in pixels
        constraints: Constraints,
        angularAxisLabelPlaceables: List<Placeable>
    ): Float = calculatePlotRadius(
        labelGap,
        constraints,
        angularAxisLabelPlaceables.map {
            Size(it.width.toFloat(), it.height.toFloat())
        }
    )

    private fun calculatePlotRadius(
        labelGap: Float, // Label gap in pixels
        constraints: Constraints,
        sizes: List<Size>
    ): Float {
        val maxR =
            maximize(0.0, min(constraints.maxWidth, constraints.maxHeight) / 2.0, tolerance = 1E-4) { radius ->
                val plotSize = calculatePlotSize(radius.toFloat() + labelGap, sizes)
                plotSize.width < constraints.maxWidth && plotSize.height < constraints.maxHeight
            }

        return maxR.toFloat()
    }

    /**
     * Computes the offset for where to place the top left corner of the [label] relative to the nominal
     * angular axis position based on the label's angular position.
     *
     * @param angle Angle in radians
     * @param label Label to have its offset computed
     */
    private fun computeLabelPositionOffset(angle: AngularValue, label: Placeable): Offset {
        return computeLabelPositionOffset(angle, Size(label.width.toFloat(), label.height.toFloat()))
    }

    /**
     * Computes how much a label should be offset to place it's anchor
     * point at the target position given an angle for its placement and its size.
     *
     * @param angle Angle in radians
     * @param label Size of the label to have its offset computed
     */
    private fun computeLabelPositionOffset(angle: AngularValue, label: Size): Offset {
        return when {
            topSector.contains(angle) -> Offset(-label.width / 2f, -label.height)
            bottomSector.contains(angle) -> Offset(-label.width / 2.0f, 0f)
            rightSector.contains(angle) -> Offset(0f, -label.height / 2.0f)
            leftSector.contains(angle) -> Offset(-label.width, -label.height / 2.0f)
            topRightSector.contains(angle) -> Offset(0f, -label.height)
            bottomRightSector.contains(angle) -> Offset(0f, 0f)
            bottomLeftSector.contains(angle) -> Offset(-label.width, 0f)
            topLeftSector.contains(angle) -> Offset(-label.width, -label.height)
            else -> Offset(0f, 0f) // shouldn't happen
        }
    }

    /**
     * Given a plot [radius], [labelAngles], and [labelSizes], computes where the labels should be located and
     * returns their bounding rectangles. This is done in a coordinate system where the plot pole is at
     * (0, 0) in cartesian coordinates.
     */
    private fun doRadialLabelLayout(
        radius: Float,
        labelAngles: List<AngularValue>,
        labelSizes: List<Size>
    ): List<Rect> = buildList {
        labelAngles.forEachIndexed { index, angle ->
            val labelOffset = polarToCartesian(radius, angle) + computeLabelPositionOffset(angle, labelSizes[index])
            add(Rect(labelOffset, labelSizes[index]))
        }
    }

    // These sectors define breakpoints where angular axis labels will be anchored to different points on the label
    private val topSector = AngularSector(255.0.deg, 285.0.deg)
    private val bottomSector = AngularSector(75.0.deg, 105.0.deg)
    private val rightSector = AngularSector((-15.0).deg, 15.0.deg)
    private val leftSector = AngularSector(165.0.deg, 195.0.deg)
    private val topRightSector = AngularSector(285.0.deg, 345.0.deg)
    private val bottomRightSector = AngularSector(15.0.deg, 75.0.deg)
    private val bottomLeftSector = AngularSector(105.0.deg, 165.0.deg)
    private val topLeftSector = AngularSector(195.0.deg, 255.0.deg)
}

/**
 * Represents an angular sector of the full circle as a section between a min and max angle. Provides
 * for testing if an angle falls within the sector.
 */
private data class AngularSector(val minAngle: AngularValue, val maxAngle: AngularValue) {
    val normalizedMin = normalize(minAngle.toDegrees().value)
    val normalizedMax = normalize(maxAngle.toDegrees().value)

    /**
     * Tests whether the provided [angle] is within this sector.
     */
    fun contains(angle: AngularValue): Boolean {
        @Suppress("MagicNumber")
        val normalizedAngle = (angle.toDegrees().value % 360.0 + 360.0) % 360.0

        return if (normalizedMin > normalizedMax) { // straddling the 0-degree line
            normalizedAngle > normalizedMin || normalizedAngle < normalizedMax
        } else {
            normalizedAngle in normalizedMin..normalizedMax
        }
    }

    private fun normalize(angle: Double): Double {
        @Suppress("MagicNumber")
        return (angle.toDegrees().value % 360.0 + 360.0) % 360.0
    }
}

/**
 * A Graph using polar coordinates - a radial axis and an angular axis.
 * Multiple series of data can be plotted on a polar graph as lines and/or shaded regions with or without
 * symbols at each plotted point.
 *
 * @param T The data type for the angular axis
 * @param radialAxisModel Provides the radial axis coordinate system
 * @param angularAxisModel An [AngularAxisModel] providing the angular axis coordinate system
 * @param radialAxisLabelText Provides strings for radial axis labels.
 * @param angularAxisLabelText Provides strings for angular axis labels.
 * @param polarGraphProperties Properties to customize plot styling.
 * @param content Content to display on the plot, see [PolarPlotSeries].
 */
@ExperimentalKoalaPlotApi
@Composable
public fun <T> PolarGraph(
    radialAxisModel: FloatRadialAxisModel,
    angularAxisModel: AngularAxisModel<T>,
    radialAxisLabelText: (Float) -> String = { it.toString() },
    angularAxisLabelText: (T) -> String = { it.toString() },
    modifier: Modifier = Modifier,
    polarGraphProperties: PolarGraphProperties = PolarGraphDefaults.PolarGraphPropertyDefaults(),
    content: @Composable PolarGraphScope<T>.() -> Unit
) {
    PolarGraph(
        radialAxisModel,
        angularAxisModel,
        radialAxisLabels = {
            Text(
                radialAxisLabelText(it),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodySmall
            )
        },
        angularAxisLabels = {
            Text(
                angularAxisLabelText(it),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodySmall
            )
        },
        modifier,
        polarGraphProperties,
        content
    )
}
