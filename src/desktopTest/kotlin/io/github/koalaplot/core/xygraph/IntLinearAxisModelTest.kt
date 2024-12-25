@file:Suppress("MagicNumber")

package io.github.koalaplot.core.xygraph

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Test
import kotlin.test.assertEquals

class IntLinearAxisModelTest {
    @Test
    fun testComputeMajorTickValues0to10() {
        testLinearAxisMajorTicks(
            0..10,
            500.dp,
            500.dp,
            listOf(
                0,
                10
            )
        )
        testLinearAxisMajorTicks(
            0..10,
            50.dp,
            500.dp,
            List(11) { it }
        )
        testLinearAxisMajorTicks(
            0..10,
            100.dp,
            500.dp,
            List(6) { it * 2 }
        )
    }

    @Test
    fun testComputeMajorTickValues0to10Negative() {
        testLinearAxisMajorTicks(
            -10..0,
            500.dp,
            500.dp,
            listOf(-10, 0)
        )
        testLinearAxisMajorTicks(
            -10..0,
            50.dp,
            500.dp,
            List(11) { it * -1 }.reversed()
        )
        testLinearAxisMajorTicks(
            -10..0,
            100.dp,
            500.dp,
            List(6) { it * -2 }.reversed()
        )
    }

    @Test
    fun testComputeMajorTickValues0to100() {
        testLinearAxisMajorTicks(
            0..100,
            500.dp,
            500.dp,
            listOf(0, 100)
        )
        testLinearAxisMajorTicks(
            0..100,
            50.dp,
            500.dp,
            List(11) { it * 10 }
        )
        testLinearAxisMajorTicks(
            0..100,
            100.dp,
            500.dp,
            List(6) { it * 20 }
        )
    }

    @Test
    fun testComputeMajorTickValuesShifted() {
        testLinearAxisMajorTicks(
            10..90,
            50.dp,
            500.dp,
            List(9) { it * 10 + 10 }
        )
        testLinearAxisMajorTicks(
            10..90,
            100.dp,
            500.dp,
            List(4) { it * 20 + 20 }
        )
    }

    @Test
    fun testSetRangeWithinAllowedRangeAndExtents() {
        val axis = IntLinearAxisModel(0..100, minViewExtent = 10, maxViewExtent = 50)
        axis.setViewRange(0.0..20.0)
        assertEquals(0.0, axis.currentRange.value.start)
        assertEquals(20.0, axis.currentRange.value.endInclusive)
    }

    @Test
    fun testSetRangeWithTooSmallExtent() {
        val axis = IntLinearAxisModel(0..100, minViewExtent = 10, maxViewExtent = 50)
        axis.setViewRange(1.0..5.0)
        assertEquals(0.0, axis.currentRange.value.start)
        assertEquals(10.0, axis.currentRange.value.endInclusive)
    }

    @Test
    fun testSetRangeWithTooLargeExtent() {
        val axis = IntLinearAxisModel(0..100, minViewExtent = 10, maxViewExtent = 50)
        axis.setViewRange(0.0..60.0)
        assertEquals(5.0, axis.currentRange.value.start)
        assertEquals(55.0, axis.currentRange.value.endInclusive)
    }
}

private fun testLinearAxisMajorTicks(
    range: IntRange,
    minTickSpacing: Dp,
    axisLength: Dp,
    expected: List<Int>
) {
    val axis = IntLinearAxisModel(range, minimumMajorTickSpacing = minTickSpacing)
    val ticks = axis.computeTickValues(axisLength).majorTickValues

    // assertEquals on arrays of Floats does not factor in precision of the float
    // so need to implement a loop asserting each float with precision
    // e.g. +0f and -0f are not equal
    assertEquals(expected.size, ticks.size, "Number of ticks")

    expected.forEachIndexed { i, _ ->
        assertEquals(expected[i], ticks[i], "Unexpected tick value")
    }
}
