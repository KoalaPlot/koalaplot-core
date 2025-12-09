@file:Suppress("MagicNumber")

package io.github.koalaplot.core.heatmap

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Custom assertion helper for exact color comparisons using ARGB hex conversion
 */
private fun assertColorEquals(
    expected: Color,
    actual: Color,
    message: String? = null,
) {
    val expectedHex = String.format("0x%08X", expected.toArgb())
    val actualHex = String.format("0x%08X", actual.toArgb())

    if (message != null) {
        assertEquals(expectedHex, actualHex, message)
    } else {
        assertEquals(expectedHex, actualHex)
    }
}

private fun <Z> assertScaleValues(
    scale: ColorScale<Z>,
    expected: List<Pair<Z, Color>>,
    message: String? = null,
) {
    fun formatColor(c: Color) = String.format("0x%08X", c.toArgb())

    val formatedExpected = expected.map { (value, color) ->
        value to formatColor(color)
    }
    val result = expected.map { (value, color) ->
        value to formatColor(scale(value))
    }
    if (message != null) {
        assertEquals(formatedExpected.toString(), result.toString(), message)
    } else {
        assertEquals(formatedExpected.toString(), result.toString())
    }
}

val todoColor = Color(0x0)

class ColorScalesTest {
    @Test
    fun testLinearColorScaleWithFloat() {
        val colors = listOf(Color.Red, Color.Blue, Color.Green)
        val scale = linearColorScale(0f..10f, colors)

        // Test start of domain
        assertColorEquals(Color.Red, scale(0f), "Start of domain should return Red")

        // Test end of domain
        assertColorEquals(Color.Green, scale(10f), "End of domain should return Green")

        // Test middle point (should be exactly Blue)
        assertColorEquals(Color.Blue, scale(5f))

        // Test basic interpolation - check that it's not the same as start or end
        val quarterColor = scale(2.5f)
        assertColorEquals(Color(0xFF8C53A2), quarterColor, "Quarter color should be 25% between Red and Blue")

        // Test interpolation between Blue and Green
        val threeQuarterColor = scale(7.5f)
        assertColorEquals(Color(0xFF00AABF), threeQuarterColor, "Three quarter color should be 50% between Blue and Green")
    }

    @Test
    fun testLinearColorScaleWithInt() {
        val colors = listOf(Color.Red, Color.Green)
        val scale = linearColorScale(0..100, colors)

        assertColorEquals(Color.Red, scale(0))
        assertColorEquals(Color.Green, scale(100))

        // Test middle value
        val midColor = scale(50)
        assertColorEquals(Color(0xFFD0A800), midColor, "Middle color should be 50% between Yellow and Cyan")
    }

    @Test
    fun testLinearColorScaleWithDouble() {
        val colors = listOf(Color.Yellow, Color.Cyan)
        val scale = linearColorScale(0.0..1.0, colors)

        assertColorEquals(Color.Yellow, scale(0.0))
        assertColorEquals(Color.Cyan, scale(1.0))

        // Test middle value
        val midColor = scale(0.5)
        assertColorEquals(Color(0xFFB0FFB0), midColor, "Middle color should be 50% between Yellow and Cyan")
    }

    @Test
    fun testLinearColorScaleWithSingleColor() {
        val colors = listOf(Color.Magenta)
        val scale = linearColorScale(0f..100f, colors)

        // Should always return single color regardless of input
        assertColorEquals(Color.Magenta, scale(0f))
        assertColorEquals(Color.Magenta, scale(50f))
        assertColorEquals(Color.Magenta, scale(100f))
    }

    @Test
    fun testLinearColorScaleOutOfBounds() {
        val colors = listOf(Color.Red, Color.Blue)
        val scale = linearColorScale(10f..20f, colors)

        // Values below domain should be clamped to first color
        assertColorEquals(Color.Red, scale(0f))
        assertColorEquals(Color.Red, scale(5f))

        // Values above domain should be clamped to last color
        assertColorEquals(Color.Blue, scale(25f))
        assertColorEquals(Color.Blue, scale(100f))
    }

    @Test
    fun testLinearColorScaleWithNegativeDomain() {
        val colors = listOf(Color.Blue, Color.Red)
        val scale = linearColorScale(-10f..10f, colors)

        assertColorEquals(Color.Blue, scale(-10f))
        assertColorEquals(Color.Red, scale(10f))
        assertColorEquals(Color.Blue, scale(-20f)) // Below domain
        assertColorEquals(Color.Red, scale(20f)) // Above domain

        assertColorEquals(Color(0xFF8C53A2), scale(0f))
    }

    @Test
    fun testDivergingColorScaleDefaultColors() {
        val scale = divergingColorScale(-1f..1f)

        assertColorEquals(Color.Blue, scale(-1f))
        assertColorEquals(Color.White, scale(0f))
        assertColorEquals(Color.Red, scale(1f))

        // Test interpolation in negative/blue range
        val negMidColor = scale(-0.5f)
        assertColorEquals(Color(0xFF74A3FF), negMidColor, "Should be a White to Blue interpolation")

        // Test interpolation in positive/red range
        val posMidColor = scale(0.5f)
        assertColorEquals(Color(0xFFFFA191), posMidColor, "Should be a White to Red interpolation")
    }

    @Test
    fun testDivergingColorScaleCustomColors() {
        val scale = divergingColorScale(
            domain = 0f..100f,
            lowColor = Color.Green,
            midColor = Color.Yellow,
            highColor = Color.Red,
        )

        assertColorEquals(Color.Green, scale(0f))
        assertColorEquals(Color.Yellow, scale(50f))
        assertColorEquals(Color.Red, scale(100f))
    }

    @Test
    fun testDivergingColorScaleOutOfBounds() {
        val scale = divergingColorScale(0f..10f)

        // Below domain should be clamped to low color
        assertColorEquals(Color.Blue, scale(-5f))

        // Above domain should be clamped to high color
        assertColorEquals(Color.Red, scale(15f))
    }

    @Test
    fun testDiscreteColorScaleWithThresholds() {
        val thresholds = listOf(10, 20, 30)
        val colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Blue)
        val scale = discreteColorScale(thresholds, colors)

        assertScaleValues(
            scale,
            listOf(
                -1 to Color.Red,
                9 to Color.Red,
                10 to Color.Yellow,
                11 to Color.Yellow,
                19 to Color.Yellow,
                20 to Color.Green,
                21 to Color.Green,
                29 to Color.Green,
                30 to Color.Blue,
                31 to Color.Blue,
                100 to Color.Blue,
            ),
        )
    }

    @Test
    fun testDiscreteColorScaleWithFloatThresholds() {
        val thresholds = listOf(0.25f, 0.5f, 0.75f)
        val colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Blue)
        val scale = discreteColorScale(thresholds, colors)

        assertScaleValues(
            scale,
            listOf(
                0.24f to Color.Red,
                0.25f to Color.Yellow,
                0.26f to Color.Yellow,
                0.40f to Color.Yellow,
                0.50f to Color.Green,
                0.74f to Color.Green,
                0.75f to Color.Blue,
                0.76f to Color.Blue,
            ),
        )
    }

    @Test
    fun testDiscreteColorScaleWithAutomaticBinning() {
        val colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Blue)
        val scale = discreteColorScale(
            domain = 0f..100f,
            colors = colors,
        )

        assertScaleValues(
            scale,
            listOf(
                -1f to Color.Red,
                24f to Color.Red,
                25f to Color.Yellow,
                26f to Color.Yellow,
                49f to Color.Yellow,
                50f to Color.Green,
                51f to Color.Green,
                74f to Color.Green,
                75f to Color.Blue,
                76f to Color.Blue,
                100f to Color.Blue,
            ),
        )
    }

    @Test
    fun testDiscreteColorScaleWithAutomaticBinningInt() {
        val colors = listOf(Color.Red, Color.Blue, Color.Green)
        val scale = discreteColorScale(
            domain = 1..10,
            colors = colors,
        )

        assertScaleValues(
            scale,
            listOf(
                0 to Color.Red,
                2 to Color.Red,
                3 to Color.Red,
                4 to Color.Blue,
                6 to Color.Blue,
                7 to Color.Green,
                10 to Color.Green,
            ),
        )
    }

    @Test
    fun testDiscreteColorScaleInsufficientColors() {
        val exception = assertFailsWith<IllegalArgumentException> {
            discreteColorScale(
                domain = 0f..100f,
                colors = emptyList<Color>(),
            )
        }
        assertEquals("Scale needs at least one color", exception.message)
    }

    @Test
    fun testDiscreteColorScaleMissmatchedThresholds() {
        val exception = assertFailsWith<IllegalArgumentException> {
            discreteColorScale(
                // 5 thresholds
                thresholds = listOf(1, 2, 3, 4, 5),
                // 3 colors (should be 4)
                colors = listOf(Color.Red, Color.Green, Color.Blue),
            )
        }
        assertEquals("There should be one more color (now 3) than thresholds (5)", exception.message)
    }

    @Test
    fun testColorScaleWithCustomTransform() {
        val inverseScale = linearColorScale(
            1..100,
            listOf(Color.Red, Color.Blue),
        )

        assertScaleValues(
            inverseScale,
            listOf(
                1 to Color.Red,
                100 to Color.Blue,
            ),
        )
    }
}
