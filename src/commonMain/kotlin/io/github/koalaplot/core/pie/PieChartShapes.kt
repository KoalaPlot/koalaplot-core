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
import io.github.koalaplot.core.util.polarToCartesian
import io.github.koalaplot.core.util.rad
import io.github.koalaplot.core.util.toDegrees
import io.github.koalaplot.core.util.toRadians
import kotlin.math.asin
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
 * Each slice features a convex shape on both its starting and ending sides.
 * For very small values, the slice gradually transitions into a shrinking circle to ensure accurate rendering
 * and maintain the intended visual appearance.
 *
 * The convex caps round the angular ends of a ring and therefore require a non-zero ring
 * thickness. When [innerRadius] is 0 there is no ring to round, so a holeless slice falls back to
 * a plain pie sector: two straight radii from the center to the rim joined by the outer arc,
 * yielding sharp angular corners instead of rounded ones. This makes the shape usable for solid
 * pie charts as well as donut charts. The shrinking-circle behavior for small values applies to
 * the donut case only; a thin sector has no caps that could collide and is simply drawn as a
 * narrow wedge.
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

        // Clamp to a valid range; sweeps > 360° degenerate the same way as exactly 360°.
        val sweepAngle = angle.coerceIn(0f, FullAngleDegrees)
        val innerCircleRadius = (radius - holeRadius) / 2F
        val innerCircleCenterRadius = (radius + holeRadius) / 2F

        // Half-angle that one cap circle subtends from the donut center. The cap circle
        // (radius innerCircleRadius, centered at innerCircleCenterRadius) is seen under
        // tangent lines at ±asin(r / R). Each cap is inset by this angle so the rounded
        // slice occupies exactly [startAngle, startAngle + sweepAngle] and never spills
        // into the neighboring slice.
        val innerCircleDegrees = asin(innerCircleRadius / innerCircleCenterRadius).rad.toDegrees().value.toFloat()

        return Path().apply {
            // Full-circle special case: a 360° sweep cannot be drawn as an arc. Skia reduces
            // it to a single point — start and end unit vectors coincide, so arcTo()/addArc()
            // emit only a moveTo/lineTo and no curve — which would render nothing at all. The
            // 0.01f tolerance also catches sweeps that land just below 360° through float
            // accumulation (e.g. value * 360f / total): those are equally fragile and would
            // otherwise leave a seam with overlapping caps instead of a closed shape. A full
            // circle has no caps, so draw the outer oval (plus the inner oval for donuts) and
            // cut the hole with the even-odd fill rule.
            if (sweepAngle >= FullAngleDegrees - FullAngleToleranceDegrees) {
                addOval(outerRect)
                if (holeRadius > 0f) addOval(innerRect)
                fillType = PathFillType.EvenOdd
                return@apply
            }

            // Pie mode (no hole): the convex caps round the angular ends of a *ring*, so they
            // are undefined without a ring thickness. At holeRadius == 0 the cap radius would
            // be radius / 2 and innerCircleDegrees would be 90°, which both bulges the ends into
            // blobs and pushes the thin-slice threshold up to 180°, so most slices would
            // mis-render. A holeless slice is therefore drawn as a plain sector: forceMoveTo =
            // false turns the implicit move-to-arc-start into the first radius (center -> rim),
            // the arc draws the outer edge, and close() draws the second radius back to center.
            if (holeRadius <= 0f) {
                moveTo(center.x, center.y)
                arcTo(rect = outerRect, startAngleDegrees = startAngle, sweepAngleDegrees = sweepAngle, forceMoveTo = false)
                close()
                return@apply
            }

            // Thin-slice fallback (donut only). Below this threshold the two convex caps meet
            // and the outer arc's sweep (sweepAngle - 2 * innerCircleDegrees) would be <= 0, so
            // the four-arc path would self-overlap and produce artifacts. Draw a single circle
            // instead, sized to shrink smoothly toward a point as the sweep approaches zero. At
            // exactly the threshold the two semicircular caps coincide into this circle, so the
            // transition into the arc-based shape is seamless.
            if (sweepAngle <= 2 * innerCircleDegrees) {
                // Largest circle that fits inside the angular wedge, tangent to both slice
                // edges. In the right triangle (center -> cap center -> edge tangent point) the
                // wedge half-angle sits at the center, innerCircleCenterRadius is the hypotenuse,
                // and the inscribed radius is the opposite side = sin(sweep / 2) * hypotenuse.
                val maxInnerCircleRadius = sin((sweepAngle / 2).deg.toRadians().value).toFloat() * innerCircleCenterRadius

                // Never exceed the ring thickness (the regular cap radius).
                val resultingInnerCircleRadius = min(innerCircleRadius, maxInnerCircleRadius)

                // Center offset from startAngle: half the sweep puts the circle on the slice's
                // angular bisector (clamped so it never moves past the regular cap position).
                val resultingInnerCircleDegrees = min(innerCircleDegrees, sweepAngle / 2)

                addOval(
                    oval = Rect(
                        center = center + polarToCartesian(
                            radius = innerCircleCenterRadius,
                            angle = (startAngle + resultingInnerCircleDegrees).deg,
                        ),
                        radius = resultingInnerCircleRadius,
                    )
                )
                return@apply
            }

            // Outer arc, shortened by innerCircleDegrees at each end to leave room for the caps.
            // forceMoveTo = true starts a fresh contour at the arc's start point; on an empty
            // Path the current point is the origin, so 'false' here would draw a stray line
            // from (0, 0).
            arcTo(
                rect = outerRect,
                startAngleDegrees = startAngle + innerCircleDegrees,
                sweepAngleDegrees = sweepAngle - 2 * innerCircleDegrees,
                forceMoveTo = true
            )

            // Convex end cap: a half-circle (assumes InnerCircleSweepAngleDegrees == 180°)
            // centered on the mid-radius and tangent to both the outer and inner circle at this
            // angle, so its endpoints land exactly on outerRect and innerRect. forceMoveTo =
            // false from here on keeps everything in ONE contour: a single closed path fills
            // correctly, whereas separate subpaths each self-close on fill and produce the
            // overlapping-wedge artifacts.
            arcTo(
                rect = Rect(
                    center = center + polarToCartesian(
                        radius = innerCircleCenterRadius,
                        angle = (startAngle + sweepAngle - innerCircleDegrees).deg,
                    ),
                    radius = innerCircleRadius,
                ),
                startAngleDegrees = startAngle + sweepAngle - innerCircleDegrees,
                sweepAngleDegrees = InnerCircleSweepAngleDegrees,
                forceMoveTo = false,
            )

            // Inner arc, traversed backwards (negative sweep) so the contour stays continuous;
            // it ends back on the inner radius where the start cap begins.
            arcTo(
                rect = innerRect,
                startAngleDegrees = startAngle + sweepAngle - innerCircleDegrees,
                sweepAngleDegrees = -(sweepAngle - 2 * innerCircleDegrees),
                forceMoveTo = false
            )

            // Convex start cap, mirroring the end cap. Its end point coincides with the outer
            // arc's start point, closing the loop.
            arcTo(
                rect = Rect(
                    center = center + polarToCartesian(
                        radius = innerCircleCenterRadius,
                        angle = (startAngle + innerCircleDegrees).deg,
                    ),
                    radius = innerCircleRadius,
                ),
                startAngleDegrees = startAngle + innerCircleDegrees + InnerCircleSweepAngleDegrees,
                sweepAngleDegrees = InnerCircleSweepAngleDegrees,
                forceMoveTo = false,
            )

            // Seal the contour. The end already meets the start, so this adds at most a
            // zero-length line, but it marks the seam as a join (not two-stroke caps) and
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
        val sweepAngle = angle.coerceIn(0f, FullAngleDegrees)
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
            if (sweepAngle >= FullAngleDegrees - FullAngleToleranceDegrees) {
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
            // zero-length line, but it marks the seam as a join (not two-stroke caps) and
            // guarantees a watertight region for fill and clip().
            close()
        }.let(Outline::Generic)
    }
}

private const val InnerCircleSweepAngleDegrees = 180F
private const val FullAngleDegrees = 360f
private const val FullAngleToleranceDegrees = 0.01f
