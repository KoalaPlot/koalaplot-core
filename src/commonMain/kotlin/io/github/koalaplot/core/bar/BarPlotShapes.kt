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
private val DefaultVerticalPlanoConvexShape: Shape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val shapeWidth = size.width
        val shapeHeight = size.height
        val arcRadius = shapeWidth / 2

        val rectHeight = max((shapeHeight - arcRadius), 0F)
        val body = Path().apply {
            addRect(
                rect = Rect(
                    offset = Offset(0F, arcRadius),
                    size = Size(shapeWidth, rectHeight),
                ),
            )
        }

        val heightRadiusOffset = max((arcRadius - shapeHeight), 0F)
        val heightRadiusOffsetDegrees =
            asin(heightRadiusOffset / arcRadius)
                .rad
                .toDegrees()
                .value
                .toFloat()
        val cap = Path().apply {
            addArc(
                oval = Size(shapeWidth, shapeWidth).toRect(),
                startAngleDegrees = 180F + heightRadiusOffsetDegrees,
                sweepAngleDegrees = 180F - 2 * heightRadiusOffsetDegrees,
            )
        }

        // Merge body and cap into a single contour. As separate subpaths the rectangle keeps its
        // interior top edge, which would be stroked as a stray line when the bar is drawn with a
        // border. The union removes that internal edge while leaving the filled area unchanged.
        return (body + cap).let(Outline::Generic)
    }
}

/**
 * Rectangle shape with convex shaped side.
 * Useful for Single Horizontal Bar Plot rendering.
 * Use in Stacked Bars is discouraged.
 */
@Stable
private val DefaultHorizontalPlanoConvexShape: Shape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val shapeWidth = size.width
        val shapeHeight = size.height
        val arcRadius = shapeHeight / 2

        val rectWidth = max((shapeWidth - arcRadius), 0F)
        val body = Path().apply { addRect(Size(rectWidth, shapeHeight).toRect()) }

        val widthRadiusOffset = max((arcRadius - shapeWidth), 0F)
        val widthRadiusOffsetDegrees =
            asin(widthRadiusOffset / arcRadius)
                .rad
                .toDegrees()
                .value
                .toFloat()
        val cap = Path().apply {
            addArc(
                oval = Rect(
                    offset = Offset(rectWidth - arcRadius - widthRadiusOffset, 0F),
                    size = Size(shapeHeight, shapeHeight),
                ),
                startAngleDegrees = 270F + widthRadiusOffsetDegrees,
                sweepAngleDegrees = 180F - 2 * widthRadiusOffsetDegrees,
            )
        }

        // Single contour (see DefaultVerticalPlanoConvexShape for the rationale).
        return (body + cap).let(Outline::Generic)
    }
}

/**
 * Rectangle shape with convex shaped sides.
 * Useful for Single Vertical Bar Plot rendering.
 * Use in Stacked Bars is discouraged.
 */
@Stable
private val DefaultVerticalBiConvexShape: Shape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val outline = DefaultVerticalPlanoConvexShape.createOutline(size, layoutDirection, density) as Outline.Generic

        val shapeWidth = size.width
        val shapeHeight = size.height
        val arcRadius = shapeWidth / 2

        val cutoutRect = Path().apply {
            addRect(
                rect = Rect(
                    offset = Offset(0F, shapeHeight - arcRadius),
                    size = Size(shapeWidth, shapeWidth),
                ),
            )
        }
        val cutoutArc = Path().apply {
            addArc(
                oval = Rect(
                    offset = Offset(0F, shapeHeight - shapeWidth),
                    size = Size(shapeWidth, shapeWidth),
                ),
                startAngleDegrees = 0F,
                sweepAngleDegrees = 180F,
            )
        }
        val cutout = (cutoutRect - cutoutArc)
        return (outline.path - cutout).let(Outline::Generic)
    }
}

/**
 * Rectangle shape with convex shaped sides.
 * Useful for Single Horizontal Bar Plot rendering.
 * Use in Stacked Bars is discouraged.
 */
@Stable
private val DefaultHorizontalBiConvexShape: Shape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val outline = DefaultHorizontalPlanoConvexShape
            .createOutline(size, layoutDirection, density) as Outline.Generic

        val shapeHeight = size.height
        val arcRadius = shapeHeight / 2

        val cutoutRect = Path().apply {
            addRect(
                rect = Rect(
                    offset = Offset(-arcRadius, 0F),
                    size = Size(shapeHeight, shapeHeight),
                ),
            )
        }
        val cutoutArc = Path().apply {
            addArc(
                oval = Size(shapeHeight, shapeHeight).toRect(),
                startAngleDegrees = 90F,
                sweepAngleDegrees = 180F,
            )
        }
        val cutout = (cutoutRect - cutoutArc)
        return (outline.path - cutout).let(Outline::Generic)
    }
}

/**
 * Rectangle shape with planar/convex shaped sides.
 * Useful for Single Vertical Bar and Stacked Bars Plot rendering.
 *
 * @param xyGraphScope Provides access to [yAxisModel] and acts as an implementation of [XYGraphScope].
 * @param index Represents the element index within the series.
 * @param value The [VerticalBarPlotEntry] that defines the cutouts for the [VerticalPlanoConvexShape].
 * @param barScope The [BarScope] of the composable emitting the bar. It provides the exact pixel geometry of the bar
 * plot, which is needed to cut the shape off at the zero line. If null, the geometry is estimated from the size of the
 * bar, which can be off by a few pixels for short bars close to the zero line.
 */
@Stable
public class VerticalPlanoConvexShape<X, E : VerticalBarPlotEntry<X, Float>>(
    private val xyGraphScope: XYGraphScope<X, Float>,
    private val index: Int,
    private val value: E,
    private val barScope: BarScope?,
) : Shape,
    XYGraphScope<X, Float> by xyGraphScope {
    /**
     * Creates the shape without a [BarScope], so the pixel geometry of the bar plot is estimated from the size of the
     * bar.
     */
    public constructor(
        xyGraphScope: XYGraphScope<X, Float>,
        index: Int,
        value: E,
    ) : this(xyGraphScope, index, value, null)

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val shapeWidth = size.width
        val shapeHeight = size.height
        val arcRadius = shapeWidth / 2

        // Rendering negative values
        val isInverted = value.y.end < value.y.start
        // Required for proper bar rendering in waterfall charts
        if (index == 0) {
            val path =
                (DefaultVerticalPlanoConvexShape.createOutline(size, layoutDirection, density) as Outline.Generic).path
            // Explicitly thread the (optionally inverted) path into the result instead of mutating
            // the wrapped outline in place.
            val result = if (isInverted) {
                path.inverted(pivotX = shapeWidth / 2F, pivotY = shapeHeight / 2F)
            } else {
                path
            }
            return result.let(Outline::Generic)
        }

        val (yZeroOffset, yMinOffset, yMaxOffset) = yAxisModel.yOffsets(value.y.start, value.y.end)

        // Prevent division by zero
        return if (yMaxOffset == yMinOffset) {
            Path().let(Outline::Generic)
        } else {
            // Bars start with a concave path which might go below zero; the respective shape must be cut appropriately
            // Getting the pixel distances of a bar's min and max value from the axis zero line
            val zeroLineDistances = barScope.zeroLineDistances(yZeroOffset, yMinOffset, yMaxOffset, size.height)
            val yMinZeroHeight = zeroLineDistances.start
            val yMaxZeroHeight = zeroLineDistances.end

            // If min and max values are greater than arcRadius, aforementioned below zero compensation is not required
            // and therefore becomes ineffective
            val yMinZeroArcHeight = max((arcRadius - yMinZeroHeight), 0F)
            val yMaxZeroArcHeight = max((arcRadius - yMaxZeroHeight), 0F)

            // Prevent arc from being drawn below zero by subtracting value in degrees
            val yMaxZeroArcHeightDegrees =
                asin(yMaxZeroArcHeight / arcRadius)
                    .rad
                    .toDegrees()
                    .value
                    .toFloat()

            val cap = Path().apply {
                addArc(
                    oval = Size(shapeWidth, shapeWidth).toRect(),
                    startAngleDegrees = 180F + yMaxZeroArcHeightDegrees,
                    sweepAngleDegrees = 180F - 2 * yMaxZeroArcHeightDegrees,
                )
            } - Path().apply {
                addArc(
                    oval = Rect(
                        offset = Offset(0F, shapeHeight),
                        size = Size(shapeWidth, shapeWidth),
                    ),
                    startAngleDegrees = 180F,
                    sweepAngleDegrees = 180F,
                )
            }

            val body = Path().apply {
                addRect(
                    rect = Rect(
                        offset = Offset(0F, arcRadius),
                        size = Size(shapeWidth, max(shapeHeight - yMinZeroArcHeight, 0F)),
                    ),
                )
            } - Path().apply {
                addArc(
                    oval = Rect(
                        offset = Offset(0F, shapeHeight),
                        size = Size(shapeWidth, shapeWidth),
                    ),
                    startAngleDegrees = 180F,
                    sweepAngleDegrees = 180F,
                )
            }

            // Union cap and body into a single contour (no internal seam when stroked with a border).
            (cap + body)
                .apply {
                    // Rendering bar in negative direction
                    if (isInverted) {
                        inverted(pivotX = shapeWidth / 2F, pivotY = shapeHeight / 2F)
                    }
                }.let(Outline::Generic)
        }
    }
}

/**
 * Rectangle shape with planar/convex shaped sides.
 * Useful for Single Horizontal Bar and Stacked Bars Plot rendering.
 *
 * @param xyGraphScope Provides access to [yAxisModel] and acts as an implementation of [XYGraphScope].
 * @param index Represents the element index within the series.
 * @param value The [HorizontalBarPlotEntry] that defines the cutouts for the [HorizontalPlanoConvexShape].
 * @param barScope The [BarScope] of the composable emitting the bar. It provides the exact pixel geometry of the bar
 * plot, which is needed to cut the shape off at the zero line. If null, the geometry is estimated from the size of the
 * bar, which can be off by a few pixels for short bars close to the zero line.
 */
@Stable
public class HorizontalPlanoConvexShape<X, E : HorizontalBarPlotEntry<Float, X>>(
    private val xyGraphScope: XYGraphScope<Float, X>,
    private val index: Int,
    private val value: E,
    private val barScope: BarScope?,
) : Shape,
    XYGraphScope<Float, X> by xyGraphScope {
    /**
     * Creates the shape without a [BarScope], so the pixel geometry of the bar plot is estimated from the size of the
     * bar.
     */
    public constructor(
        xyGraphScope: XYGraphScope<Float, X>,
        index: Int,
        value: E,
    ) : this(xyGraphScope, index, value, null)

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val shapeWidth = size.width
        val shapeHeight = size.height
        val arcRadius = shapeHeight / 2

        // Rendering negative values
        val isInverted = value.x.end < value.x.start
        // Required for proper bar rendering in waterfall charts
        if (index == 0) {
            val path =
                (DefaultHorizontalPlanoConvexShape.createOutline(size, layoutDirection, density) as Outline.Generic).path
            // Explicitly thread the (optionally inverted) path into the result instead of mutating
            // the wrapped outline in place.
            val result = if (isInverted) {
                path.inverted(pivotX = shapeWidth / 2F, pivotY = shapeHeight / 2F)
            } else {
                path
            }
            return result.let(Outline::Generic)
        }

        val (xZeroOffset, xMinOffset, xMaxOffset) = xAxisModel.xOffsets(value.x.start, value.x.end)

        // Prevent division by zero
        return if (xMaxOffset == xMinOffset) {
            Path().let(Outline::Generic)
        } else {
            // Bars start with a concave path which might go below zero; the respective shape must be cut appropriately
            // Getting the pixel distances of a bar's min and max value from the axis zero line
            val zeroLineDistances = barScope.zeroLineDistances(xZeroOffset, xMinOffset, xMaxOffset, size.width)
            val xMinZeroWidth = zeroLineDistances.start
            val xMaxZeroWidth = zeroLineDistances.end

            // If min and max values are greater than arcRadius, aforementioned below zero compensation is not required
            // and therefore becomes ineffective
            val xMinZeroArcWidth = max((arcRadius - xMinZeroWidth), 0F)
            val xMaxZeroArcWidth = max((arcRadius - xMaxZeroWidth), 0F)

            // Prevent arc from being drawn below zero by subtracting value in degrees
            val xMaxZeroArcWidthDegrees =
                asin(xMaxZeroArcWidth / arcRadius)
                    .rad
                    .toDegrees()
                    .value
                    .toFloat()

            val cap = Path().apply {
                val rectWidth = max((shapeWidth - arcRadius), 0F)
                val widthRadiusOffset = max((arcRadius - shapeWidth), 0F)
                addArc(
                    oval = Rect(
                        offset = Offset(rectWidth - arcRadius - widthRadiusOffset, 0F),
                        size = Size(shapeHeight, shapeHeight),
                    ),
                    startAngleDegrees = 270F + xMaxZeroArcWidthDegrees,
                    sweepAngleDegrees = 180F - 2 * xMaxZeroArcWidthDegrees,
                )
            } - Path().apply {
                addArc(
                    oval = Rect(offset = Offset(-shapeHeight, 0F), size = Size(shapeHeight, shapeHeight)),
                    startAngleDegrees = 270F,
                    sweepAngleDegrees = 180F,
                )
            }

            val body = Path().apply {
                addRect(
                    rect = Rect(
                        offset = Offset(-arcRadius + xMinZeroArcWidth, 0F),
                        size = Size(max(shapeWidth - xMinZeroArcWidth, 0F), shapeHeight),
                    ),
                )
            } - Path().apply {
                addArc(
                    oval = Rect(offset = Offset(-shapeHeight, 0F), size = Size(shapeHeight, shapeHeight)),
                    startAngleDegrees = 270F,
                    sweepAngleDegrees = 180F,
                )
            }

            // Union cap and body into a single contour (no internal seam when stroked with a border).
            (cap + body)
                .apply {
                    // Rendering bar in negative direction
                    if (isInverted) {
                        inverted(pivotX = shapeWidth / 2F, pivotY = shapeHeight / 2F)
                    }
                }.let(Outline::Generic)
        }
    }
}

/**
 * Rectangle shape with convex shaped sides and an additional convex cutout at the bottom.
 * Useful for Single Vertical Bar and Stacked Bars Plot rendering.
 *
 * Primary constructor:
 * @param planoConvexShape The internal shape logic used for rendering.
 * @param index Represents the element index within the series.
 * @param value The [VerticalBarPlotEntry] that defines the cutouts for the [VerticalPlanoConvexShape].
 * @param barScope The [BarScope] of the composable emitting the bar, or null.
 *
 * Secondary constructors:
 * @param xyGraphScope Provides access to [yAxisModel] and acts as an implementation of [XYGraphScope].
 * @param index Represents the element index within the series.
 * @param value The [VerticalBarPlotEntry] used to construct the internal shape as well as the additional convex cutout.
 * @param barScope The [BarScope] of the composable emitting the bar. It provides the exact pixel geometry of the bar
 * plot, which is needed to cut the shape off at the zero line. If null, the geometry is estimated from the size of the
 * bar, which can be off by a few pixels for short bars close to the zero line.
 */
@Stable
public class VerticalBiConvexShape<X, E : VerticalBarPlotEntry<X, Float>> private constructor(
    private val planoConvexShape: VerticalPlanoConvexShape<X, E>,
    private val index: Int,
    private val value: E,
    private val barScope: BarScope?,
) : Shape,
    XYGraphScope<X, Float> by planoConvexShape {
    public constructor(
        xyGraphScope: XYGraphScope<X, Float>,
        index: Int,
        value: E,
        barScope: BarScope?,
    ) : this(
        VerticalPlanoConvexShape(xyGraphScope, index, value, barScope),
        index,
        value,
        barScope,
    )

    /**
     * Creates the shape without a [BarScope], so the pixel geometry of the bar plot is estimated from the size of the
     * bar.
     */
    public constructor(
        xyGraphScope: XYGraphScope<X, Float>,
        index: Int,
        value: E,
    ) : this(xyGraphScope, index, value, null)

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val shapeWidth = size.width
        val shapeHeight = size.height
        val arcRadius = shapeWidth / 2

        // Rendering negative values
        val isInverted = value.y.end < value.y.start
        // Required for proper bar rendering in waterfall charts
        if (index == 0) {
            val path =
                (DefaultVerticalBiConvexShape.createOutline(size, layoutDirection, density) as Outline.Generic).path
            // Explicitly thread the (optionally inverted) path into the result instead of mutating
            // the wrapped outline in place.
            val result = if (isInverted) {
                path.inverted(pivotX = shapeWidth / 2F, pivotY = shapeHeight / 2F)
            } else {
                path
            }
            return result.let(Outline::Generic)
        }

        val (yZeroOffset, yMinOffset, yMaxOffset) = yAxisModel.yOffsets(value.y.start, value.y.end)

        // Prevent division by zero
        return if (yMaxOffset == yMinOffset) {
            Path().let(Outline::Generic)
        } else {
            // Bars start with a concave path which might go below zero; the respective shape must be cut appropriately
            // Getting the pixel distance of a bar's max value from the axis zero line
            val yMaxZeroHeight = barScope.zeroLineDistances(yZeroOffset, yMinOffset, yMaxOffset, size.height).end

            val outline = planoConvexShape.createOutline(size, layoutDirection, density) as Outline.Generic

            val cutoutRect = Path().apply {
                addRect(
                    rect = Rect(
                        offset = Offset(0F, yMaxZeroHeight - arcRadius),
                        size = Size(shapeWidth, shapeWidth),
                    ),
                )
            }
            val cutoutArc = Path().apply {
                addArc(
                    oval = Rect(
                        offset = Offset(0F, yMaxZeroHeight - shapeWidth),
                        size = Size(shapeWidth, shapeWidth),
                    ),
                    startAngleDegrees = 0F,
                    sweepAngleDegrees = 180F,
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

/**
 * Rectangle shape with convex shaped sides and an additional convex cutout at the bottom.
 * Useful for Single Horizontal Bar and Stacked Bars Plot rendering.
 *
 * Primary constructor:
 * @param planoConvexShape The internal shape logic used for rendering.
 * @param index Represents the element index within the series.
 * @param value The [HorizontalBarPlotEntry] that defines the cutouts for the [HorizontalPlanoConvexShape].
 * @param barScope The [BarScope] of the composable emitting the bar, or null.
 *
 * Secondary constructors:
 * @param xyGraphScope Provides access to [yAxisModel] and acts as an implementation of [XYGraphScope].
 * @param index Represents the element index within the series.
 * @param value The [HorizontalBarPlotEntry] used to construct the internal shape
 * as well as the additional convex cutout.
 * @param barScope The [BarScope] of the composable emitting the bar. It provides the exact pixel geometry of the bar
 * plot, which is needed to cut the shape off at the zero line. If null, the geometry is estimated from the size of the
 * bar, which can be off by a few pixels for short bars close to the zero line.
 */
@Stable
public class HorizontalBiConvexShape<X, E : HorizontalBarPlotEntry<Float, X>> private constructor(
    private val planoConvexShape: HorizontalPlanoConvexShape<X, E>,
    private val index: Int,
    private val value: E,
    private val barScope: BarScope?,
) : Shape,
    XYGraphScope<Float, X> by planoConvexShape {
    public constructor(
        xyGraphScope: XYGraphScope<Float, X>,
        index: Int,
        value: E,
        barScope: BarScope?,
    ) : this(
        HorizontalPlanoConvexShape(xyGraphScope, index, value, barScope),
        index,
        value,
        barScope,
    )

    /**
     * Creates the shape without a [BarScope], so the pixel geometry of the bar plot is estimated from the size of the
     * bar.
     */
    public constructor(
        xyGraphScope: XYGraphScope<Float, X>,
        index: Int,
        value: E,
    ) : this(xyGraphScope, index, value, null)

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val shapeWidth = size.width
        val shapeHeight = size.height
        val arcRadius = shapeHeight / 2

        // Rendering negative values
        val isInverted = value.x.end < value.x.start
        // Required for proper bar rendering in waterfall charts
        if (index == 0) {
            val path =
                (DefaultHorizontalBiConvexShape.createOutline(size, layoutDirection, density) as Outline.Generic).path
            // Explicitly thread the (optionally inverted) path into the result instead of mutating
            // the wrapped outline in place.
            val result = if (isInverted) {
                path.inverted(pivotX = shapeWidth / 2F, pivotY = shapeHeight / 2F)
            } else {
                path
            }
            return result.let(Outline::Generic)
        }

        val (xZeroOffset, xMinOffset, xMaxOffset) = xAxisModel.xOffsets(value.x.start, value.x.end)

        // Prevent division by zero
        return if (xMaxOffset == xMinOffset) {
            Path().let(Outline::Generic)
        } else {
            // Bars start with a concave path which might go below zero; the respective shape must be cut appropriately
            // Getting the pixel distance of a bar's min value from the axis zero line
            val xMinZeroWidth = barScope.zeroLineDistances(xZeroOffset, xMinOffset, xMaxOffset, size.width).start

            val outline = planoConvexShape.createOutline(size, layoutDirection, density) as Outline.Generic

            // Like the vertical variant, the cutout extends beyond the zero line. Ending it exactly at the zero
            // line would make its edge coincide with the cap's chord there, which can break the path difference.
            val cutoutRect = Path().apply {
                addRect(
                    rect = Rect(
                        offset = Offset(-xMinZeroWidth - arcRadius, 0F),
                        size = Size(shapeHeight, shapeHeight),
                    ),
                )
            }
            val cutoutArc = Path().apply {
                addArc(
                    oval = Rect(
                        offset = Offset(-xMinZeroWidth, 0F),
                        size = Size(shapeHeight, shapeHeight),
                    ),
                    startAngleDegrees = 90F,
                    sweepAngleDegrees = 180F,
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

private fun Path.inverted(
    pivotX: Float,
    pivotY: Float,
): Path {
    Matrix()
        .apply {
            resetToPivotedTransform(
                pivotX = pivotX,
                pivotY = pivotY,
                rotationZ = 180F,
            )
        }.let(::transform)
    return this
}

private fun AxisModel<Float>.xOffsets(
    xMin: Float,
    xMax: Float,
): XOffsets {
    val xZeroOffset = computeOffset(0F).coerceIn(0F, 1F)
    val xMinOffset = computeOffset(xMin).coerceIn(0f, 1f)
    val xMaxOffset = computeOffset(xMax).coerceIn(0f, 1f)
    return XOffsets(
        xZeroOffset = xZeroOffset,
        xMinOffset = xMinOffset,
        xMaxOffset = xMaxOffset,
    )
}

private fun AxisModel<Float>.yOffsets(
    yMin: Float,
    yMax: Float,
): YOffsets {
    val yZeroOffset = computeOffset(0F).coerceIn(0F, 1F)
    val yMinOffset = computeOffset(yMin).coerceIn(0f, 1f)
    val yMaxOffset = computeOffset(yMax).coerceIn(0f, 1f)
    return YOffsets(
        yZeroOffset = yZeroOffset,
        yMinOffset = yMinOffset,
        yMaxOffset = yMaxOffset,
    )
}

/**
 * Signed distances in pixels from the zero line of the dependent axis to the start and to the end of a bar, measured
 * in the direction in which the bar grows.
 */
private class ZeroLineDistances(
    val start: Float,
    val end: Float,
)

/**
 * Computes the [ZeroLineDistances] of a bar from the relative axis offsets of the zero line and of the bar's start and
 * end.
 *
 * Inside a bar plot, the distances are derived from the same pixel edges as used by its layout, so they are exact.
 * Without the pixel scale of a bar plot it is estimated from the [barLength] in pixels instead, which can be off by a
 * few pixels for bars that are only a few pixels long.
 */
private fun BarScope?.zeroLineDistances(
    zeroOffset: Float,
    startOffset: Float,
    endOffset: Float,
    barLength: Float,
): ZeroLineDistances {
    val pixelScale = (this as? BarScopeImpl)?.pixelScale
    return if (pixelScale != null) {
        // Pixel edges are measured from the start of the axis, so they are mirrored for bars growing toward it
        val direction = if (endOffset >= startOffset) 1 else -1
        val zeroEdge = pixelScale.edge(zeroOffset)
        ZeroLineDistances(
            start = ((pixelScale.edge(startOffset) - zeroEdge) * direction).toFloat(),
            end = ((pixelScale.edge(endOffset) - zeroEdge) * direction).toFloat(),
        )
    } else {
        val pixelsPerOffset = barLength / (endOffset - startOffset)
        ZeroLineDistances(
            start = (startOffset - zeroOffset) * pixelsPerOffset,
            end = (endOffset - zeroOffset) * pixelsPerOffset,
        )
    }
}

private data class XOffsets(
    val xZeroOffset: Float,
    val xMinOffset: Float,
    val xMaxOffset: Float,
)

private data class YOffsets(
    val yZeroOffset: Float,
    val yMinOffset: Float,
    val yMaxOffset: Float,
)
