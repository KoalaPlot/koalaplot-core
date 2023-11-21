package io.github.koalaplot.core.xygraph

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Test
import kotlin.math.abs
import kotlin.test.assertEquals

class LinearAxisStateTest {
    @Test
    fun testComputeMajorTickValues0to10() {
        testLinearAxisMajorTicks(
            0f..10f,
            500.dp,
            500.dp,
            listOf(
                0f,
                10f
            )
        )
        testLinearAxisMajorTicks(
            0f..10f,
            50.dp,
            500.dp,
            List(11) { it * 1f }
        )
        testLinearAxisMajorTicks(
            0f..10f,
            100.dp,
            500.dp,
            List(6) { it * 2f }
        )
    }

    @Test
    fun testComputeMajorTickValues0to10Negative() {
        testLinearAxisMajorTicks(
            -10f..0f,
            500.dp,
            500.dp,
            listOf(
                -10f,
                0f
            )
        )
        testLinearAxisMajorTicks(
            -10f..0f,
            50.dp,
            500.dp,
            List(11) { it * -1f }.reversed()
        )
        testLinearAxisMajorTicks(
            -10f..0f,
            100.dp,
            500.dp,
            List(6) { it * -2f }.reversed()
        )
    }

    @Test
    fun testComputeMajorTickValues01to1() {
        testLinearAxisMajorTicks(
            0f..1f,
            500.dp,
            500.dp,
            listOf(
                0f,
                1f
            )
        )
        testLinearAxisMajorTicks(
            0f..1f,
            50.dp,
            500.dp,
            List(11) { it * 0.1f }
        )
        testLinearAxisMajorTicks(
            0f..1f,
            100.dp,
            500.dp,
            List(6) { it * 0.2f }
        )
    }

    @Test
    fun testComputeMajorTickValues0to100() {
        testLinearAxisMajorTicks(
            0f..100f,
            500.dp,
            500.dp,
            listOf(
                0f,
                100f
            )
        )
        testLinearAxisMajorTicks(
            0f..100f,
            50.dp,
            500.dp,
            List(11) { it * 10f }
        )
        testLinearAxisMajorTicks(
            0f..100f,
            100.dp,
            500.dp,
            List(6) { it * 20f }
        )
    }

    @Test
    fun testComputeMajorTickValuesShifted() {
        testLinearAxisMajorTicks(
            10f..90f,
            50.dp,
            500.dp,
            List(9) { it * 10f + 10f }
        )
        testLinearAxisMajorTicks(
            10f..90f,
            100.dp,
            500.dp,
            List(4) { it * 20f + 20f }
        )
    }

    @Test
    fun testComputeMinorTickValues() {
        testLinearAxisMinorTicks(
            0f..20f,
            1000.dp,
            expected = listOf(
                0.4f, 0.8f, 1.2f, 1.6f,
                2.4f, 2.8f, 3.2f, 3.6f,
                4.4f, 4.8f, 5.2f, 5.6f,
                6.4f, 6.8f, 7.2f, 7.6f,
                8.4f, 8.8f, 9.2f, 9.6f,
                10.4f, 10.8f, 11.2f, 11.6f,
                12.4f, 12.8f, 13.2f, 13.6f,
                14.4f, 14.8f, 15.2f, 15.6f,
                16.4f, 16.8f, 17.2f, 17.6f,
                18.4f, 18.8f, 19.2f, 19.6f
            )
        )
    }
}

private fun testLinearAxisMajorTicks(
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
        assertEquals(expected[i], ticks[i], abs(expected[i] * 1e-3f), "Unexpected tick value")
    }
}

private fun testLinearAxisMinorTicks(
    range: ClosedFloatingPointRange<Float>,
    axisLength: Dp,
    minTickSpacing: Dp = 50.dp,
    expected: List<Float>,
) {
    val axis = LinearAxisModel(range, minimumMajorTickSpacing = minTickSpacing)
    val ticks = axis.computeTickValues(axisLength).minorTickValues

    // assertEquals on arrays of Floats does not factor in precision of the float
    // so need to implement a loop asserting each float with precision
    // e.g. +0f and -0f are not equal
    assertEquals(expected.size, ticks.size, "Number of ticks")

    expected.forEachIndexed { i, _ ->
        assertEquals(expected[i], ticks[i], abs(expected[i] * 1e-3f), "Unexpected tick value")
    }
}
