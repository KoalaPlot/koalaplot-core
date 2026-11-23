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
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

private enum class BarStyle { Solid, PlanoConvex, BiConvex }

private const val PlotLength = 1000
private const val PlotThickness = 600
private const val AnimationSteps = 25

private val years = listOf(1980, 1990, 2000, 2010, 2020)

// Population per borough, one list per stack layer. The first and the last layer contain very short bars.
private val populations = listOf(
    listOf(168_972, 1_203_789, 1_332_650, 1_385_108, 1_446_788),
    listOf(2_230_936, 2_300_664, 2_465_326, 2_552_911, 2_648_452),
    listOf(1_428_285, 1_487_536, 1_537_195, 1_585_873, 1_638_281),
    listOf(1_891_325, 1_951_598, 2_229_379, 2_250_002, 2_330_295),
    listOf(152_121, 378_977, 443_728, 468_730, 487_155),
)

private val colors = listOf(
    Color(0xFFB30000),
    Color(0xFF8FB300),
    Color(0xFF00B347),
    Color(0xFF0047B3),
    Color(0xFF8F00B3),
)

private fun Int.toMillions(): Float = (this / 1E6).toFloat()

/**
 * Renders stacked bar plots at several steps of the start animation and checks that the background does not show
 * through between adjacent bars of a stack.
 */
@OptIn(ExperimentalTestApi::class)
class StackedBarPlotGapTest {
    @Test
    fun verticalSolidBarsHaveNoGaps() = assertNoGaps(vertical = true, BarStyle.Solid)

    @Test
    fun verticalPlanoConvexBarsHaveNoGaps() = assertNoGaps(vertical = true, BarStyle.PlanoConvex)

    @Test
    fun verticalBiConvexBarsHaveNoGaps() = assertNoGaps(vertical = true, BarStyle.BiConvex)

    @Test
    fun horizontalSolidBarsHaveNoGaps() = assertNoGaps(vertical = false, BarStyle.Solid)

    @Test
    fun horizontalPlanoConvexBarsHaveNoGaps() = assertNoGaps(vertical = false, BarStyle.PlanoConvex)

    @Test
    fun horizontalBiConvexBarsHaveNoGaps() = assertNoGaps(vertical = false, BarStyle.BiConvex)

    /**
     * A horizontal bi-convex bar whose stack ends within the arc radius of the zero line used to lose most of its
     * area because the edge of its cutout coincided with the edge of its cap. Size and values are taken from such a
     * bar, the second one of the first stack during the start animation.
     */
    @Test
    fun horizontalBiConvexShapeIsFilledCloseToZeroLine() {
        val width = 61
        val height = 162
        runDesktopComposeUiTest(width = width, height = height) {
            val scope = TestGraphScope(FloatLinearAxisModel(0f..10f), CategoryAxisModel(years))
            val start = populations[0][0].toMillions()
            val entry = DefaultHorizontalBarPlotEntry(years[0], DefaultBarPosition(start, start + populations[1][0].toMillions()))
            setContent {
                val density = LocalDensity.current
                Box(
                    Modifier
                        .size(with(density) { width.toDp() }, with(density) { height.toDp() })
                        .background(Color.White)
                        .background(Color.Black, HorizontalBiConvexShape(scope, 1, entry)),
                )
            }
            val pixels = onRoot().captureToImage().toPixelMap()
            assertFalse(pixels.isBackground(width / 2, height / 2), "Center of the bar is not filled")
        }
    }

    private fun assertNoGaps(
        vertical: Boolean,
        style: BarStyle,
    ) {
        val width = if (vertical) PlotThickness else PlotLength
        val height = if (vertical) PlotLength else PlotThickness
        val failures = mutableListOf<String>()

        runDesktopComposeUiTest(width = width, height = height, testTimeout = 5.minutes) {
            val startAnimation = StartAnimationUseCase(StartAnimationUseCase.ExecutionType.None, snap())
            setContent {
                val density = LocalDensity.current
                Box(
                    Modifier
                        .size(with(density) { width.toDp() }, with(density) { height.toDp() })
                        .background(Color.White),
                ) {
                    if (vertical) {
                        VerticalStack(style, startAnimation)
                    } else {
                        HorizontalStack(style, startAnimation)
                    }
                }
            }

            (1..AnimationSteps).map { it.toFloat() / AnimationSteps }.forEach { progress ->
                startAnimation.animatables[0].snapTo(progress)
                waitForIdle()
                val gaps = onRoot().captureToImage().toPixelMap().findGaps(vertical)
                if (gaps.isNotEmpty()) failures.add("progress=$progress: (bar, pixel)=$gaps")
            }
        }

        assertTrue(failures.isEmpty(), "Background visible between stacked bars:\n${failures.joinToString("\n")}")
    }

    @Composable
    private fun VerticalStack(
        style: BarStyle,
        startAnimation: StartAnimationUseCase,
    ) {
        with(TestGraphScope(CategoryAxisModel(years), FloatLinearAxisModel(0f..10f))) {
            StackedVerticalBarPlot(startAnimationUseCase = startAnimation) {
                populations.forEachIndexed { layer, values ->
                    val bar = when (style) {
                        BarStyle.Solid -> verticalSolidBar<Int, Float>(colors[layer])
                        BarStyle.PlanoConvex -> verticalPlanoConvexBar(colors[layer])
                        BarStyle.BiConvex -> verticalBiConvexBar(colors[layer])
                    }
                    series(bar) {
                        values.forEachIndexed { i, value -> item(years[i], value.toMillions()) }
                    }
                }
            }
        }
    }

    @Composable
    private fun HorizontalStack(
        style: BarStyle,
        startAnimation: StartAnimationUseCase,
    ) {
        with(TestGraphScope(FloatLinearAxisModel(0f..10f), CategoryAxisModel(years))) {
            StackedHorizontalBarPlot(startAnimationUseCase = startAnimation) {
                populations.forEachIndexed { layer, values ->
                    val bar = when (style) {
                        BarStyle.Solid -> horizontalSolidBar<Float, Int>(colors[layer])
                        BarStyle.PlanoConvex -> horizontalPlanoConvexBar(colors[layer])
                        BarStyle.BiConvex -> horizontalBiConvexBar(colors[layer])
                    }
                    series(bar) {
                        values.forEachIndexed { i, value -> item(value.toMillions(), years[i]) }
                    }
                }
            }
        }
    }
}

/**
 * Scans the center line of every stack and returns the background pixels lying between the first and the last
 * colored pixel of the stack, as pairs of the stack index and the pixel position along the stack.
 */
private fun PixelMap.findGaps(vertical: Boolean): List<Pair<Int, Int>> = years.indices.flatMap { index ->
    val center = ((index + 1f) / (years.size + 1f) * PlotThickness).toInt()
    val background = (0 until PlotLength).map { position ->
        if (vertical) isBackground(center, position) else isBackground(position, PlotThickness - 1 - center)
    }
    val first = background.indexOfFirst { !it }
    val last = background.indexOfLast { !it }
    if (first < 0) emptyList() else (first..last).filter { background[it] }.map { index to it }
}
