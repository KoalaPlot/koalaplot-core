package io.github.koalaplot.core.pie

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Placeable
import io.github.koalaplot.core.util.AngularValue
import io.github.koalaplot.core.util.cos
import io.github.koalaplot.core.util.deg
import io.github.koalaplot.core.util.div
import io.github.koalaplot.core.util.plus
import io.github.koalaplot.core.util.sin
import io.github.koalaplot.core.util.toDegrees
import io.github.koalaplot.core.util.toRadians
import io.github.koalaplot.core.util.y2theta
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

/**
 * Places labels circularly around the outer perimeter of the pie, adjusting them vertically to not overlap with
 * adjacent labels.
 */
public class CircularLabelOffsetProvider(private val labelSpacing: Float) : LabelOffsetProvider {
    override fun computeLabelOffsets(
        pieDiameter: Float,
        placeables: List<Placeable>,
        pieSliceData: List<PieSliceData>
    ): List<LabelOffsets> {
        val labelDiameter = pieDiameter * labelSpacing

        return computeLabelOffsets(
            labelDiameter,
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

    private fun computeLabelOffsets(
        labelDiameter: Float,
        sliceGroups: Map<Quadrant, List<SliceLabelData>>
    ): List<LabelOffsets> {
        // create a mapping of slice index to label offset for sorting back to a list later
        val sliceOffsetMap = HashMap<Int, LabelOffsets>()

        var lastY = Float.POSITIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        sliceGroups[Quadrant.NorthEast]?.reversed()?.forEach {
            val labelOffset = computeLabelOffset(
                it.centerAngle.toRadians(),
                labelDiameter,
                it.labelPlaceable,
                Quadrant.NorthEast,
                lastY
            )
            lastY = labelOffset.position.y
            maxY = max(maxY, labelOffset.position.y + it.labelPlaceable.height)
            sliceOffsetMap[it.index] = labelOffset
        }

        lastY = maxY
        sliceGroups[Quadrant.SouthEast]?.forEach {
            val labelOffset = computeLabelOffset(
                it.centerAngle,
                labelDiameter,
                it.labelPlaceable,
                Quadrant.SouthEast,
                lastY
            )
            lastY = labelOffset.position.y + it.labelPlaceable.height
            sliceOffsetMap[it.index] = labelOffset
        }

        lastY = Float.POSITIVE_INFINITY
        maxY = Float.NEGATIVE_INFINITY
        sliceGroups[Quadrant.NorthWest]?.forEach {
            val labelOffset = computeLabelOffset(
                it.centerAngle,
                labelDiameter,
                it.labelPlaceable,
                Quadrant.NorthWest,
                lastY
            )
            lastY = labelOffset.position.y
            maxY = max(maxY, labelOffset.position.y + it.labelPlaceable.height)
            sliceOffsetMap[it.index] = labelOffset
        }

        lastY = maxY
        sliceGroups[Quadrant.SouthWest]?.reversed()?.forEach {
            val labelOffset = computeLabelOffset(
                it.centerAngle,
                labelDiameter,
                it.labelPlaceable,
                Quadrant.SouthWest,
                lastY
            )
            lastY = labelOffset.position.y + it.labelPlaceable.height
            sliceOffsetMap[it.index] = labelOffset
        }

        return sliceOffsetMap.entries.sortedBy { it.key }.map { it.value }
    }

    /**
     * Computes the position and anchor (line connection) position for a label.
     * @param angle angular position for the label
     * @param labelDiameter The diameter at which to place the label
     * @param placeable The Placeable for the label for which to calculate the position
     * @param quadrant Quadrant in which the label is located
     * @param lastY min or max Y-coordinate of the previous label this label cannot interfere with
     */
    private fun computeLabelOffset(
        angle: AngularValue,
        labelDiameter: Float,
        placeable: Placeable,
        quadrant: Quadrant,
        lastY: Float
    ): LabelOffsets {
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

        return LabelOffsets(offset, anchorPoint, if (isLeftSide) 0.deg else 180.deg)
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

private data class SliceLabelData(
    val index: Int,
    val pieSliceData: PieSliceData,
    val labelPlaceable: Placeable,
    val centerAngle: AngularValue
)
