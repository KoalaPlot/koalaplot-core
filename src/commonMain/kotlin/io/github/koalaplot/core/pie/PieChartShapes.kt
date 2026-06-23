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
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.util.deg
import io.github.koalaplot.core.util.min
import io.github.koalaplot.core.util.moveTo
import io.github.koalaplot.core.util.polarToCartesian
import io.github.koalaplot.core.util.rad
import io.github.koalaplot.core.util.toDegrees
import io.github.koalaplot.core.util.toRadians
import kotlin.math.asin
import kotlin.math.max
import kotlin.math.sin

/**
 * A semicircle shaped pie chart slice implementation that can form full slices as well as slices
 * with a "hole" for donut charts. Each slice features a convex shape on both its starting and ending sides.
 *
 * It is intended to be used in donut charts where the holeSize is significantly greater than 0;
 * otherwise, the resulting slice shape may appear visually unbalanced.
 *
 * @receiver Provides drawing and interaction parameters for the slice scope
 * @param color The Color of the Slice
 * @param modifier The modifier to be applied to this item
 * @param clickable If clicking should be enabled.
 * @param antiAlias Set to true if the slice should be drawn with anti-aliasing, false otherwise
 * @param gap Specifies the gap between slices. It is the angular distance, in degrees, between the
 * start/stop values the slice represents and where the slice is actually drawn. Cannot be negative.
 * @param onClick handler of clicks on the slice
 */
@ExperimentalKoalaPlotApi
@Composable
public fun PieSliceScope.BiConvexSlice(
    color: Color,
    modifier: Modifier = Modifier,
    clickable: Boolean = false,
    antiAlias: Boolean = false,
    gap: Float = 0.0f,
    onClick: () -> Unit = {},
) {
    require(gap >= 0F) { "gap cannot be negative" }
    val shape = BiConvexSlice(
        pieSliceData.startAngle
            .toDegrees()
            .value
            .toFloat() + gap,
        pieSliceData.angle
            .toDegrees()
            .value
            .toFloat() - 2 * gap,
        innerRadius,
        outerRadius,
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                drawIntoCanvas {
                    val path = (shape.createOutline(size, layoutDirection, this) as Outline.Generic).path

                    // draw slice
                    it.drawPath(
                        path,
                        Paint().apply {
                            isAntiAlias = antiAlias
                            this.color = color
                        },
                    )
                }
                drawContent()
            }.clip(shape)
            .then(
                if (clickable) {
                    Modifier.clickable(
                        enabled = true,
                        role = Role.Button,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            ),
    ) {}
}

/**
 * A semicircle shaped pie chart slice implementation that can form full slices as well as slices
 * with a "hole" for donut charts. The endings of each slice consist of a concave and a convex shape.
 *
 * @receiver Provides drawing and interaction parameters for the slice scope
 * @param color The Color of the Slice
 * @param modifier The modifier to be applied to this item
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
    clickable: Boolean = false,
    antiAlias: Boolean = false,
    gap: Float = 0.0f,
    onClick: () -> Unit = {},
) {
    require(gap >= 0F) { "gap cannot be negative" }
    val shape = ConcaveConvexSlice(
        pieSliceData.startAngle
            .toDegrees()
            .value
            .toFloat() + gap,
        pieSliceData.angle
            .toDegrees()
            .value
            .toFloat() - 2 * gap,
        innerRadius,
        outerRadius,
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                drawIntoCanvas {
                    val path = (shape.createOutline(size, layoutDirection, this) as Outline.Generic).path

                    // draw slice
                    it.drawPath(
                        path,
                        Paint().apply {
                            isAntiAlias = antiAlias
                            this.color = color
                        },
                    )
                }
                drawContent()
            }.clip(shape)
            .then(
                if (clickable) {
                    Modifier.clickable(
                        enabled = true,
                        role = Role.Button,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            ),
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
    private val outerRadius: Float = 1.0F,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val radius = size.width / 2f * outerRadius
        val holeRadius = size.width / 2f * innerRadius
        val center = Offset(size.width / 2f, size.width / 2f)

        val innerRect = Rect(center, holeRadius)
        val outerRect = Rect(center, radius)

        // Clamp to a valid range; sweeps > 360° degenerate the same way as exactly 360°.
        val sweepAngle = angle.coerceIn(0f, 360f)
        val innerCircleRadius = (radius - holeRadius) / 2f
        val innerCircleCenterRadius = (radius + holeRadius) / 2f

        return Path().apply {
            // Full-circle special case: a 360° sweep cannot be drawn as an arc.
            // Skia reduces an arc with a 360° sweep to a single point — the start and
            // end unit vectors coincide, so arcTo()/addArc() emit only a moveTo/lineTo
            // and no curve — which would render nothing at all. The 0.01f tolerance also
            // catches sweeps that land just below 360° through float accumulation (e.g.
            // value * 360f / total): those are equally fragile and would otherwise leave
            // a seam with overlapping caps instead of a closed ring.
            // A full ring has no start/end caps, so draw the outer and inner ovals and
            // cut the hole with the even-odd fill rule.
            if (sweepAngle >= 360f - 0.01f) {
                addOval(outerRect)
                if (holeRadius > 0f) addOval(innerRect)
                fillType = PathFillType.EvenOdd
                return@apply
            }

            // Outer arc. forceMoveTo = true starts a fresh contour at the arc's start
            // point; on an empty Path the current point is the origin, so 'false' here
            // would draw a stray line from (0, 0).
            arcTo(
                rect = outerRect,
                startAngleDegrees = startAngle,
                sweepAngleDegrees = sweepAngle,
                forceMoveTo = true
            )

            // Convex cap at the end angle, bridging the outer to the inner radius. It is
            // a half-circle (InnerCircleSweepAngleDegrees == 180°), so its endpoints land
            // exactly on the inner/outer radius — which is what lets the arcs chain.
            // forceMoveTo = false from here on keeps everything in ONE contour: a single
            // closed path fills correctly, whereas separate subpaths each self-close on
            // fill and produce the overlapping wedge artifacts.
            arcTo(
                rect = Rect(
                    center = center + polarToCartesian(
                        radius = innerCircleCenterRadius,
                        angle = (startAngle + sweepAngle).deg,
                    ),
                    radius = innerCircleRadius,
                ),
                startAngleDegrees = startAngle + sweepAngle,
                sweepAngleDegrees = InnerCircleSweepAngleDegrees,
                forceMoveTo = false,
            )

            // Inner arc, traversed backwards (negative sweep) so the contour stays
            // continuous; it ends back on the inner radius at the start angle.
            arcTo(
                rect = innerRect,
                startAngleDegrees = startAngle + sweepAngle,
                sweepAngleDegrees = -sweepAngle,
                forceMoveTo = false
            )

            // Concave cap at the start angle, traversed backwards (inner -> outer) to
            // close the loop. Its end point coincides with the outer arc's start point.
            arcTo(
                rect = Rect(
                    center = center + polarToCartesian(
                        radius = innerCircleCenterRadius,
                        angle = startAngle.deg,
                    ),
                    radius = innerCircleRadius,
                ),
                startAngleDegrees = startAngle + InnerCircleSweepAngleDegrees,
                sweepAngleDegrees = -InnerCircleSweepAngleDegrees,
                forceMoveTo = false,
            )

            // Seal the contour. The end already meets the start, so this adds at most a
            // zero-length line, but it marks the seam as a join (not two stroke caps) and
            // guarantees a watertight region for fill and clip().
            close()
        }.let(Outline::Generic)
    }
}

/**
 * Creates a pie chart slice shape with a total angular extent of [angle] degrees with an
 * optional holeSize that is specified as a percentage of the overall slice radius.
 * The pie diameter is equal to the Shape's size width. The slice is positioned with its vertex
 * at the center.
 *
 * Each slice features a convex shape on both its starting and ending sides.
 * For very small values, the slice gradually transitions into a shrinking circle to ensure accurate rendering
 * and maintain the intended visual appearance.
 */
private class BiConvexSlice(
    private val startAngle: Float,
    private val angle: Float,
    private val innerRadius: Float = 0.5F,
    private val outerRadius: Float = 1.0F,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
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
            asin(innerCircleRadius / innerCircleCenterRadius)
                .rad
                .toDegrees()
                .value
                .toFloat()

        val outerPieSweepAngle = max(sweepAngle - 2 * innerCircleDegrees, 0F)
        val outerPie = Path().apply {
            moveTo(center)
            arcTo(
                rect = outerRect,
                startAngleDegrees = startAngle + innerCircleDegrees,
                sweepAngleDegrees = outerPieSweepAngle,
                forceMoveTo = false,
            )
        }

        val innerPieSweepAngle = max(sweepAngle - 2 * innerCircleDegrees, 0F)
        val innerPie = Path().apply {
            moveTo(center)
            arcTo(
                rect = innerRect,
                startAngleDegrees = startAngle + innerCircleDegrees,
                sweepAngleDegrees = innerPieSweepAngle,
                forceMoveTo = false,
            )
        }

        // The following calculations ensure that for very small sweep angles (thin slices),
        // the donut slice gradually transitions into a smaller circular shape.
        // This prevents rendering artifacts and keeps the shape visually consistent.
        // As a result, the slice consists of two convex arcs forming a smooth circular shape.

        // Calculates the maximum radius of the inner circle.
        // This corresponds to the opposite side of the triangle:
        // Max inner circle radius = opposite side = sin(angle) * hypotenuse
        val maxInnerCircleRadius =
            sin((sweepAngle / 2).deg.toRadians().value).toFloat() * innerCircleCenterRadius

        val resultingInnerCircleRadius =
            min(innerCircleRadius, maxInnerCircleRadius)

        val resultingInnerCircleDegrees =
            min(innerCircleDegrees, sweepAngle / 2)

        val convexPaths = Path().apply {
            addArc(
                oval = Rect(
                    center = center + polarToCartesian(
                        radius = innerCircleCenterRadius,
                        angle = (startAngle + resultingInnerCircleDegrees).deg,
                    ),
                    radius = resultingInnerCircleRadius,
                ),
                startAngleDegrees = startAngle + resultingInnerCircleDegrees,
                sweepAngleDegrees = -InnerCircleSweepAngleDegrees,
            )
            addArc(
                oval = Rect(
                    center = center + polarToCartesian(
                        radius = innerCircleCenterRadius,
                        angle = (startAngle + sweepAngle - resultingInnerCircleDegrees).deg,
                    ),
                    radius = resultingInnerCircleRadius,
                ),
                startAngleDegrees = startAngle + sweepAngle - resultingInnerCircleDegrees,
                sweepAngleDegrees = InnerCircleSweepAngleDegrees,
            )
        }

        return Path()
            .apply {
                addPath(outerPie - innerPie)
                addPath(convexPaths)
            }.let(Outline::Generic)
    }
}

private const val InnerCircleSweepAngleDegrees = 180F
