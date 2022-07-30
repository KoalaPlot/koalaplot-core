package io.github.koalaplot.core.legend

import androidx.compose.foundation.layout.LayoutScopeMarker
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.unit.Density

/**
 * Identifies the location at which to place a chart's legend.
 */
public enum class LegendLocation {
    NONE, LEFT, TOP, RIGHT, BOTTOM
}

/**
 * Scope for the children of a [ColumnLegend] or [GridLegend].
 */
@LayoutScopeMarker
public interface LegendScope {
    /**
     * Align the element vertically within the cell.
     */
    public fun Modifier.align(alignment: Alignment.Vertical): Modifier = this.then(
        VerticalAlignModifier(
            vertical = alignment,
        )
    )

    /**
     * Align the element horizontally within the cell.
     */
    public fun Modifier.align(alignment: Alignment.Horizontal): Modifier = this.then(
        HorizontalAlignModifier(horizontal = alignment)
    )
}

internal val legendScopeInstance = object : LegendScope {}

private class VerticalAlignModifier(val vertical: Alignment.Vertical) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any =
        ((parentData as? LegendParentData) ?: LegendParentData()).also {
            it.verticalAlignment = vertical
        }
}

private class HorizontalAlignModifier(val horizontal: Alignment.Horizontal) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any =
        ((parentData as? LegendParentData) ?: LegendParentData()).also {
            it.horizontalAlignment = horizontal
        }
}
