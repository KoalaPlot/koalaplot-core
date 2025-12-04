package io.github.koalaplot.core.pie

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import io.github.koalaplot.core.util.div
import io.github.koalaplot.core.util.maximize
import io.github.koalaplot.core.util.plus
import io.github.koalaplot.core.util.polarToCartesian
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * @param forceCenteredPie If true, will force the pie to be centered in the parent component by adjusting its size to
 * accommodate asymmetric label sizes and positions.
 */
internal class PieMeasurePolicy constructor(
    private val pieSliceData: List<PieSliceData>,
    private val holeSize: Float,
    private val labelPositionProvider: LabelPositionProvider,
    private val initOuterRadius: Float,
    private val forceCenteredPie: Boolean = false,
) {
    internal fun MeasureScope.layoutPie(
        size: Size,
        labelPositions: List<LabelPosition>,
        labelConnectorTranslations: List<Offset?>,
        pieDiameter: Float,
        piePlaceables: PiePlaceables,
    ) = layout(size.width.toInt(), size.height.toInt()) {
        val translation = if (forceCenteredPie) {
            Offset(size.width / 2, size.height / 2)
        } else {
            val positions = labelPositions.mapNotNull { it.getPositionOrNull() }
            Offset(
                max(-(positions.minOfOrNull { it.x } ?: 0f), pieDiameter / 2),
                max(-(positions.minOfOrNull { it.y } ?: 0f), pieDiameter / 2),
            )
        }

        piePlaceables.labelConnectors.forEachIndexed { index, placeable ->
            labelConnectorTranslations[index]?.let {
                val position = translation - it
                placeable.place(position.x.toInt(), position.y.toInt())
            }
        }

        piePlaceables.pie.place(
            (translation.x - pieDiameter / 2).toInt(),
            (translation.y - pieDiameter / 2).toInt(),
        )

        piePlaceables.labels.forEachIndexed { index, placeable ->
            labelPositions[index].getPositionOrNull()?.let {
                val position: Offset = it + translation
                placeable.place(position.x.toInt(), position.y.toInt())
            }
        }

        val position: Offset = translation - Offset(
            piePlaceables.hole.width / 2f,
            piePlaceables.hole.height / 2f,
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
     * @param labelPositions The label positions
     * @param pieDiameter The pie chart diameter
     * @return The translation required to be applied to each label connector so the computed
     * labelConnectorScope values result in the connector being correctly positioned. A null entry indicates
     * that the label does not require a connector.
     */
    internal fun computeLabelConnectorScopes(
        labelPositions: List<LabelPosition>,
        pieDiameter: Float,
    ): List<Pair<Offset, LabelConnectorScope>?> = buildList {
        for (i in labelPositions.indices) {
            if (labelPositions[i] is ExternalLabelPosition) {
                val labelConnectorScope = LabelConnectorScopeImpl()
                with(labelConnectorScope) {
                    startAngle.value = pieSliceData[i].startAngle + (pieSliceData[i].angle / 2f)
                    startPosition.value = polarToCartesian(pieDiameter / 2f * initOuterRadius, startAngle.value)
                    endPosition.value = (labelPositions[i] as ExternalLabelPosition).anchorPoint
                    endAngle.value = (labelPositions[i] as ExternalLabelPosition).anchorAngle

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
            } else {
                add(null)
            }
        }
    }

    internal fun measure(
        pie: Measurable,
        labels: List<Measurable>,
        constraints: Constraints,
        minPieDiameterPx: Float,
        maxPieDiameterPx: Float,
    ): Triple<Float, Placeable, List<Placeable>> {
        val labelConstraint = constraints.copy(
            maxWidth = ((constraints.maxWidth - minPieDiameterPx) / 2)
                .toInt()
                .coerceAtLeast(constraints.minWidth),
        )

        val labelPlaceables = labels.map {
            it.measure(labelConstraint)
        }

        // use floor for PieDiameter because below the size is set via constraints which is an integer
        val pieDiameter = floor(
            findMaxDiameter(constraints, labelPlaceables, minPieDiameterPx).coerceIn(minPieDiameterPx, maxPieDiameterPx),
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
        minAllowedPieDiameter: Float,
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
        val labelOffsets = labelPositionProvider.computeLabelPositions(
            test.toFloat(),
            holeSize,
            labelPlaceables,
            pieSliceData,
        )
        val s = computeSize(labelPlaceables, labelOffsets, test.toFloat())
        return (s.width < constraints.maxWidth && s.height < constraints.maxHeight)
    }

    /**
     * Computes the size required to contain the pie + its surrounding labels based on the
     * labels as represented by the placeables, their labelOffsets, and the pieDiameter.
     */
    internal fun computeSize(
        placeables: List<Placeable>,
        labelPositions: List<LabelPosition>,
        pieDiameter: Float,
    ): Size {
        // Compute height/width required for the pie plus all labels
        // calculate min/max label extents used for computing overall component width/height
        var minX = -pieDiameter / 2f // minimum x-coordinate of all objects
        var maxX = pieDiameter / 2f // max x-coordinate of all objects
        var minY = -pieDiameter / 2f // minimum y-coordinate of all objects
        var maxY = pieDiameter / 2f // max y-coordinate of all objects
        placeables.forEachIndexed { index, placeable ->
            labelPositions[index].getPositionOrNull()?.let {
                minX = min(minX, it.x)
                maxX = max(maxX, it.x + placeable.width)
                minY = min(minY, it.y)
                maxY = max(maxY, it.y + placeable.height)
            }
        }

        val width = if (forceCenteredPie) {
            2 * max(abs(maxX), abs(minX)) // this works because the label positions were based on a pie center at (0,0)
        } else {
            maxX - minX
        }
        val height = if (forceCenteredPie) {
            2 * max(abs(maxY), abs(minY)) // this works because the label positions were based on a pie center at (0,0)
        } else {
            maxY - minY
        }

        return Size(width, height)
    }
}
