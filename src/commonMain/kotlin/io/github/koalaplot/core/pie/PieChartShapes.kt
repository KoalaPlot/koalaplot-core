package io.github.koalaplot.core.pie

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.util.deg
import io.github.koalaplot.core.util.polarToCartesian
import io.github.koalaplot.core.util.rad
import io.github.koalaplot.core.util.toDegrees
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.max

/**
 * A semicircle shaped pie chart slice implementation that can form full slices as well as slices
 * with a "hole" for donut charts. The endings of each slice consist of a concave and a convex shape.
 *
 * @receiver Provides drawing and interaction parameters for the slice scope
 * @param color The Color of the Slice
 * @param modifier The modifier to be applied to this item
 * @param hoverElement Content to show when the mouse/pointer hovers over the slice
 * @param clickable If clicking should be enabled.
 * @param antiAlias Set to true if the slice should be drawn with anti-aliasing, false otherwise
 * @param gap Specifies the gap between slices. It is the angular distance, in degrees, between the
 * start/stop values the slice represents and where the slice is actually drawn. Cannot be negative.
 * @param onClick handler of clicks on the slice
 */
@ExperimentalKoalaPlotApi
@Composable
public fun PieSliceScope.ConcaveConvexSlice(
    color: Color,
    modifier: Modifier = Modifier,
    hoverElement: @Composable () -> Unit = {},
    clickable: Boolean = false,
    antiAlias: Boolean = false,
    gap: Float = 0.0f,
    onClick: () -> Unit = {}
) {
    require(gap >= 0F) { "gap cannot be negative" }
    val shape = ConcaveConvexSlice(
        pieSliceData.startAngle.toDegrees().value.toFloat() + gap,
        pieSliceData.angle.toDegrees().value.toFloat() - 2 * gap,
        innerRadius,
        outerRadius
    )

    Box(
        modifier = modifier.fillMaxSize()
            .drawWithContent {
                drawIntoCanvas {
                    val path = (shape.createOutline(size, layoutDirection, this) as Outline.Generic).path

                    // draw slice
                    it.drawPath(
                        path,
                        Paint().apply {
                            isAntiAlias = antiAlias
                            this.color = color
                        }
                    )
                }
                drawContent()
            }.clip(shape)
            .then(
                if (clickable) {
                    Modifier.clickable(
                        enabled = true,
                        role = Role.Button,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .hoverableElement(hoverElement)
    ) {}
}

/**
 * Creates a pie chart slice shape with a total angular extent of [angle] degrees with an
 * optional holeSize that is specified as a percentage of the overall slice radius.
 * The pie diameter is equal to the Shape's size width. The slice is positioned with its vertex
 * at the center.
 *
 * The slice shape starts with a concave and ends with a convex shape.
 */
private class ConcaveConvexSlice(
    private val startAngle: Float,
    private val angle: Float,
    private val innerRadius: Float = 0.5F,
    private val outerRadius: Float = 1.0F
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val radius = size.width / 2F * outerRadius
        val holeRadius = size.width / 2F * innerRadius
        val center = Offset(size.width / 2F, size.width / 2F)

        val innerRect = Rect(center, holeRadius)
        val outerRect = Rect(center, radius)
        val layout = Layout(
            center = center,
            innerRect = innerRect,
            outerRect = outerRect
        )

        // Gap can lead to negative sweep angle which causes rendering issues
        val sweepAngle = max(0F, angle)
        val innerCircleRadius = (radius - holeRadius) / 2F
        val innerCircleCenterRadius = (radius + holeRadius) / 2F

        val innerCircleDegrees =
            asin(innerCircleRadius / innerCircleCenterRadius).rad.toDegrees().value.toFloat()
        val innerCircle = InnerCircle(
            innerCircleCenterRadius = innerCircleCenterRadius,
            innerCircleDegrees = innerCircleDegrees,
            innerCircleRadius = innerCircleRadius
        )

        val concaveRingSlice = concaveRingSlice(
            layout = layout,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            innerCircle = innerCircle
        )

        val ringSlice = ringSlice(
            layout = layout,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            innerCircle = innerCircle
        )

        val convexRingSlice = convexRingSlice(
            layout = layout,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            innerCircle = innerCircle
        )

        return Path().apply {
            addPath(convexRingSlice)
            addPath(ringSlice)
            addPath(concaveRingSlice)
        }.let(Outline::Generic)
    }
}

private const val InnerCircleSweepAngleDegrees = 180F

/**
 * Path provider function for concave part of ring/donut slice.
 *
 * @param layout Specifies layout of pie chart.
 * @param startAngle The start angle of the slice.
 * @param sweepAngle The sweepAngle of the slice.
 * @param innerCircle Specifies shape of concave/convex part of ring/donut slice.
 */
private fun concaveRingSlice(
    layout: Layout,
    startAngle: Float,
    sweepAngle: Float,
    innerCircle: InnerCircle
): Path {
    val (center, innerRect, outerRect) = layout
    val (innerCircleCenterRadius, innerCircleDegrees, innerCircleRadius) = innerCircle
    val deltaSmallSweepAngle =
        if (sweepAngle < innerCircleDegrees) abs(sweepAngle - innerCircleDegrees) else 0F

    val toOuterStartAngleDegrees = startAngle - innerCircleDegrees / 2F
    val toInnerStartAngleDegrees = startAngle + innerCircleDegrees / 2F - deltaSmallSweepAngle
    val outerSweepAngleDegrees = innerCircleDegrees - deltaSmallSweepAngle

    val slice = Path().apply {
        arcTo(
            rect = outerRect,
            startAngleDegrees = toOuterStartAngleDegrees,
            sweepAngleDegrees = outerSweepAngleDegrees,
            false
        )
        arcTo(
            rect = innerRect,
            startAngleDegrees = toInnerStartAngleDegrees,
            sweepAngleDegrees = -outerSweepAngleDegrees,
            false
        )
    }

    val toInnerCircleDegrees = startAngle - (innerCircleDegrees / 2F)

    val convexSemicircle = Path().apply {
        addArc(
            oval = Rect(
                center = center + polarToCartesian(
                    radius = innerCircleCenterRadius,
                    angle = toInnerCircleDegrees.deg
                ),
                radius = innerCircleRadius
            ),
            startAngleDegrees = toInnerCircleDegrees,
            sweepAngleDegrees = InnerCircleSweepAngleDegrees
        )
    }
    return slice - convexSemicircle
}

/**
 * Path provider function for ring part of ring/donut slice.
 * Returns empty path if slice consists only of concave/convex pieces.
 *
 * @param layout Specifies layout of pie chart.
 * @param startAngle The start angle of the slice.
 * @param sweepAngle The sweepAngle of the slice.
 * @param innerCircle Specifies shape of concave/convex part of ring/donut slice.
 */
private fun ringSlice(
    layout: Layout,
    startAngle: Float,
    sweepAngle: Float,
    innerCircle: InnerCircle
): Path {
    val (_, innerRect, outerRect) = layout
    val (_, innerCircleDegrees, _) = innerCircle
    if (sweepAngle <= innerCircleDegrees) return Path()

    val toOuterStartAngleDegrees = startAngle + (innerCircleDegrees / 2F)
    val toInnerStartAngleDegrees = startAngle + sweepAngle - innerCircleDegrees / 2F
    val outerSweepAngleDegrees = sweepAngle - innerCircleDegrees

    return Path().apply {
        addArc(
            oval = outerRect,
            startAngleDegrees = toOuterStartAngleDegrees,
            sweepAngleDegrees = outerSweepAngleDegrees
        )
        arcTo(
            rect = innerRect,
            startAngleDegrees = toInnerStartAngleDegrees,
            sweepAngleDegrees = -outerSweepAngleDegrees,
            forceMoveTo = false
        )
    }
}

/**
 * Path provider function for convex part of ring/donut slice.
 *
 * @param layout Specifies layout of pie chart.
 * @param startAngle The start angle of the slice.
 * @param sweepAngle The sweepAngle of the slice.
 * @param innerCircle Specifies shape of concave/convex part of ring/donut slice.
 */
private fun convexRingSlice(
    layout: Layout,
    startAngle: Float,
    sweepAngle: Float,
    innerCircle: InnerCircle
): Path {
    val (center, _, _) = layout
    val (innerCircleCenterRadius, innerCircleDegrees, innerCircleRadius) = innerCircle
    val toInnerCircleDegrees = (startAngle + sweepAngle - innerCircleDegrees / 2F)

    val convexSemicircle = Path().apply {
        addArc(
            oval = Rect(
                center = center + polarToCartesian(
                    radius = innerCircleCenterRadius,
                    angle = toInnerCircleDegrees.deg
                ),
                radius = innerCircleRadius
            ),
            startAngleDegrees = toInnerCircleDegrees,
            sweepAngleDegrees = InnerCircleSweepAngleDegrees
        )
    }

    if (sweepAngle < innerCircleDegrees) {
        val toConcaveInnerCircleDegrees = startAngle - (innerCircleDegrees / 2F)
        val concaveSemicircle = Path().apply {
            addArc(
                oval = Rect(
                    center = center + polarToCartesian(
                        radius = innerCircleCenterRadius,
                        angle = toConcaveInnerCircleDegrees.deg
                    ),
                    radius = innerCircleRadius
                ),
                startAngleDegrees = toConcaveInnerCircleDegrees,
                sweepAngleDegrees = InnerCircleSweepAngleDegrees
            )
        }

        return convexSemicircle - concaveSemicircle
    }
    return convexSemicircle
}

/**
 * Parameter class specifying layout of concave/convex shaped pie chart slices.
 *
 * @param center The center of the pie chart.
 * @param innerRect Rect corresponding to pie chart's hole.
 * @param outerRect Rect corresponding to pie chart's outer radius.
 */
private data class Layout(
    val center: Offset,
    val innerRect: Rect,
    val outerRect: Rect
)

/**
 * Parameter class providing inner circle values specifying shape of concave/convex part of ring/donut slice.
 *
 * @param innerCircleCenterRadius Radius pointing to average of outer and inner radius.
 * @param innerCircleDegrees Angle from center which encompasses slice's inner circle.
 * @param innerCircleRadius Radius of slice's inner circle.
 */
private data class InnerCircle(
    val innerCircleCenterRadius: Float,
    val innerCircleDegrees: Float,
    val innerCircleRadius: Float,
)
