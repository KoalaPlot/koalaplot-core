package io.github.koalaplot.core.heatmap

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import io.github.koalaplot.core.animation.StartAnimationUseCase
import io.github.koalaplot.core.style.KoalaPlotTheme
import io.github.koalaplot.core.xygraph.Point
import io.github.koalaplot.core.xygraph.XYGraphScope
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

public typealias HeatMapGrid<Z> = Array<Array<Z>>

@Composable
public fun <X : Comparable<X>, Y : Comparable<Y>, Z> XYGraphScope<X, Y>.HeatMapPlot(
    xDomain: ClosedRange<X>,
    yDomain: ClosedRange<Y>,
    bins: HeatMapGrid<Z>,
    colorScale: (Z) -> Color,
    alphaScale: (Z) -> Float = { 1f },
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec,
) {
    if (bins.isEmpty() || bins[0].isEmpty()) return

    val beta = remember { Animatable(0f) }
    LaunchedEffect(null) { beta.animateTo(1f, animationSpec = animationSpec) }

    val xBins = bins.size
    val yBins = bins[0].size

    Canvas(modifier = Modifier.fillMaxSize()) {
        fun mapX(x: X): Float = xAxisModel.computeOffset(x) * size.width

        fun mapY(y: Y): Float = yAxisModel.computeOffset(y) * size.height

        fun <T : Comparable<T>> sortPair(
            a: T,
            b: T,
        ): Pair<T, T> = if (a <= b) a to b else b to a

        val (left, right) = sortPair(
            mapX(xDomain.start),
            mapX(xDomain.endInclusive),
        )
        val (top, bottom) = sortPair(
            mapY(yDomain.start),
            mapY(yDomain.endInclusive),
        )

        // Pre-calculate cell size
        val cellWidth = (right - left) / xBins
        val cellHeight = (top - bottom) / yBins
        val cellSize = Size(
            beta.value * abs(cellWidth),
            beta.value * abs(cellHeight),
        )
        val animationOffset = (1f - beta.value) / 2f

        for (xi in 0 until xBins) {
            @Suppress("LoopWithTooManyJumpStatements")
            for (yi in 0 until yBins) {
                val value = bins[xi][yi] ?: continue

                val alpha = alphaScale(value) * beta.value
                if (alpha <= 0f) continue

                val cellColor = colorScale(value)
                val cellLeft = left + (xi + animationOffset) * cellWidth
                val cellTop = bottom + (yi + 1 + animationOffset) * cellHeight

                drawRect(
                    color = cellColor,
                    topLeft = Offset(cellLeft, cellTop),
                    size = cellSize,
                    alpha = alpha,
                )
            }
        }
    }
}
