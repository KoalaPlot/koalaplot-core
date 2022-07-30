package io.github.koalaplot.core.legend

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import io.github.koalaplot.core.theme.KoalaPlotTheme
import kotlin.math.max

/**
 * A flow layout type of legend where each entry will take as much horizontal space as it requires,
 * up to the maximum width of the component. Multiple legend entries will be placed in the same
 * row until all horizontal space is consumed.
 */
@Composable
public fun FlowLegend(
    itemCount: Int,
    symbol: @Composable LegendScope.(item: Int) -> Unit = {},
    label: @Composable LegendScope.(item: Int) -> Unit = {},
    symbolGap: Dp = KoalaPlotTheme.sizes.gap,
    columnGap: Dp = KoalaPlotTheme.sizes.gap,
    rowGap: Dp = KoalaPlotTheme.sizes.gap,
    modifier: Modifier = Modifier
) {
    fun Density.sizeOrPlace(
        placeables: List<Placeable>,
        constraints: Constraints,
        place: Placeable.(Int, Int) -> Unit
    ): IntSize {
        var x = 0
        var y = 0
        var rowHeight = 0
        var width = 0 // total width

        for (i in 0 until itemCount) {
            placeables[i].place(x, y)
            rowHeight = max(rowHeight, placeables[i].height)
            x += placeables[i].width + columnGap.roundToPx()

            if (i == itemCount - 1 ||
                x + placeables[i + 1].width > constraints.maxWidth
            ) { // last item or next item will exceed horizontal size -> end of row
                width = max(width, x - columnGap.roundToPx())
                x = 0
                y += rowGap.roundToPx() + rowHeight
                rowHeight = 0
            }
        }

        return IntSize(width, y - rowGap.roundToPx())
    }

    Layout(modifier = modifier.clipToBounds(), content = {
        for (i in 0 until itemCount) {
            Row {
                Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                    legendScopeInstance.symbol(i)
                }
                Spacer(modifier = Modifier.size(symbolGap))
                Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                    legendScopeInstance.label(i)
                }
            }
        }
    }) { measureables, constraints ->
        val placeables = measureables.map {
            it.measure(constraints)
        }
        val size = sizeOrPlace(placeables, constraints) { _, _ -> }

        layout(size.width, size.height) {
            sizeOrPlace(placeables, constraints) { x, y ->
                this.place(x, y)
            }
        }
    }
}
