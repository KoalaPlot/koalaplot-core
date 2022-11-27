package io.github.koalaplot.core.legend

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import io.github.koalaplot.core.theme.KoalaPlotTheme
import kotlin.math.max

private const val ColumnLegendColumns = 3

/**
 * Creates a legend with [itemCount] legend items laid out vertically in a column.
 * Each row of the legend consists of a symbol, label, and value.
 */
@Composable
public fun ColumnLegend(
    itemCount: Int,
    symbol: @Composable LegendScope.(item: Int) -> Unit = {},
    label: @Composable LegendScope.(item: Int) -> Unit = {},
    value: @Composable LegendScope.(item: Int) -> Unit = {},
    rowGap: Dp = KoalaPlotTheme.sizes.gap,
    columnGap: Dp = KoalaPlotTheme.sizes.gap,
    modifier: Modifier = Modifier
) {
    Layout(modifier = modifier.clipToBounds(), content = {
        for (i in 0 until itemCount) {
            legendScopeInstance.symbol(i)
            legendScopeInstance.label(i)
            legendScopeInstance.value(i)
        }
    }) { measureables, constraints ->
        measureColumnLegend(itemCount, measureables, constraints, columnGap, rowGap)
    }
}

private fun MeasureScope.measureColumnLegend(
    itemCount: Int,
    measureables: List<Measurable>,
    constraints: Constraints,
    columnGap: Dp,
    rowGap: Dp
): MeasureResult {
    val rowMeasurables = (0 until itemCount).map {
        val rowIndex = it * ColumnLegendColumns
        Triple(measureables[rowIndex], measureables[rowIndex + 1], measureables[rowIndex + 2])
    }
    val symbolPlaceables = rowMeasurables.map { it.first.measure(constraints) }
    val symbolWidth = symbolPlaceables.maxOf { it.width }

    val valueColConstraint = constraints.copy(
        maxWidth =
        (constraints.maxWidth - (columnGap.toPx() * (ColumnLegendColumns - 1)) - symbolWidth)
            .toInt().coerceAtLeast(constraints.minWidth)
    )
    val valuePlaceables = rowMeasurables.map { it.third.measure(valueColConstraint) }
    val valueWidth = valuePlaceables.maxOf { it.width }

    val labelColConstraint = valueColConstraint.copy(
        maxWidth = (valueColConstraint.maxWidth - valueWidth).coerceAtLeast(
            valueColConstraint.minWidth
        )
    )
    val labelPlaceables = rowMeasurables.map { it.second.measure(labelColConstraint) }
    val labelWidth = labelPlaceables.maxOf { it.width }

    val rowHeights = (0 until itemCount).map {
        max(symbolPlaceables[it].height, max(valuePlaceables[it].height, labelPlaceables[it].height))
    }

    return layout(
        (symbolWidth + valueWidth + labelWidth + (columnGap.toPx() * (ColumnLegendColumns - 1)).toInt())
            .coerceAtMost(constraints.maxWidth),
        (rowHeights.sum() + (rowGap.toPx() * (itemCount - 1)).toInt())
            .coerceAtMost(constraints.maxHeight)
    ) {
        var offset = Offset(0f, 0f)
        for (row in 0 until itemCount) {
            offset = offset.copy(x = 0f)
            placeCell(
                rowMeasurables[row].first,
                symbolPlaceables[row],
                rowHeights[row],
                symbolWidth,
                offset
            )
            offset += Offset(symbolWidth + columnGap.toPx(), 0f)
            placeCell(
                rowMeasurables[row].second,
                labelPlaceables[row],
                rowHeights[row],
                labelWidth,
                offset
            )
            offset += Offset(labelWidth + columnGap.toPx(), 0f)
            placeCell(rowMeasurables[row].third, valuePlaceables[row], rowHeights[row], valueWidth, offset)
            offset += Offset(0f, rowHeights[row] + this.run { rowGap.toPx() })
        }
    }
}

private fun Placeable.PlacementScope.placeCell(
    measurable: Measurable,
    placeable: Placeable,
    rowHeight: Int,
    colWidth: Int,
    offset: Offset,
) {
    val y = measurable.verticalAlignment.align(placeable.height, rowHeight)
    val x = measurable.horizontalAlignment.align(placeable.width, colWidth, LayoutDirection.Ltr)

    placeable.place((offset.x + x).toInt(), (offset.y + y).toInt())
}

/**
 * Parent data associated with children.
 */
internal data class LegendParentData(
    var verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    var horizontalAlignment: Alignment.Horizontal = Alignment.Start,
)

private val Measurable.legendParentData: LegendParentData?
    get() = parentData as? LegendParentData

private val Measurable.verticalAlignment: Alignment.Vertical
    get() = legendParentData?.verticalAlignment ?: Alignment.CenterVertically

private val Measurable.horizontalAlignment: Alignment.Horizontal
    get() = legendParentData?.horizontalAlignment ?: Alignment.Start
