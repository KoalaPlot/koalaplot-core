package io.github.koalaplot.core.pie

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Placeable
import io.github.koalaplot.core.util.AngularValue

/**
 * This interface is implemented to provide specific layout algorithms for positioning of Pie Slice labels around
 * the outside of the Pie.
 */
public interface LabelOffsetProvider {
    /**
     * Computes [LabelOffsets] for every pie slice for a given [pieDiameter] and list of label [placeables].
     * [placeables] and [pieSliceData] are ordered according to the data values provided to PieChart, and the
     * returned [List] of [LabelOffsets] must be in the same order.
     *
     * This function may be called multiple times per composition as part of the process for maximizing the pie
     * diameter and should not rely on the results of prior invocations or save state for use between invocations.
     */
    public fun computeLabelOffsets(
        pieDiameter: Float,
        placeables: List<Placeable>,
        pieSliceData: List<PieSliceData>
    ): List<LabelOffsets>
}

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
public data class LabelOffsets(val position: Offset, val anchorPoint: Offset, val anchorAngle: AngularValue)
