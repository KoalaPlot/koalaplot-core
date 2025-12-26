package io.github.koalaplot.core.heatmap

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import io.github.koalaplot.core.util.lerp
import io.github.koalaplot.core.util.normalize

public typealias ColorScale<Z> = (Z) -> Color

/**
 * Creates a linear color scale that interpolates between colors.
 * @param domain Range of values to map
 * @param colors List of colors to interpolate between
 */
public fun <Z> linearColorScale(
    domain: ClosedRange<Z>,
    colors: List<Color>,
): ColorScale<Z> where Z : Comparable<Z>, Z : Number = { value ->
    val normalized = domain.normalize(value).toFloat().coerceIn(0f, 1f)

    if (colors.size == 1) {
        colors[0]
    } else {
        val segmentSize = 1f / (colors.size - 1)
        val segmentIndex = (normalized / segmentSize).toInt().coerceAtMost(colors.size - 2)
        val segmentProgress = (normalized - segmentIndex * segmentSize) / segmentSize

        lerp(colors[segmentIndex], colors[segmentIndex + 1], segmentProgress)
    }
}

/**
 * Creates a diverging color scale with a neutral midpoint.
 * @param domain Range of values to map
 * @param lowColor Color for low values
 * @param midColor Color for midpoint values
 * @param highColor Color for high values
 */
@Suppress("MagicNumber")
public fun <Z> divergingColorScale(
    domain: ClosedRange<Z>,
    lowColor: Color = Color.Blue,
    midColor: Color = Color.White,
    highColor: Color = Color.Red,
): ColorScale<Z> where Z : Comparable<Z>, Z : Number = { value ->
    val normalized = domain.normalize(value).toFloat().coerceIn(0f, 1f)

    if (normalized < 0.5f) {
        val progress = normalized * 2f
        lerp(lowColor, midColor, progress)
    } else {
        val progress = (normalized - 0.5f) * 2f
        lerp(midColor, highColor, progress)
    }
}

/**
 * Creates a discrete color scale that maps values to specific colors.
 * @param thresholds List of threshold values (ascending)
 * @param colors List of colors (same length as thresholds + 1)
 */
public fun <Z> discreteColorScale(
    thresholds: List<Z>,
    colors: List<Color>,
): ColorScale<Z> where Z : Comparable<Z> {
    require(colors.size == thresholds.size + 1) {
        "There should be one more color (now ${colors.size}) " +
            "than thresholds (${thresholds.size})"
    }
    return { value ->
        val index = thresholds.indexOfFirst { it > value }
        if (index < 0) {
            colors.last()
        } else {
            colors[index]
        }
    }
}

/**
 * Creates a discrete color scale with automatic binning.
 * @param domain Range of values to map
 * @param binCount Number of discrete bins
 * @param colors List of colors for each bin
 */
public fun <Z> discreteColorScale(
    domain: ClosedRange<Z>,
    colors: List<Color>,
): ColorScale<Z> where Z : Comparable<Z>, Z : Number {
    require(colors.size >= 1) { "Scale needs at least one color" }
    val binCount = colors.size

    val thresholds = (1 until binCount).map { i ->
        val normalized = (0..binCount).normalize(i)
        domain.lerp(normalized)
    }

    return discreteColorScale(
        thresholds,
        colors,
    )
}
