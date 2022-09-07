package io.github.koalaplot.core.pie

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import io.github.koalaplot.core.util.DEG2RAD
import io.github.koalaplot.core.util.maximize
import io.github.koalaplot.core.util.pol2Cart
import io.github.koalaplot.core.util.y2theta
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

internal data class LabelOffsets(val position: Offset, val anchorPoint: Offset)

internal class PieMeasurePolicy constructor(
    private val pieSliceData: List<PieSliceData>,
    private val labelSpacing: Float,
    private val initOuterRadius: Float
) {
    internal fun MeasureScope.layoutPie(
        size: Size,
        labelOffsets: List<LabelOffsets>,
        labelConnectorTranslations: List<Offset>,
        pieDiameter: Float,
        piePlaceables: PiePlaceables
    ) = layout(size.width.toInt(), size.height.toInt()) {
        val translation =
            Offset(
                max(-(labelOffsets.minOfOrNull { it.position.x } ?: 0f), pieDiameter / 2),
                max(-(labelOffsets.minOfOrNull { it.position.y } ?: 0f), pieDiameter / 2)
            )
        piePlaceables.labels.forEachIndexed { index, placeable ->
            val position: Offset = labelOffsets[index].position + translation
            placeable.place(position.x.toInt(), position.y.toInt())
        }

        piePlaceables.labelConnectors.forEachIndexed { index, placeable ->
            val position = translation - labelConnectorTranslations[index]
            placeable.place(position.x.toInt(), position.y.toInt())
        }

        piePlaceables.pie.place(
            (translation.x - pieDiameter / 2).toInt(),
            (translation.y - pieDiameter / 2).toInt()
        )

        val position: Offset = translation - Offset(
            piePlaceables.hole.width / 2f,
            piePlaceables.hole.height / 2f
        )
        piePlaceables.hole.place(position.x.toInt(), position.y.toInt())
    }

    internal data class PiePlaceables(
        val pie: Placeable,
        val hole: Placeable,
        val labels: List<Placeable>,
        val labelConnectors: List<Placeable>,
    )

    /**
     * Calculates offsets and LabelConnectorScopes for label connectors.
     *
     * @param labelOffsets The label positions
     * @param pieDiameter The pie chart diameter
     * @return The translation required to be applied to each label connector so the computed
     * labelConnectorScope values result in the connector being correctly positioned.
     */
    internal fun computeLabelConnectorScopes(
        labelOffsets: List<LabelOffsets>,
        pieDiameter: Float
    ): List<Pair<Offset, LabelConnectorScope>> {
        return buildList {
            for (i in labelOffsets.indices) {
                val labelConnectorScope = LabelConnectorScopeImpl()
                with(labelConnectorScope) {
                    startPosition.value = pol2Cart(
                        pieDiameter / 2f * initOuterRadius,
                        pieSliceData[i].startAngle + pieSliceData[i].angleExtent / 2f
                    )
                    endPosition.value = labelOffsets[i].anchorPoint
                    startAngle.value =
                        pieSliceData[i].startAngle + pieSliceData[i].angleExtent / 2f
                    @Suppress("MagicNumber")
                    endAngle.value = if (startPosition.value.x <= endPosition.value.x) {
                        180f
                    } else {
                        0f
                    }

                    // Shift label connector coordinates to a bounding box with top left at 0, 0
                    // and compute the translation required to position it correctly.
                    // Position connector right/down within the bounding box (by pieDiameter/2)
                    // to prevent clipping of
                    // curves bending to negative coordinates when using modifier.alpha().
                    // From the Modifier.alpha documentation:
                    // "Note when an alpha less than 1.0f is provided, contents are implicitly
                    // clipped to their bounds."
                    val left = min(startPosition.value.x, endPosition.value.x)
                    val top = min(startPosition.value.y, endPosition.value.y)
                    val translate = Offset(-left + pieDiameter / 2, -top + pieDiameter / 2)
                    startPosition.value += translate
                    endPosition.value += translate

                    add(Pair(translate, labelConnectorScope))
                }
            }
        }
    }

    internal fun measure(
        pie: Measurable,
        labels: List<Measurable>,
        constraints: Constraints,
        minPieDiameterPx: Float,
        maxPieDiameterPx: Float
    ): Triple<Float, Placeable, List<Placeable>> {
        val labelPlaceables = labels.map {
            it.measure(
                constraints.copy(
                    maxWidth = ((constraints.maxWidth - minPieDiameterPx) / 2).toInt()
                        .coerceAtLeast(constraints.minWidth)
                )
            )
        }

        // use floor for PieDiameter because below the size is set via constraints
        // which is an integer
        val pieDiameter = floor(
            findMaxDiameter(constraints, labelPlaceables, minPieDiameterPx).coerceIn(minPieDiameterPx, maxPieDiameterPx)
        )

        val piePlaceable =
            pie.measure(Constraints.fixed(pieDiameter.toInt(), pieDiameter.toInt()))

        return Triple(pieDiameter, piePlaceable, labelPlaceables)
    }

    /**
     * Performs a binary search to find the maximum Pie diameter that will fit within the
     * constraints.
     */
    private fun findMaxDiameter(
        constraints: Constraints,
        labelPlaceables: List<Placeable>,
        minAllowedPieDiameter: Float
    ): Float {
        val maxPieDiameter =
            max(min(constraints.maxWidth, constraints.maxHeight).toFloat(), minAllowedPieDiameter)

        return maximize(minAllowedPieDiameter.toDouble(), maxPieDiameter.toDouble()) {
            if (it <= minAllowedPieDiameter) {
                true
            } else {
                checkDiameter(it, labelPlaceables, constraints)
            }
        }.toFloat()
    }

    private fun checkDiameter(
        test: Double,
        labelPlaceables: List<Placeable>,
        constraints: Constraints,
    ): Boolean {
        val labelOffsets: List<Offset> =
            computeLabelOffsets(test.toFloat(), labelPlaceables).map {
                it.position
            }
        val s = computeSize(labelPlaceables, labelOffsets, test.toFloat())
        return (s.width < constraints.maxWidth && s.height < constraints.maxHeight)
    }

    /**
     * Computes the size required to contain the pie + its surrounding labels based on the
     * labels as represented by the placeables, their labelOffsets, and the pieDiameter.
     */
    internal fun computeSize(
        placeables: List<Placeable>,
        labelOffsets: List<Offset>,
        pieDiameter: Float
    ): Size {
        // Compute height/width required for the pie plus all labels
        // calculate min/max label extents used for computing overall component width/height
        var minX = -pieDiameter / 2f // minimum x-coordinate of all objects
        var maxX = pieDiameter / 2f // max x-coordinate of all objects
        var minY = -pieDiameter / 2f // minimum y-coordinate of all objects
        var maxY = pieDiameter / 2f // max y-coordinate of all objects
        placeables.forEachIndexed { index, placeable ->
            minX = min(minX, labelOffsets[index].x)
            maxX = max(maxX, labelOffsets[index].x + placeable.width)
            minY = min(minY, labelOffsets[index].y)
            maxY = max(maxY, labelOffsets[index].y + placeable.height)
        }

        val width = maxX - minX
        val height = maxY - minY

        return Size(width, height)
    }

    /**
     * Computes where labels should be placed around a pie chart based on the diameter of the pie,
     * pieDiameter, the labels themselves as represented by placeables, and the individual pie
     * slice angles. Returns a list of Offsets representing the x, y coordinate
     * for each label where the center of the pie is at the origin (0, 0).
     */
    internal fun computeLabelOffsets(
        pieDiameter: Float,
        placeables: List<Placeable>
    ): List<LabelOffsets> {
        val labelDiameter = pieDiameter * labelSpacing

        return computeLabelOffsets(
            labelDiameter,
            groupLabels(placeables, pieSliceData)
        )
    }
}

internal const val AngleCCWTop = -90f

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
    val centerAngle: Float
)

// Group slice labels by their quadrant, with related data
private fun groupLabels(
    labelPlaceables: List<Placeable>,
    pieSliceDatas: List<PieSliceData>,
): Map<Quadrant, List<SliceLabelData>> {
    val sliceGroups = HashMap<Quadrant, MutableList<SliceLabelData>>()

    labelPlaceables.forEachIndexed { index, placeable ->
        val centerAngle = pieSliceDatas[index].startAngle + pieSliceDatas[index].angleExtent / 2

        val matchingQuadrant = Quadrant.values().first {
            it.angleRange.contains(centerAngle)
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
            it.centerAngle * DEG2RAD,
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
            it.centerAngle * DEG2RAD,
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
            it.centerAngle * DEG2RAD,
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
            it.centerAngle * DEG2RAD,
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
 * @param angleRad angular position for the label in radians
 * @param labelDiameter The diameter at which to place the label
 * @param placeable The Placeable for the label for which to calculate the position
 * @param quadrant Quadrant in which the label is located
 * @param lastY min or max Y-coordinate of the previous label this label cannot interfere with
 */
private fun computeLabelOffset(
    angleRad: Double,
    labelDiameter: Float,
    placeable: Placeable,
    quadrant: Quadrant,
    lastY: Float
): LabelOffsets {
    val isLeftSide = quadrant.isWest()
    val labelTop = if (quadrant.isSouth()) {
        max(labelDiameter / 2f * sin(angleRad).toFloat() - placeable.height / 2.0F, lastY)
    } else {
        min(
            labelDiameter / 2f * sin(angleRad).toFloat() - placeable.height / 2.0F,
            lastY - placeable.height
        )
    }
    val labelBottom = labelTop + placeable.height

    val thetas1 = y2theta(labelTop, labelDiameter / 2f)
    val thetas2 = y2theta(labelBottom, labelDiameter / 2f)

    // xLimit is the x-axis position closest to the center of the pie allowed
    val xLimit = (labelDiameter / 2f) * cos(angleRad).toFloat()

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
    return LabelOffsets(offset, anchorPoint)
}
