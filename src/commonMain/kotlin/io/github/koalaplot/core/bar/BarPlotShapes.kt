package io.github.koalaplot.core.bar

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import io.github.koalaplot.core.util.rad
import io.github.koalaplot.core.util.toDegrees
import io.github.koalaplot.core.xygraph.AxisModel
import io.github.koalaplot.core.xygraph.XYGraphScope
import kotlin.math.asin
import kotlin.math.max

/**
 * Rectangle shape with convex shaped side.
 * Useful for Single Vertical Bar Plot rendering.
 * Use in Stacked Bars is discouraged.
 */
@Stable
private val DefaultPlanoConvexShape: Shape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val shapeWidth = size.width
        val shapeHeight = size.height
        val arcRadius = shapeWidth / 2

        return Path().apply {
            val rectHeight = max((shapeHeight - arcRadius), 0F)
            addRect(
                rect = Rect(
                    offset = Offset(0F, arcRadius),
                    size = Size(shapeWidth, rectHeight)
                )
            )

            val heightRadiusOffset = max((arcRadius - shapeHeight), 0F)
            val heightRadiusOffsetDegrees =
                asin(heightRadiusOffset / arcRadius).rad.toDegrees().value.toFloat()
            addArc(
                oval = Size(shapeWidth, shapeWidth).toRect(),
                startAngleDegrees = 180F + heightRadiusOffsetDegrees,
                sweepAngleDegrees = 180F - 2 * heightRadiusOffsetDegrees
            )
        }.let(Outline::Generic)
    }
}

/**
 * Rectangle shape with convex shaped sides.
 * Useful for Single Vertical Bar Plot rendering.
 * Use in Stacked Bars is discouraged.
 */
@Stable
private val DefaultBiConvexShape: Shape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val outline = DefaultPlanoConvexShape.createOutline(size, layoutDirection, density) as Outline.Generic

        val shapeWidth = size.width
        val shapeHeight = size.height
        val arcRadius = shapeWidth / 2

        val cutoutRect = Path().apply {
            addRect(
                rect = Rect(
                    offset = Offset(0F, shapeHeight - arcRadius),
                    size = Size(shapeWidth, shapeWidth)
                )
            )
        }
        val cutoutArc = Path().apply {
            addArc(
                oval = Rect(
                    offset = Offset(0F, shapeHeight - shapeWidth),
                    size = Size(shapeWidth, shapeWidth)
                ),
                startAngleDegrees = 0F,
                sweepAngleDegrees = 180F
            )
        }
        val cutout = (cutoutRect - cutoutArc)
        return (outline.path - cutout).let(Outline::Generic)
    }
}

/**
 * Rectangle shape with concave/convex shaped sides.
 * Useful for Single Vertical Bar and Stacked Bars Plot rendering.
 *
 * @param xyGraphScope Provides access to [yAxisModel] and acts as an implementation of [XYGraphScope].
 * @param value The [VerticalBarPlotEntry] that defines the cutouts for the [PlanoConvexShape].
 */
@Stable
public class PlanoConvexShape<X, E : VerticalBarPlotEntry<X, Float>>(
    private val xyGraphScope: XYGraphScope<X, Float>,
    private val index: Int,
    private val value: E
) : Shape, XYGraphScope<X, Float> by xyGraphScope {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val shapeWidth = size.width
        val shapeHeight = size.height
        val arcRadius = shapeWidth / 2

        // Rendering negative values
        val isInverted = value.y.yMax < value.y.yMin
        // Required for proper bar rendering in waterfall charts
        if (index == 0) {
            val outline = DefaultPlanoConvexShape.createOutline(size, layoutDirection, density) as Outline.Generic

            outline.path.apply {
                // Rendering bar in negative direction
                if (isInverted) {
                    inverted(pivotX = shapeWidth / 2F, pivotY = shapeHeight / 2F)
                }
            }
            return outline
        }

        val (yZeroOffset, yMinOffset, yMaxOffset) = yAxisModel.yOffsets(value.y.yMin, value.y.yMax)

        // Prevent division by zero
        return if (yMaxOffset == yMinOffset) {
            Path().let(Outline::Generic)
        } else {
            // AxisModel's `computeOffset` method provides relative values between 0 and 1
            // Mapping offset values to pixel values
            val heightOffsetRatio = size.height / (yMaxOffset - yMinOffset)
            val offsetToHeight = { offset: Float -> offset * heightOffsetRatio }

            // Bars start with a concave path which might go below zero; the respective shape must be cut appropriately
            // Calculating aforementioned offset values for a bar's max and min value relative to the axis zero line
            val yMinZeroOffset = yMinOffset - yZeroOffset
            val yMaxZeroOffset = yMaxOffset - yZeroOffset

            // Getting screen's height pixel values from offsets
            val yMinZeroHeight = offsetToHeight(yMinZeroOffset)
            val yMaxZeroHeight = offsetToHeight(yMaxZeroOffset)

            // If min and max values are greater than arcRadius, aforementioned below zero compensation is not required
            // and therefore becomes ineffective
            val yMinZeroArcHeight = max((arcRadius - yMinZeroHeight), 0F)
            val yMaxZeroArcHeight = max((arcRadius - yMaxZeroHeight), 0F)

            // Prevent arc from being drawn below zero by subtracting value in degrees
            val yMaxZeroArcHeightDegrees =
                asin(yMaxZeroArcHeight / arcRadius).rad.toDegrees().value.toFloat()

            Path().apply {
                (
                    Path().apply {
                        addArc(
                            oval = Size(shapeWidth, shapeWidth).toRect(),
                            startAngleDegrees = 180F + yMaxZeroArcHeightDegrees,
                            sweepAngleDegrees = 180F - 2 * yMaxZeroArcHeightDegrees
                        )
                    } - Path().apply {
                        addArc(
                            oval = Rect(
                                offset = Offset(0F, shapeHeight),
                                size = Size(shapeWidth, shapeWidth)
                            ),
                            startAngleDegrees = 180F,
                            sweepAngleDegrees = 180F
                        )
                    }
                    ).let(::addPath)

                (
                    Path().apply {
                        addRect(
                            rect = Rect(
                                offset = Offset(0F, arcRadius),
                                size = Size(shapeWidth, max(shapeHeight - yMinZeroArcHeight, 0F))
                            )
                        )
                    } - Path().apply {
                        addArc(
                            oval = Rect(
                                offset = Offset(0F, shapeHeight),
                                size = Size(shapeWidth, shapeWidth)
                            ),
                            startAngleDegrees = 180F,
                            sweepAngleDegrees = 180F
                        )
                    }
                    ).let(::addPath)
                // Rendering bar in negative direction
                if (isInverted) {
                    inverted(pivotX = shapeWidth / 2F, pivotY = shapeHeight / 2F)
                }
            }.let(Outline::Generic)
        }
    }
}

/**
 * Rectangle shape with concave/convex shaped sides and additional convex cutout at the bottom.
 * Useful for Single Vertical Bar and Stacked Bars Plot rendering.
 *
 * Primary constructor:
 * @param planoConvexShape The internal shape logic used for rendering.
 * @param value The [VerticalBarPlotEntry] that defines the cutouts for the [PlanoConvexShape].
 *
 * Secondary constructor:
 * @param xyGraphScope Provides access to [yAxisModel] and acts as an implementation of [XYGraphScope].
 * @param value The [VerticalBarPlotEntry] used to construct the internal shape as well as the additional convex cutout.
 */
@Stable
public class BiConvexShape<X, E : VerticalBarPlotEntry<X, Float>> private constructor(
    private val planoConvexShape: PlanoConvexShape<X, E>,
    private val index: Int,
    private val value: E
) : Shape, XYGraphScope<X, Float> by planoConvexShape {

    public constructor(
        xyGraphScope: XYGraphScope<X, Float>,
        index: Int,
        value: E
    ) : this(
        PlanoConvexShape(xyGraphScope, index, value),
        index,
        value
    )

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val shapeWidth = size.width
        val shapeHeight = size.height
        val arcRadius = shapeWidth / 2

        // Rendering negative values
        val isInverted = value.y.yMax < value.y.yMin
        // Required for proper bar rendering in waterfall charts
        if (index == 0) {
            val outline = DefaultBiConvexShape.createOutline(size, layoutDirection, density) as Outline.Generic

            outline.path.apply {
                // Rendering bar in negative direction
                if (isInverted) {
                    inverted(pivotX = shapeWidth / 2F, pivotY = shapeHeight / 2F)
                }
            }
            return outline
        }

        val (yZeroOffset, yMinOffset, yMaxOffset) = yAxisModel.yOffsets(value.y.yMin, value.y.yMax)

        // Prevent division by zero
        return if (yMaxOffset == yMinOffset) {
            Path().let(Outline::Generic)
        } else {
            // AxisModel's `computeOffset` method provides relative values between 0 and 1
            // Mapping offset values to pixel values
            val heightOffsetRatio = size.height / (yMaxOffset - yMinOffset)
            val offsetToHeight = { offset: Float -> offset * heightOffsetRatio }

            // Bars start with a concave path which might go below zero; the respective shape must be cut appropriately
            // Calculating aforementioned offset values for a bar's max and min value relative to the axis zero line
            val yMaxZeroOffset = yMaxOffset - yZeroOffset
            // Getting screen's height pixel values from offsets
            val yMaxZeroHeight = offsetToHeight(yMaxZeroOffset)

            val outline = planoConvexShape.createOutline(size, layoutDirection, density) as Outline.Generic

            val cutoutRect = Path().apply {
                addRect(
                    rect = Rect(
                        offset = Offset(0F, yMaxZeroHeight - arcRadius),
                        size = Size(shapeWidth, shapeWidth)
                    )
                )
            }
            val cutoutArc = Path().apply {
                addArc(
                    oval = Rect(
                        offset = Offset(0F, yMaxZeroHeight - shapeWidth),
                        size = Size(shapeWidth, shapeWidth)
                    ),
                    startAngleDegrees = 0F,
                    sweepAngleDegrees = 180F
                )
            }
            val cutout = (cutoutRect - cutoutArc).apply {
                // Rendering bar in negative direction
                if (isInverted) {
                    inverted(pivotX = shapeWidth / 2F, pivotY = shapeHeight / 2F)
                }
            }
            (outline.path - cutout).let(Outline::Generic)
        }
    }
}

private fun Path.inverted(pivotX: Float, pivotY: Float): Path {
    Matrix().apply {
        resetToPivotedTransform(
            pivotX = pivotX,
            pivotY = pivotY,
            rotationZ = 180F
        )
    }.let(::transform)
    return this
}

private fun AxisModel<Float>.yOffsets(yMin: Float, yMax: Float): YOffsets {
    val yZeroOffset = computeOffset(0F).coerceIn(0F, 1F)
    val yMinOffset = computeOffset(yMin).coerceIn(0f, 1f)
    val yMaxOffset = computeOffset(yMax).coerceIn(0f, 1f)
    return YOffsets(
        yZeroOffset = yZeroOffset,
        yMinOffset = yMinOffset,
        yMaxOffset = yMaxOffset
    )
}

private data class YOffsets(
    val yZeroOffset: Float,
    val yMinOffset: Float,
    val yMaxOffset: Float
)
