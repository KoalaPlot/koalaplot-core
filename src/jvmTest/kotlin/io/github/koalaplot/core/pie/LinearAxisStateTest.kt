package io.github.koalaplot.core.pie

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.koalaplot.core.xychart.LinearAxisModel
import org.junit.Test
import kotlin.test.assertEquals

class LinearAxisStateTest {
    @Test
    fun testComputeMajorTickValues0to10() {
        testLinearAxis(0f..10f, 500.dp, 500.dp, listOf(0f, 10f))
        testLinearAxis(0f..10f, 50.dp, 500.dp, List(11) { it * 1f })
        testLinearAxis(0f..10f, 100.dp, 500.dp, List(6) { it * 2f })
    }

    @Test
    fun testComputeMajorTickValues0to10Negative() {
        testLinearAxis(-10f..0f, 500.dp, 500.dp, listOf(-10f, 0f))
        testLinearAxis(-10f..0f, 50.dp, 500.dp, List(11) { it * -1f }.reversed())
        testLinearAxis(-10f..0f, 100.dp, 500.dp, List(6) { it * -2f }.reversed())
    }

    @Test
    fun testComputeMajorTickValues01to1() {
        testLinearAxis(0f..1f, 500.dp, 500.dp, listOf(0f, 1f))
        testLinearAxis(0f..1f, 50.dp, 500.dp, List(11) { it * 0.1f })
        testLinearAxis(0f..1f, 100.dp, 500.dp, List(6) { it * 0.2f })
    }

    @Test
    fun testComputeMajorTickValues0to100() {
        testLinearAxis(0f..100f, 500.dp, 500.dp, listOf(0f, 100f))
        testLinearAxis(0f..100f, 50.dp, 500.dp, List(11) { it * 10f })
        testLinearAxis(0f..100f, 100.dp, 500.dp, List(6) { it * 20f })
    }

    @Test
    fun testComputeMajorTickValuesShifted() {
        testLinearAxis(10f..90f, 50.dp, 500.dp, List(9) { it * 10f + 10f })
        testLinearAxis(10f..90f, 100.dp, 500.dp, List(4) { it * 20f + 20f })
    }
}

private fun testLinearAxis(
    range: ClosedFloatingPointRange<Float>,
    minTickSpacing: Dp,
    axisLength: Dp,
    expected: List<Float>
) {
    val axis = LinearAxisModel(range, minimumMajorTickSpacing = minTickSpacing)
    val ticks = axis.computeTickValues(axisLength).majorTickValues

    // assertEquals on arrays of Floats does not factor in precision of the float
    // so need to implement a loop asserting each float with precision
    // e.g. +0f and -0f are not equal
    assertEquals(expected.size, ticks.size, "Number of ticks")

    expected.forEachIndexed { i, _ ->
        assertEquals(expected[i], ticks[i], kotlin.math.abs(expected[i] * 1e-3f), "Unexpected tick value")
    }
}
