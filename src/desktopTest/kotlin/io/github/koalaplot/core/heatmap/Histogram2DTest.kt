@file:Suppress("MagicNumber")

package io.github.koalaplot.core.heatmap

import org.junit.Test
import kotlin.test.assertEquals

/** Test helper class for 2D points */
data class TestPoint2D(
    val x: Double,
    val y: Double,
)

/** Lazy version of assertEquals */
inline fun <T> assertEquals(
    expected: T,
    actual: T,
    lazyMessage: () -> String,
) {
    kotlin.test.assertEquals(expected, actual, lazyMessage())
}

/**
 * Creates and asserts histogram results with sensible defaults.
 * Provides comprehensive error messages showing complete expected vs actual histogram.
 *
 * @param samples List of test points
 * @param nBinsX Number of bins in X direction (default: 3)
 * @param nBinsY Number of bins in Y direction (default: 3)
 * @param xDomain X coordinate range (default: 0.0..3.0)
 * @param yDomain Y coordinate range (default: 0.0..3.0)
 * @param expected Expected 2D histogram array
 * @param message Optional assertion message describing the test scenario
 */
private fun assertHistogram2D(
    samples: List<Pair<Double, Double>>,
    nBinsX: Int = 2,
    nBinsY: Int = 3,
    xDomain: ClosedRange<Double> = 0.0..2.0,
    yDomain: ClosedRange<Double> = 0.0..3.0,
    expected: Array<Array<Int>>,
    message: String? = null,
) {
    val result = generateHistogram2D(
        samples = samples,
        nBinsX = nBinsX,
        nBinsY = nBinsY,
        xDomain = xDomain,
        yDomain = yDomain,
        xGetter = { (x, y) -> x },
        yGetter = { (x, y) -> y },
    )

    fun snapshot(message: String): String = buildString {
        if (message != null) {
            append("$message\n")
            append("\n")
        }
        append("Expected histogram:\n")
        append(formatHistogram(expected))
        append("\nActual histogram:\n")
        append(formatHistogram(result))
        append("\n")
        append("Input samples: ${samples.map { (x,y) -> "($x, $y)" }}\n")
        append("Domain: X=$xDomain, Y=$yDomain, Bins: ${nBinsX}x${nBinsY}\n")
    }

    assertEquals(expected.size, result.size) {
        snapshot("${message ?: ""}X bins count mismatch. Expected: ${expected.size}, Actual: ${result.size}")
    }
    assertEquals(expected[0].size, result[0].size) {
        snapshot("${message ?: ""}Y bins count mismatch. Expected: ${expected[0].size}, Actual: ${result[0].size}")
    }

    // Compare all values with detailed error message showing complete histograms
    for (y in 0 until nBinsY) {
        for (x in 0 until nBinsX) {
            val expectedValue = expected[x][y]
            val actualValue = result[x][y]
            assertEquals(expectedValue, actualValue) {
                snapshot("Histogram mismatch at bin [$x,$y]: expected $expectedValue, got $actualValue")
            }
        }
    }
}

/**
 * Formats histogram as readable grid for error messages.
 * Displays the histogram as rows (Y) x columns (X) to match test data structure.
 */
private fun formatHistogram(histogram: Array<Array<Int>>): String = buildString {
    // Display as rows (Y dimension) with columns (X dimension)
    for (x in histogram.indices) {
        for (y in histogram[x].indices) {
            append("${histogram[x][y].toString().padStart(2)} ")
        }
        append("\n")
    }
}

class Histogram2DTest {
    @Test
    fun `Histogram single sample at origin`() {
        assertHistogram2D(
            samples = listOf(0.0 to 0.0),
            expected = arrayOf(
                arrayOf(1, 0, 0),
                arrayOf(0, 0, 0),
            ),
        )
    }

    @Test
    fun `Histogram single sample in middle of 0 0 bin`() {
        assertHistogram2D(
            samples = listOf(0.5 to 0.5),
            expected = arrayOf(
                arrayOf(1, 0, 0),
                arrayOf(0, 0, 0),
            ),
        )
    }

    @Test
    fun `Histogram single sample on next x bin`() {
        assertHistogram2D(
            samples = listOf(1.0 to 0.5),
            expected = arrayOf(
                arrayOf(0, 0, 0),
                arrayOf(1, 0, 0),
            ),
        )
    }

    @Test
    fun `Histogram single sample just before next x bin`() {
        assertHistogram2D(
            samples = listOf(0.99 to 0.5),
            expected = arrayOf(
                arrayOf(1, 0, 0),
                arrayOf(0, 0, 0),
            ),
        )
    }

    @Test
    fun `Histogram single sample beyond max x`() {
        assertHistogram2D(
            samples = listOf(2.0 to 0.5),
            expected = arrayOf(
                arrayOf(0, 0, 0),
                arrayOf(0, 0, 0),
            ),
        )
    }

    @Test
    fun `Histogram single sample beyond min x`() {
        assertHistogram2D(
            samples = listOf(-0.1 to 0.5),
            expected = arrayOf(
                arrayOf(0, 0, 0),
                arrayOf(0, 0, 0),
            ),
        )
    }

    @Test
    fun `Histogram single sample on next y bin`() {
        assertHistogram2D(
            samples = listOf(0.5 to 1.0),
            expected = arrayOf(
                arrayOf(0, 1, 0),
                arrayOf(0, 0, 0),
            ),
        )
    }

    @Test
    fun `Histogram single sample just before next y bin`() {
        assertHistogram2D(
            samples = listOf(0.5 to 0.99),
            expected = arrayOf(
                arrayOf(1, 0, 0),
                arrayOf(0, 0, 0),
            ),
        )
    }

    @Test
    fun `Histogram single sample beyond max y`() {
        assertHistogram2D(
            samples = listOf(0.5 to 3.0),
            expected = arrayOf(
                arrayOf(0, 0, 0),
                arrayOf(0, 0, 0),
            ),
        )
    }

    @Test
    fun `Histogram single sample beyond min y`() {
        assertHistogram2D(
            samples = listOf(0.5 to -0.1),
            expected = arrayOf(
                arrayOf(0, 0, 0),
                arrayOf(0, 0, 0),
            ),
        )
    }

    @Test
    fun `Histogram single sample scaled x domain under threshold`() {
        assertHistogram2D(
            xDomain = 0.0..20.0, // x10
            samples = listOf(9.9 to 0.5),
            expected = arrayOf(
                arrayOf(1, 0, 0),
                arrayOf(0, 0, 0),
            ),
        )
    }

    @Test
    fun `Histogram single sample scaled x domain over threshold`() {
        assertHistogram2D(
            xDomain = 0.0..20.0, // x10
            samples = listOf(10.0 to 0.5),
            expected = arrayOf(
                arrayOf(0, 0, 0),
                arrayOf(1, 0, 0),
            ),
        )
    }

    @Test
    fun `Histogram single sample scaled y domain under threshold`() {
        assertHistogram2D(
            yDomain = 0.0..30.0, // x10
            samples = listOf(0.5 to 9.9),
            expected = arrayOf(
                arrayOf(1, 0, 0),
                arrayOf(0, 0, 0),
            ),
        )
    }

    @Test
    fun `Histogram single sample scaled y domain over threshold`() {
        assertHistogram2D(
            yDomain = 0.0..30.0, // x10
            samples = listOf(0.5 to 10.0),
            expected = arrayOf(
                arrayOf(0, 1, 0),
                arrayOf(0, 0, 0),
            ),
        )
    }

    @Test
    fun `Histogram single sample offsetted x domain under threshold`() {
        assertHistogram2D(
            xDomain = 10.0..12.0, // +10
            samples = listOf(10.9 to 0.5),
            expected = arrayOf(
                arrayOf(1, 0, 0),
                arrayOf(0, 0, 0),
            ),
        )
    }

    @Test
    fun `Histogram single sample offsetted x domain over threshold`() {
        assertHistogram2D(
            xDomain = 10.0..12.0, // +10
            samples = listOf(11.0 to 0.5),
            expected = arrayOf(
                arrayOf(0, 0, 0),
                arrayOf(1, 0, 0),
            ),
        )
    }

    @Test
    fun `Histogram single sample offsetted y domain under threshold`() {
        assertHistogram2D(
            yDomain = 10.0..13.0, // +10
            samples = listOf(0.5 to 10.9),
            expected = arrayOf(
                arrayOf(1, 0, 0),
                arrayOf(0, 0, 0),
            ),
        )
    }

    @Test
    fun `Histogram single sample offsetted y domain over threshold`() {
        assertHistogram2D(
            yDomain = 10.0..13.0, // +10
            samples = listOf(0.5 to 11.0),
            expected = arrayOf(
                arrayOf(0, 1, 0),
                arrayOf(0, 0, 0),
            ),
        )
    }

    @Test
    fun `Histogram multiple samples in different bins get counted`() {
        assertHistogram2D(
            samples = listOf(
                0.5 to 1.5,
                1.5 to 2.5,
            ),
            expected = arrayOf(
                arrayOf(0, 1, 0),
                arrayOf(0, 0, 1),
            ),
        )
    }

    @Test
    fun `Histogram multiple samples in same bin get added`() {
        assertHistogram2D(
            samples = listOf(
                0.5 to 1.5,
                0.7 to 1.2,
            ),
            expected = arrayOf(
                arrayOf(0, 2, 0),
                arrayOf(0, 0, 0),
            ),
        )
    }

    @Test
    fun `Histogram with inverted x domains`() {
        assertHistogram2D(
            xDomain = 2.0..0.0,
            samples = listOf(
                1.5 to 0.5,
                0.5 to 2.5,
            ),
            expected = arrayOf(
                arrayOf(1, 0, 0),
                arrayOf(0, 0, 1),
            ),
        )
    }

    @Test
    fun `Histogram with inverted y domains`() {
        assertHistogram2D(
            yDomain = 3.0..0.0,
            samples = listOf(
                1.5 to 0.5,
                0.5 to 2.5,
            ),
            expected = arrayOf(
                arrayOf(1, 0, 0),
                arrayOf(0, 0, 1),
            ),
        )
    }
}
