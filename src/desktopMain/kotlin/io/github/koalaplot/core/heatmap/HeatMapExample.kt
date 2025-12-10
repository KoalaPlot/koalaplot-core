package io.github.koalaplot.core.heatmap

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.koalaplot.core.heatmap.discreteColorScale
import io.github.koalaplot.core.heatmap.divergingColorScale
import io.github.koalaplot.core.heatmap.generateHistogram2D
import io.github.koalaplot.core.heatmap.linearColorScale
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.AxisContent
import io.github.koalaplot.core.xygraph.XYGraph
import io.github.koalaplot.core.xygraph.rememberAxisStyle
import io.github.koalaplot.core.xygraph.rememberFloatLinearAxisModel
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Configuration for a Gaussian cluster in 2D space
 */
public data class ClusterConfig(
    val centerX: Double,
    val centerY: Double,
    val standardDeviation: Double,
    val weight: Double,
)

/**
 * Generate 2D points following a Gaussian distribution around a center point
 * Uses Box-Muller transform for normally distributed coordinates
 */
private fun generateGaussianCluster(
    random: Random,
    numPoints: Int,
    config: ClusterConfig,
): List<Point2D> = List(numPoints) {
    val angle = random.nextDouble() * 2 * PI
    // Box-Muller transform for Gaussian distribution
    val u1 = random.nextDouble()
    val u2 = random.nextDouble()
    val radius = abs(sqrt(-2.0 * ln(u1)) * cos(2.0 * PI * u2) * config.standardDeviation)
    Point2D(
        config.centerX + radius * cos(angle),
        config.centerY + radius * sin(angle),
    )
}

/**
 * Generate 2D points with multiple distinct clusters for histogram demonstration
 */
public fun generateHistogramPoints(
    numPoints: Int,
    clusterConfigs: List<ClusterConfig> = defaultClusterConfigs(),
): List<Point2D> {
    require(numPoints > 0) { "Number of points must be positive" }

    val random = Random(42) // Fixed seed for reproducible results

    // Generate points for each cluster
    val clusterPoints = clusterConfigs.flatMap { config ->
        val pointsInCluster = (numPoints * config.weight).toInt()
        generateGaussianCluster(random, pointsInCluster, config)
    }

    return clusterPoints
}

/**
 * Default cluster configurations that create overlapping patterns
 */
public fun defaultClusterConfigs(): List<ClusterConfig> = listOf(
    ClusterConfig(centerX = 25.0, centerY = 25.0, standardDeviation = 1500.0, weight = 0.4),
    ClusterConfig(centerX = 75.0, centerY = 75.0, standardDeviation = 1200.0, weight = 0.3),
    ClusterConfig(centerX = 10.0, centerY = 80.0, standardDeviation = 1000.0, weight = 0.2),
)

/**
 * Extension function to find maximum value in a HeatMapGrid
 */
public fun HeatMapGrid<Int>.maxValue(): Int = this.flatten().maxOrNull() ?: 0

/**
 * Simple data class for 2D points
 */
public data class Point2D(
    val x: Double,
    val y: Double,
)

/**
 * Generate heatmap data using a continuous function with noise
 */
public fun generateHeatMapData(
    width: Int,
    height: Int,
): Array<Array<Int>> {
    val random = Random(42) // Fixed seed for reproducible results
    val data = Array(width) { Array<Int>(height) { 0 } }

    for (x in 0 until width) {
        for (y in 0 until height) {
            // Normalize coordinates to 0-1 range
            val nx = x.toFloat() / width
            val ny = y.toFloat() / height

            // Create interesting pattern using multiple sine waves
            val baseValue = (
                // Large wave pattern
                sin(nx * PI * 2) * cos(ny * PI * 2) * 30 +
                    // Smaller ripples
                    sin(nx * PI * 8) * sin(ny * PI * 8) * 15 +
                    // Diagonal gradient
                    (nx + ny) * 20 +
                    // Center hotspot
                    exp(-((nx - 0.5).pow(2) - (ny - 0.5).pow(2)) * 10) * 25
            )

            // Add random noise
            val noise = (random.nextFloat() - 0.5f) * 20

            // Combine and clamp to 0-100 range
            val finalValue = (baseValue + noise).toInt().coerceIn(0, 100)

            data[x][y] = finalValue
        }
    }

    return data
}

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
public fun HeatMapExample() {
    // Create larger sample data: 50x50 grid with continuous function + noise
    val gridSize = 100
    val bins = generateHeatMapData(gridSize, gridSize)
    val valueRange = 0..100

    @Composable
    fun Label(text: Float) {
        Text(
            text.toString(),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(2.dp),
        )
    }

    // Create 2x2 grid layout with 4 different heatmaps
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // Top row: Histogram + Linear scale
        Row(
            modifier = Modifier.weight(1f),
        ) {
            // 2D Histogram with modulated alpha
            val histogramPoints = generateHistogramPoints(300000)
            val histogramBins = generateHistogram2D(
                samples = histogramPoints,
                nBinsX = gridSize / 4,
                nBinsY = gridSize / 4,
                xDomain = 0.0..gridSize.toDouble(),
                yDomain = 0.0..gridSize.toDouble(),
                xGetter = { it.x },
                yGetter = { it.y },
            )
            val maxOccurrences = histogramBins.flatten().maxOrNull()?.takeIf { it > 0 } ?: 1
            val color = MaterialTheme.colorScheme.primary

            XYGraph(
                xAxisModel = rememberFloatLinearAxisModel(0f..gridSize.toFloat()),
                yAxisModel = rememberFloatLinearAxisModel(0f..gridSize.toFloat()),
                xAxisContent = AxisContent({ Label(it) }, {}, rememberAxisStyle()),
                yAxisContent = AxisContent({ Label(it) }, {}, rememberAxisStyle()),
                modifier = Modifier.weight(1f),
            ) {
                HeatMapPlot(
                    xDomain = 0f..gridSize.toFloat(),
                    yDomain = 0f..gridSize.toFloat(),
                    bins = histogramBins,
                    colorScale = { color },
                    alphaScale = { value: Int -> (value.toFloat() / maxOccurrences).coerceIn(0f, 1f) },
                )
            }

            Spacer(Modifier.width(8.dp))

            // Linear Color Scale
            XYGraph(
                xAxisModel = rememberFloatLinearAxisModel(0f..gridSize.toFloat()),
                yAxisModel = rememberFloatLinearAxisModel(0f..gridSize.toFloat()),
                xAxisContent = AxisContent({ Label(it) }, {}, rememberAxisStyle()),
                yAxisContent = AxisContent({ Label(it) }, {}, rememberAxisStyle()),
                modifier = Modifier.weight(1f),
            ) {
                HeatMapPlot(
                    xDomain = 0f..gridSize.toFloat(),
                    yDomain = 0f..gridSize.toFloat(),
                    bins = bins,
                    colorScale = linearColorScale(
                        domain = 0..100,
                        colors = listOf(
                            Color(0xFF000033), // Dark blue
                            Color(0xFF000066), // Blue
                            Color(0xFF0000CC), // Bright blue
                            Color(0xFF0066FF), // Light blue
                            Color(0xFF00CCFF), // Cyan
                            Color(0xFF00FF66), // Green-cyan
                            Color(0xFF66FF00), // Yellow-green
                            Color(0xFFCCFF00), // Yellow
                            Color(0xFFFFCC00), // Orange-yellow
                            Color(0xFFFF6600), // Orange
                        ),
                    ),
                    alphaScale = { value: Int -> 1f },
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Bottom row: Diverging + Discrete
        Row(
            modifier = Modifier.weight(1f),
        ) {
            // Diverging Color Scale
            XYGraph(
                xAxisModel = rememberFloatLinearAxisModel(0f..gridSize.toFloat()),
                yAxisModel = rememberFloatLinearAxisModel(0f..gridSize.toFloat()),
                xAxisContent = AxisContent({ Label(it) }, {}, rememberAxisStyle()),
                yAxisContent = AxisContent({ Label(it) }, {}, rememberAxisStyle()),
                modifier = Modifier.weight(1f),
            ) {
                HeatMapPlot(
                    xDomain = 0f..gridSize.toFloat(),
                    yDomain = 0f..gridSize.toFloat(),
                    bins = bins,
                    colorScale = divergingColorScale(
                        domain = 0..100,
                        lowColor = Color.Blue,
                        midColor = Color.White,
                        highColor = Color.Red,
                    ),
                    alphaScale = { value: Int -> 1f },
                )
            }

            Spacer(Modifier.width(8.dp))

            // Discrete Color Scale
            XYGraph(
                xAxisModel = rememberFloatLinearAxisModel(0f..gridSize.toFloat()),
                yAxisModel = rememberFloatLinearAxisModel(0f..gridSize.toFloat()),
                xAxisContent = AxisContent({ Label(it) }, {}, rememberAxisStyle()),
                yAxisContent = AxisContent({ Label(it) }, {}, rememberAxisStyle()),
                modifier = Modifier.weight(1f),
            ) {
                HeatMapPlot(
                    xDomain = 0f..gridSize.toFloat(),
                    yDomain = 0f..gridSize.toFloat(),
                    bins = bins,
                    colorScale = discreteColorScale(
                        domain = 0..100,
                        colors = listOf(
                            Color(0xFF000033), // Dark blue
                            Color(0xFF0066FF), // Light blue
                            Color(0xFF00FF66), // Green-cyan
                            Color(0xFFFFCC00), // Yellow
                            Color(0xFFFF6600), // Orange
                        ),
                    ),
                    alphaScale = { value: Int -> 1f },
                )
            }
        }
    }
}

public fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "HeatMap Example",
        ) {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    HeatMapExample()
                }
            }
        }
    }
}
