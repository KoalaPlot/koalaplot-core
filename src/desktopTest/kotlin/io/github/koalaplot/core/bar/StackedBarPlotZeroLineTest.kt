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

private const val ZeroLinePlotLength = 1000
private const val ZeroLinePlotThickness = 600
private const val ZeroLineAnimationSteps = 10

// The axis also covers negative values, so the zero line lies inside the plot area
private val valueAxisRange = -2f..10f

private val categories = listOf(0, 1, 2, 3, 4)

// Bar lengths per stack layer and category. The first layers are mostly shorter than the arc radius of the bars, so
// their shapes have to be cut off at the zero line.
private val layers = listOf(
    listOf(0.2f, 0.05f, 0.3f, 0.1f, 0.45f),
    listOf(0.04f, 0.03f, 0.02f, 0.1f, 0.01f),
    listOf(1f, 0.5f, 0.2f, 0.1f, 2f),
)

private val layerColors = listOf(Color(0xFFB30000), Color(0xFF00B347), Color(0xFF0047B3))

/**
 * Renders stacked bars close to a zero line lying inside the plot area and checks that the bar shapes are cut off
 * exactly at the zero line, which requires the exact pixel distance between each bar and the zero line.
 */
@OptIn(ExperimentalTestApi::class)
class StackedBarPlotZeroLineTest {
    @Test
    fun verticalPlanoConvexBarsEndAtZeroLine() = assertNothingBeyondZeroLine(vertical = true, biConvex = false)

    @Test
    fun verticalBiConvexBarsEndAtZeroLine() = assertNothingBeyondZeroLine(vertical = true, biConvex = true)

    @Test
    fun horizontalPlanoConvexBarsEndAtZeroLine() = assertNothingBeyondZeroLine(vertical = false, biConvex = false)

    @Test
    fun horizontalBiConvexBarsEndAtZeroLine() = assertNothingBeyondZeroLine(vertical = false, biConvex = true)

    private fun assertNothingBeyondZeroLine(
        vertical: Boolean,
        biConvex: Boolean,
    ) {
        val width = if (vertical) ZeroLinePlotThickness else ZeroLinePlotLength
        val height = if (vertical) ZeroLinePlotLength else ZeroLinePlotThickness
        val valueAxis = FloatLinearAxisModel(valueAxisRange)
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
                        VerticalStack(valueAxis, biConvex, startAnimation)
                    } else {
                        HorizontalStack(valueAxis, biConvex, startAnimation)
                    }
                }
            }

            (1..ZeroLineAnimationSteps).map { it.toFloat() / ZeroLineAnimationSteps }.forEach { progress ->
                startAnimation.animatables[0].snapTo(progress)
                waitForIdle()
                val pixels = onRoot().captureToImage().toPixelMap()

                // Position of the zero line in pixels from the start of the axis, as computed by the bar layout
                val zeroEdge = (valueAxis.computeOffset(0f) * ZeroLinePlotLength * progress).roundToInt()
                val beyond = pixels.coloredPixelsBeyondZeroLine(vertical, zeroEdge)
                if (beyond > 0) failures.add("progress=$progress: $beyond bar pixels beyond the zero line")
                assertTrue(pixels.hasColorNextToZeroLine(vertical, zeroEdge), "No bars drawn at progress=$progress")
            }
        }

        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Composable
    private fun VerticalStack(
        valueAxis: FloatLinearAxisModel,
        biConvex: Boolean,
        startAnimation: StartAnimationUseCase,
    ) {
        with(TestGraphScope(CategoryAxisModel(categories), valueAxis)) {
            StackedVerticalBarPlot(startAnimationUseCase = startAnimation) {
                layers.forEachIndexed { layer, values ->
                    val bar = if (biConvex) {
                        verticalBiConvexBar(layerColors[layer])
                    } else {
                        verticalPlanoConvexBar(layerColors[layer])
                    }
                    series(bar) {
                        values.forEachIndexed { i, value -> item(categories[i], value) }
                    }
                }
            }
        }
    }

    @Composable
    private fun HorizontalStack(
        valueAxis: FloatLinearAxisModel,
        biConvex: Boolean,
        startAnimation: StartAnimationUseCase,
    ) {
        with(TestGraphScope(valueAxis, CategoryAxisModel(categories))) {
            StackedHorizontalBarPlot(startAnimationUseCase = startAnimation) {
                layers.forEachIndexed { layer, values ->
                    val bar = if (biConvex) {
                        horizontalBiConvexBar(layerColors[layer])
                    } else {
                        horizontalPlanoConvexBar(layerColors[layer])
                    }
                    series(bar) {
                        values.forEachIndexed { i, value -> item(value, categories[i]) }
                    }
                }
            }
        }
    }
}

/**
 * Counts the non-background pixels beyond the zero line, which lies [zeroEdge] pixels from the start of the axis
 * (bottom for vertical bars, left for horizontal bars).
 */
private fun PixelMap.coloredPixelsBeyondZeroLine(
    vertical: Boolean,
    zeroEdge: Int,
): Int = if (vertical) {
    (ZeroLinePlotLength - zeroEdge until ZeroLinePlotLength).sumOf { y ->
        (0 until ZeroLinePlotThickness).count { x -> !isBackground(x, y) }
    }
} else {
    (0 until zeroEdge).sumOf { x ->
        (0 until ZeroLinePlotThickness).count { y -> !isBackground(x, y) }
    }
}

/**
 * Returns true if the pixel row (vertical bars) or column (horizontal bars) right in front of the zero line contains
 * bar pixels.
 */
private fun PixelMap.hasColorNextToZeroLine(
    vertical: Boolean,
    zeroEdge: Int,
): Boolean = (0 until ZeroLinePlotThickness).any { i ->
    if (vertical) !isBackground(i, ZeroLinePlotLength - zeroEdge - 1) else !isBackground(zeroEdge, i)
}
