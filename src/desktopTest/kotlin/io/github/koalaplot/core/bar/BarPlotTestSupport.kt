package io.github.koalaplot.core.bar

import androidx.compose.ui.graphics.PixelMap
import io.github.koalaplot.core.xygraph.AxisModel
import io.github.koalaplot.core.xygraph.AxisState
import io.github.koalaplot.core.xygraph.XYGraphScope

/**
 * Minimal [XYGraphScope] for rendering bar plots without an XYGraph, so that the plot area covers the whole window.
 */
internal class TestGraphScope<X, Y>(
    override val xAxisModel: AxisModel<X>,
    override val yAxisModel: AxisModel<Y>,
) : XYGraphScope<X, Y> {
    override val xAxisState: AxisState = object : AxisState {
        override val majorTickOffsets: List<Float> = emptyList()
        override val minorTickOffsets: List<Float> = emptyList()
    }
    override val yAxisState: AxisState = xAxisState
}

/**
 * Returns true if the pixel at [x], [y] shows the white background.
 */
internal fun PixelMap.isBackground(
    x: Int,
    y: Int,
): Boolean = this[x, y].let { it.red > 0.95f && it.green > 0.95f && it.blue > 0.95f }
