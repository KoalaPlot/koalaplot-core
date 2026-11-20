package io.github.koalaplot.core.bar

import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import io.github.koalaplot.core.animation.StartAnimationUseCase
import io.github.koalaplot.core.xygraph.CategoryAxisModel
import io.github.koalaplot.core.xygraph.FloatLinearAxisModel
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertTrue

private const val GroupedPlotLength = 800
private const val GroupedPlotThickness = 600
private const val GroupedAnimationSteps = 5

// The axis also covers negative values, so the zero line lies inside the plot area
private val groupedValueRange = -2f..8f

private val quarters = listOf("Q1", "Q2", "Q3", "Q4")

// Bar lengths per series and quarter, including bars shorter than the arc radius
private val groupedSeries = listOf(
    listOf(3f, 5f, 2f, 6f),
    listOf(4f, 1f, 0.3f, 5f),
    listOf(2f, 6f, 4f, 0.2f),
)

private val seriesColors = listOf(Color(0xFF1565C0), Color(0xFFEF6C00), Color(0xFF2E7D32))

/**
 * Renders grouped bar plots with plano-convex and bi-convex bars and checks that every bar is drawn as one piece
 * between the zero line and its end, which requires each bar shape to receive the position of its own bar.
 */
@OptIn(ExperimentalTestApi::class)
class GroupedBarPlotShapeTest {
    @Test
    fun verticalPlanoConvexBarsAreComplete() = assertCompleteBars(vertical = true, biConvex = false)

    @Test
    fun verticalBiConvexBarsAreComplete() = assertCompleteBars(vertical = true, biConvex = true)

    @Test
    fun horizontalPlanoConvexBarsAreComplete() = assertCompleteBars(vertical = false, biConvex = false)

    @Test
    fun horizontalBiConvexBarsAreComplete() = assertCompleteBars(vertical = false, biConvex = true)

    private fun assertCompleteBars(
        vertical: Boolean,
        biConvex: Boolean,
    ) {
        val width = if (vertical) GroupedPlotThickness else GroupedPlotLength
        val height = if (vertical) GroupedPlotLength else GroupedPlotThickness
        val valueAxis = FloatLinearAxisModel(groupedValueRange)
        val failures = mutableListOf<String>()

        runDesktopComposeUiTest(width = width, height = height) {
            val startAnimation = StartAnimationUseCase(StartAnimationUseCase.ExecutionType.None, snap())
            setContent {
                val density = LocalDensity.current
                Box(
                    Modifier
                        .size(with(density) { width.toDp() }, with(density) { height.toDp() })
                        .background(Color.White),
                ) {
                    if (vertical) {
                        VerticalGroups(valueAxis, biConvex, startAnimation)
                    } else {
                        HorizontalGroups(valueAxis, biConvex, startAnimation)
                    }
                }
            }

            for (step in 1..GroupedAnimationSteps) {
                val progress = step.toFloat() / GroupedAnimationSteps
                startAnimation.animatables[0].snapTo(progress)
                waitForIdle()
                val pixels = onRoot().captureToImage().toPixelMap()
                val zeroEdge = (valueAxis.computeOffset(0f) * GroupedPlotLength * progress).roundToInt()
                val brokenLines = pixels.linesWithHoles(vertical)
                if (brokenLines.isNotEmpty()) failures.add("progress=$progress: holes in lines $brokenLines")
                if (pixels.colorBeyondZeroLine(vertical, zeroEdge)) {
                    failures.add("progress=$progress: bar pixels beyond the zero line")
                }
            }
        }

        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Composable
    private fun VerticalGroups(
        valueAxis: FloatLinearAxisModel,
        biConvex: Boolean,
        startAnimation: StartAnimationUseCase,
    ) {
        with(TestGraphScope(CategoryAxisModel(quarters), valueAxis)) {
            GroupedVerticalBarPlot(startAnimationUseCase = startAnimation) {
                groupedSeries.forEachIndexed { index, values ->
                    val color = seriesColors[index]
                    series(if (biConvex) verticalBiConvexBar(color) else verticalPlanoConvexBar(color)) {
                        values.forEachIndexed { i, value -> item(quarters[i], 0f, value) }
                    }
                }
            }
        }
    }

    @Composable
    private fun HorizontalGroups(
        valueAxis: FloatLinearAxisModel,
        biConvex: Boolean,
        startAnimation: StartAnimationUseCase,
    ) {
        with(TestGraphScope(valueAxis, CategoryAxisModel(quarters))) {
            GroupedHorizontalBarPlot(startAnimationUseCase = startAnimation) {
                groupedSeries.forEachIndexed { index, values ->
                    val color = seriesColors[index]
                    series(if (biConvex) horizontalBiConvexBar(color) else horizontalPlanoConvexBar(color)) {
                        values.forEachIndexed { i, value -> item(quarters[i], 0f, value) }
                    }
                }
            }
        }
    }
}

/**
 * Returns the pixel lines across the bars (columns for vertical bars, rows for horizontal bars) in which the bar
 * pixels are interrupted by background pixels. Grouped bars do not overlap, so every line crosses at most one bar.
 */
private fun PixelMap.linesWithHoles(vertical: Boolean): List<Int> = (0 until GroupedPlotThickness).filter { line ->
    val background = (0 until GroupedPlotLength).map { position ->
        if (vertical) isBackground(line, position) else isBackground(position, line)
    }
    val first = background.indexOfFirst { !it }
    val last = background.indexOfLast { !it }
    first >= 0 && (first..last).any { background[it] }
}

/**
 * Returns true if bar pixels lie beyond the zero line, which is [zeroEdge] pixels from the start of the axis.
 */
private fun PixelMap.colorBeyondZeroLine(
    vertical: Boolean,
    zeroEdge: Int,
): Boolean = (0 until GroupedPlotThickness).any { line ->
    (0 until zeroEdge).any { distance ->
        if (vertical) {
            !isBackground(line, GroupedPlotLength - 1 - distance)
        } else {
            !isBackground(distance, line)
        }
    }
}
