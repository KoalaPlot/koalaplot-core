package io.github.koalaplot.core.pie

import io.github.koalaplot.core.util.maximize
import org.junit.Test
import kotlin.test.assertEquals

class UtilTest {
    @Test
    fun testMaximize() {
        val x = maximize(0.0, 10.0, 0.01) {
            it < 5.0
        }
        assertEquals(5.0, x, 5.0 * 0.01, "Result $x was not close to 5.0")
    }

    @Test
    fun testMaximizeWithInfinity() {
        val x = maximize(0.0, Double.POSITIVE_INFINITY, 0.01) {
            it < 5.0
        }
        assertEquals(5.0, x, 5.0 * 0.01, "Result $x was not close to 5.0")
    }

    @Test
    fun testMaximizeInverted() {
        val x = -maximize(-10.0, 0.0, 0.01) {
            it < -5.0
        }
        assertEquals(5.0, x, 5.0 * 0.01, "Result $x was not close to 5.0")
    }
}
