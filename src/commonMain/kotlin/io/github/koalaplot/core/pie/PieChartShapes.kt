package io.github.koalaplot.core.pie

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
 * @param hoverExpandFactor Amount the slice expands when hovered. 1 is no expansion, values greater
 * than 1 expand outward from the pie, and values less than 1 shrink. If expansion on hover is
 * desired, a good starting value is 1.05.
 * @param hoverElement Content to show when the mouse/pointer hovers over the slice
 * @param clickable If clicking should be enabled.
 * @param antiAlias Set to true if the slice should be drawn with anti-aliasing, false otherwise
 * start/stop values the slice represents and where the slice is actually drawn.
 * @param onClick handler of clicks on the slice
 */
@ExperimentalKoalaPlotApi
@Composable
public fun PieSliceScope.ConcaveConvexSlice(
    color: Color,
    modifier: Modifier = Modifier,
    hoverExpandFactor: Float = 1.0f,
    hoverElement: @Composable () -> Unit = {},
    clickable: Boolean = false,
    antiAlias: Boolean = false,
    gap: Float = 0.0f,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val targetOuterRadius by animateFloatAsState(outerRadius * if (isHovered) hoverExpandFactor else 1f)

    val shape = ConcaveConvexSlice(
        pieSliceData.startAngle.toDegrees().value.toFloat() + gap,
        pieSliceData.angle.toDegrees().value.toFloat() - 2 * gap,
        innerRadius,
        targetOuterRadius
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
            .hoverable(interactionSource)
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

        // Gap can lead to negative sweep angle which causes rendering issues
        val sweepAngle = max(0F, angle)
        val innerCircleRadius = (radius - holeRadius) / 2F
        val innerCircleCenterRadius = (radius + holeRadius) / 2F

        val innerCircleDegrees =
            asin(innerCircleRadius / innerCircleCenterRadius).rad.toDegrees().value.toFloat()

        val concaveRingSlice = concaveRingSlice(
            center = center,
            innerRect = innerRect,
            outerRect = outerRect,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            innerCircleCenterRadius = innerCircleCenterRadius,
            innerCircleDegrees = innerCircleDegrees,
            innerCircleRadius = innerCircleRadius,
        )

        val ringSlice = ringSlice(
            innerRect = innerRect,
            outerRect = outerRect,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            innerCircleDegrees = innerCircleDegrees
        )

        val convexRingSlice = convexRingSlice(
            center = center,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            innerCircleCenterRadius = innerCircleCenterRadius,
            innerCircleDegrees = innerCircleDegrees,
            innerCircleRadius = innerCircleRadius,
        )

        return Path().apply {
            addPath(convexRingSlice)
            addPath(ringSlice)
            addPath(concaveRingSlice)
        }.let(Outline::Generic)
    }
}

/**
 * Path provider function for concave part of ring/donut slice.
 *
 * @param center The center of the pie chart.
 * @param innerRect Rect corresponding to pie chart's hole.
 * @param outerRect Rect corresponding to pie chart's outer radius.
 * @param startAngle The start angle of the slice.
 * @param sweepAngle The sweepAngle of the slice.
 * @param innerCircleCenterRadius Radius pointing to average of outer and inner radius.
 * @param innerCircleDegrees Angle from center which encompasses slice's inner circle.
 * @param innerCircleRadius Radius of slice's inner circle.
 */
private fun concaveRingSlice(
    center: Offset,
    innerRect: Rect,
    outerRect: Rect,
    startAngle: Float,
    sweepAngle: Float,
    innerCircleCenterRadius: Float,
    innerCircleDegrees: Float,
    innerCircleRadius: Float,
): Path {
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
    val innerCircleSweepAngleDegrees = 180F

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
            sweepAngleDegrees = innerCircleSweepAngleDegrees
        )
    }
    return slice - convexSemicircle
}

/**
 * Path provider function for ring part of ring/donut slice.
 * Returns empty path if slice consists only of concave/convex pieces.
 *
 * @param innerRect Rect corresponding to pie chart's hole.
 * @param outerRect Rect corresponding to pie chart's outer radius.
 * @param startAngle The start angle of the slice.
 * @param sweepAngle The sweepAngle of the slice.
 * @param innerCircleDegrees Angle from center which encompasses slice's inner circle.
 */
private fun ringSlice(
    innerRect: Rect,
    outerRect: Rect,
    startAngle: Float,
    sweepAngle: Float,
    innerCircleDegrees: Float
): Path {
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
 * @param center The center of the pie chart.
 * @param startAngle The start angle of the slice.
 * @param sweepAngle The sweepAngle of the slice.
 * @param innerCircleCenterRadius Radius pointing to average of outer and inner radius.
 * @param innerCircleDegrees Angle from center which encompasses slice's inner circle.
 * @param innerCircleRadius Radius of slice's inner circle.
 */
private fun convexRingSlice(
    center: Offset,
    startAngle: Float,
    sweepAngle: Float,
    innerCircleCenterRadius: Float,
    innerCircleDegrees: Float,
    innerCircleRadius: Float,
): Path {
    val toInnerCircleDegrees = (startAngle + sweepAngle - innerCircleDegrees / 2F)
    val innerCircleSweepAngleDegrees = 180F

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
            sweepAngleDegrees = innerCircleSweepAngleDegrees
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
                sweepAngleDegrees = innerCircleSweepAngleDegrees
            )
        }

        return convexSemicircle - concaveSemicircle
    }
    return convexSemicircle
}
