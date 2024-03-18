package io.github.koalaplot.core.pie

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Placeable
import io.github.koalaplot.core.util.AngularValue

/**
 * This interface is implemented to provide specific layout algorithms for positioning of Pie Slice labels around
 * the outside of the Pie.
 */
public interface LabelPositionProvider {
    /**
     * Computes [ExternalLabelPosition] for every pie slice for a given [pieDiameter] and list of label [placeables].
     *
     * [placeables] and [pieSliceData] are ordered according to the data values provided to PieChart, and the
     * returned [List] of [ExternalLabelPosition] must be in the same order.
     *
     * This function may be called multiple times per composition as part of the process for maximizing the pie
     * diameter and should not rely on the results of prior invocations or save state for use between invocations.
     *
     * @param holeSize A relative size for an inner hole of the pie, creating a donut chart, with a
     *  value between 0 and 1. See [PieChart].
     */
    public fun computeLabelPositions(
        pieDiameter: Float,
        holeSize: Float,
        placeables: List<Placeable>,
        pieSliceData: List<PieSliceData>
    ): List<LabelPosition>
}

/**
 * Specifies positioning values for a Pie Chart slice label.
 */
public sealed interface LabelPosition

/**
 * Specifies positioning values for a Pie Chart slice's external label (the label that is outside the perimeter of
 * the pie).
 *
 * [position] specifies the offset of the label's top-left corner relative to the center of the Pie Chart, which
 * is at the coordinate (0, 0), with positive values down and to the right.
 *
 * [anchorPoint] specifies the point on the label to which a connector line should be drawn, in the same coordinate
 * system as [position].
 *
 * [anchorAngle] specifies the angle at which the connector line should attach to the label at [anchorPoint], where
 * 0 degrees is to the right and angles increase in the clockwise direction.
 */
public data class ExternalLabelPosition(
    val position: Offset,
    val anchorPoint: Offset,
    val anchorAngle: AngularValue
) : LabelPosition

/**
 * Specifies positioning for a Pie Chart slice's internal label (the label is placed within the pie slice).
 *
 *  [position] specifies the offset of the label's top-left corner relative to the center of the Pie Chart, which
 *  is at the coordinate (0, 0), with positive values down and to the right.
 */
public data class InternalLabelPosition(val position: Offset) : LabelPosition

/**
 * Specifies a label that is not positioned, and should not be displayed. This is used, for example, when
 * only internal slice labels should be used and a label does not fit within its slice.
 */
public data object None : LabelPosition

internal fun LabelPosition.getPositionOrNull(): Offset? {
    return when (this) {
        is ExternalLabelPosition -> position
        is InternalLabelPosition -> position
        is None -> null
    }
}
