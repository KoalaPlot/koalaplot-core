package io.github.koalaplot.core.pie

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Placeable
import io.github.koalaplot.core.util.AngularValue
import io.github.koalaplot.core.util.PolarCoordinate
import io.github.koalaplot.core.util.cartesianToPolar
import io.github.koalaplot.core.util.cos
import io.github.koalaplot.core.util.deg
import io.github.koalaplot.core.util.div
import io.github.koalaplot.core.util.plus
import io.github.koalaplot.core.util.polarToCartesian
import io.github.koalaplot.core.util.sin
import io.github.koalaplot.core.util.toDegrees
import io.github.koalaplot.core.util.y2theta
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

/**
 * Specifies the strategy for placing Pie chart labels.
 */
public sealed interface PieLabelPlacement {
    /**
     * Specify that all pie chart labels should be placed around the outside of the pie (i.e., not within the
     * individual slices).
     */
    public data object External : PieLabelPlacement

    /**
     * Specify that all pie chart labels should be placed within the individual pie slices. If the label does not
     * fit, it will not be shown.
     *
     * @param radius Radial position of the label Composable relative to the slice outer radius, between 0 and 1
     */
    public data class Internal(val radius: Float = 0.7f) : PieLabelPlacement {
        init {
            require(radius > 0f && radius < 1f) { "radius must be between 0 and 1" }
        }
    }

    /**
     * Specify that pie chart labels should be placed within the individual pie slices if they fit, and if not,
     * then placed externally to the pie.
     *
     * @param radius Radial position of the label Composable relative to the slice outer radius, between 0 and 1
     */
    public data class InternalOrExternal(val radius: Float = 0.7f) : PieLabelPlacement {
        init {
            require(radius > 0f && radius < 1f) { "radius must be between 0 and 1" }
        }
    }
}

/**
 * Places labels circularly around the outer perimeter of the pie, adjusting them vertically to not overlap with
 * adjacent labels.
 *
 * @param labelSpacing A value greater than 1 specifying the distance from the center of
 * the pie at which to place external labels relative to the overall diameter of the pie, where a value
 * of 1 is at the outer edge of the pie. Values between 1.05 and 1.4 tend to work well depending
 * on the size of the labels and overall pie diameter.
 * @param labelPlacement Specifies the locations the labels may be placed, including within the slice or
 * externally. @see [PieLabelPlacement].
 */
public class CircularLabelPositionProvider(
    private val labelSpacing: Float,
    private val labelPlacement: PieLabelPlacement = PieLabelPlacement.External,
) : LabelPositionProvider {
    override fun computeLabelPositions(
        pieDiameter: Float,
        holeSize: Float,
        placeables: List<Placeable>,
        pieSliceData: List<PieSliceData>
    ): List<LabelPosition> {
        return computeLabelPositions(
            PieParameters(pieDiameter, holeSize, labelPlacement),
            groupLabels(placeables, pieSliceData)
        )
    }

    // Group slice labels by their quadrant, with related data
    private fun groupLabels(
        labelPlaceables: List<Placeable>,
        pieSliceDatas: List<PieSliceData>,
    ): Map<Quadrant, List<SliceLabelData>> {
        val sliceGroups = HashMap<Quadrant, MutableList<SliceLabelData>>()

        labelPlaceables.forEachIndexed { index, placeable ->
            val centerAngle = pieSliceDatas[index].startAngle + (pieSliceDatas[index].angle / 2f)

            val matchingQuadrant = Quadrant.entries.first {
                it.angleRange.contains(centerAngle.toDegrees().value)
            }
            sliceGroups[matchingQuadrant] =
                (sliceGroups.getOrElse(matchingQuadrant) { ArrayList() }).apply {
                    add(SliceLabelData(index, pieSliceDatas[index], placeable, centerAngle))
                }
        }

        return sliceGroups
    }

    private class YState(var last: Float, var max: Float)

    private fun computeInQuadrant(
        pieParameters: PieParameters,
        sliceLabelData: SliceLabelData,
        quadrant: Quadrant,
        yState: YState
    ): LabelPosition {
        var pos1: LabelPosition? = null
        if (pieParameters.labelPlacement !is PieLabelPlacement.External) {
            // Try internal placement first
            pos1 = computeInternalLabelOffset(pieParameters, sliceLabelData)
        }

        if (pos1 == null || (pos1 is None && pieParameters.labelPlacement is PieLabelPlacement.InternalOrExternal)) {
            // do external label placement
            pos1 = computeExternalLabelOffset(pieParameters, sliceLabelData, quadrant, yState.last)

            if (quadrant.isNorth()) {
                yState.last = pos1.position.y
                // update maxY
                yState.max = max(yState.max, pos1.position.y + sliceLabelData.labelPlaceable.height)
            } else {
                // update lastY
                yState.last = pos1.position.y + sliceLabelData.labelPlaceable.height
            }
        }

        return pos1
    }

    private fun computeLabelPositions(
        pieParameters: PieParameters,
        sliceGroups: Map<Quadrant, List<SliceLabelData>>,
    ): List<LabelPosition> {
        // create a mapping of slice index to label offset for sorting back to a list later
        val sliceOffsetMap = HashMap<Int, LabelPosition>()

        // yState keeps track of the last external label's y-axis position so the next
        // external label can position itself without interfering
        val yState = YState(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)
        sliceGroups[Quadrant.NorthEast]?.reversed()?.forEach {
            sliceOffsetMap[it.index] = computeInQuadrant(pieParameters, it, Quadrant.NorthEast, yState)
        }

        yState.last = yState.max
        sliceGroups[Quadrant.SouthEast]?.forEach {
            sliceOffsetMap[it.index] = computeInQuadrant(pieParameters, it, Quadrant.SouthEast, yState)
        }

        yState.last = Float.POSITIVE_INFINITY
        yState.max = Float.NEGATIVE_INFINITY
        sliceGroups[Quadrant.NorthWest]?.forEach {
            sliceOffsetMap[it.index] = computeInQuadrant(pieParameters, it, Quadrant.NorthWest, yState)
        }

        yState.last = yState.max
        sliceGroups[Quadrant.SouthWest]?.reversed()?.forEach {
            sliceOffsetMap[it.index] = computeInQuadrant(pieParameters, it, Quadrant.SouthWest, yState)
        }

        return sliceOffsetMap.entries.sortedBy { it.key }.map { it.value }
    }

    /**
     * Computes the position and anchor (line connection) position for a label when external label placement
     * is used.
     * @param sliceLabelData slice label data
     * @param quadrant Quadrant in which the label is located
     * @param lastY min or max Y-coordinate of the previous label this label cannot interfere with
     */
    private fun computeExternalLabelOffset(
        pieParameters: PieParameters,
        sliceLabelData: SliceLabelData,
        quadrant: Quadrant,
        lastY: Float,
    ): ExternalLabelPosition {
        val angle = sliceLabelData.centerAngle
        val placeable = sliceLabelData.labelPlaceable
        val labelDiameter = pieParameters.pieDiameter * labelSpacing
        val isLeftSide = quadrant.isWest()
        val labelTop = if (quadrant.isSouth()) {
            max(labelDiameter / 2f * sin(angle).toFloat() - placeable.height / 2.0F, lastY)
        } else {
            min(
                labelDiameter / 2f * sin(angle).toFloat() - placeable.height / 2.0F,
                lastY - placeable.height
            )
        }
        val labelBottom = labelTop + placeable.height

        val thetas1 = y2theta(labelTop, labelDiameter / 2f)
        val thetas2 = y2theta(labelBottom, labelDiameter / 2f)

        // xLimit is the x-axis position closest to the center of the pie allowed
        val xLimit = (labelDiameter / 2f) * cos(angle).toFloat()

        // thetas1 or thetas2 could be NaN if the y-coordinate was larger than labelDiameter / 2
        val xOffset =
            if (isLeftSide) {
                val xMin1 = if (thetas1.first.isNaN()) {
                    Float.POSITIVE_INFINITY
                } else {
                    labelDiameter / 2f * min(cos(thetas1.first), cos(thetas1.second))
                }

                val xMin2 = if (thetas2.first.isNaN()) {
                    Float.POSITIVE_INFINITY
                } else {
                    labelDiameter / 2f * min(cos(thetas2.first), cos(thetas2.second))
                }

                if (xMin1.isInfinite() && xMin2.isInfinite()) {
                    xLimit
                } else {
                    min(min(xMin1, xMin2), xLimit)
                }
            } else {
                val xMax1 = if (thetas1.first.isNaN()) {
                    Float.NEGATIVE_INFINITY
                } else {
                    labelDiameter / 2f * max(cos(thetas1.first), cos(thetas1.second))
                }

                val xMax2 = if (thetas2.first.isNaN()) {
                    Float.NEGATIVE_INFINITY
                } else {
                    labelDiameter / 2f * max(cos(thetas2.first), cos(thetas2.second))
                }

                if (xMax1.isInfinite() && xMax2.isInfinite()) {
                    xLimit
                } else {
                    max(max(xMax1, xMax2), xLimit)
                }
            }

        var offset = Offset(xOffset, labelTop)

        // left side labels need x-position shifted left by their width
        if (isLeftSide) {
            offset += Offset(-placeable.width.toFloat(), 0f)
        }

        val anchorPoint = offset + Offset(0f, placeable.height / 2f) + if (isLeftSide) {
            Offset(placeable.width.toFloat(), 0f)
        } else {
            Offset(0f, 0f)
        }

        return ExternalLabelPosition(offset, anchorPoint, if (isLeftSide) 0.deg else 180.deg)
    }

    /**
     * Computes the position and anchor (line connection) position for a label when internal label placement
     * is used.
     */
    private fun computeInternalLabelOffset(
        pieParameters: PieParameters,
        sliceLabelData: SliceLabelData,
    ): LabelPosition {
        val pieSliceData = sliceLabelData.pieSliceData
        val placeable = sliceLabelData.labelPlaceable

        val radius = when (pieParameters.labelPlacement) {
            is PieLabelPlacement.Internal -> pieParameters.labelPlacement.radius
            is PieLabelPlacement.InternalOrExternal -> pieParameters.labelPlacement.radius
            else -> return None
        }

        val innerRadius = pieParameters.pieDiameter / 2f * pieParameters.holeSize
        val outerRadius = pieParameters.pieDiameter / 2f

        val labelCenter = polarToCartesian(
            outerRadius * radius,
            pieSliceData.startAngle + (pieSliceData.angle.toDegrees().value / 2f).deg
        )

        // test if the 4 corners of the placeable are contained within the slice area
        val c1 = cartesianToPolar(Offset(labelCenter.x - placeable.width / 2, labelCenter.y - placeable.height / 2))
        val c2 = cartesianToPolar(Offset(labelCenter.x + placeable.width / 2, labelCenter.y - placeable.height / 2))
        val c3 = cartesianToPolar(Offset(labelCenter.x - placeable.width / 2, labelCenter.y + placeable.height / 2))
        val c4 = cartesianToPolar(Offset(labelCenter.x + placeable.width / 2, labelCenter.y + placeable.height / 2))
        val bounded = isBounded(innerRadius, outerRadius, pieSliceData, c1) &&
            isBounded(innerRadius, outerRadius, pieSliceData, c2) &&
            isBounded(innerRadius, outerRadius, pieSliceData, c3) &&
            isBounded(innerRadius, outerRadius, pieSliceData, c4)

        return if (bounded) {
            InternalLabelPosition(Offset(labelCenter.x - placeable.width / 2, labelCenter.y - placeable.height / 2))
        } else {
            None
        }
    }

    /**
     * Tests if the point [pt] is within the bounds of the slice.
     */
    @Suppress("MagicNumber")
    private fun isBounded(
        innerRadius: Float,
        outerRadius: Float,
        pieSliceData: PieSliceData,
        pt: PolarCoordinate
    ): Boolean {
        // normalize start angle to between 0 and 360
        val startNorm = ((pieSliceData.startAngle.toDegrees().value % 360.0) + 360.0) % 360.0
        val endNorm = startNorm + pieSliceData.angle.toDegrees().value // end angle > 0

        var pointAngle = pt.angle.toDegrees().value
        while (pointAngle < startNorm) {
            pointAngle += 360.0
        }

        return pointAngle < endNorm && pt.radius < outerRadius && pt.radius > innerRadius
    }
}

/**
 * Defines 4 quadrants of a circle, and the angle range that each occupies, in degrees going
 * clockwise.
 */
@Suppress("MagicNumber")
private enum class Quadrant(val angleRange: ClosedFloatingPointRange<Float>) {
    NorthEast(-90f..0f),
    SouthEast(0f..90f),
    SouthWest(90f..180f),
    NorthWest(180f..270f)
}

private fun Quadrant.isWest(): Boolean {
    return this == Quadrant.NorthWest || this == Quadrant.SouthWest
}

private fun Quadrant.isSouth(): Boolean {
    return this == Quadrant.SouthWest || this == Quadrant.SouthEast
}

private fun Quadrant.isNorth(): Boolean {
    return this == Quadrant.NorthWest || this == Quadrant.NorthEast
}

private data class SliceLabelData(
    val index: Int,
    val pieSliceData: PieSliceData,
    val labelPlaceable: Placeable,
    val centerAngle: AngularValue,
)

private data class PieParameters(
    val pieDiameter: Float,
    val holeSize: Float,
    val labelPlacement: PieLabelPlacement
)
