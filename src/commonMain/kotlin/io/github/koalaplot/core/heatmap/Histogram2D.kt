package io.github.koalaplot.core.heatmap

import kotlin.math.floor

/**
 * Generates a 2D histogram from a list of samples.
 *
 * Creates a 2D grid where each cell contains the count of samples that fall within that cell's boundaries.
 * Samples outside the specified domains are ignored (clipped).
 *
 * @param T The type of data points being processed
 * @param X The numeric type for x-coordinates
 * @param Y The numeric type for y-coordinates
 * @param samples List of data points to histogram
 * @param nBinsX Number of bins along x-axis
 * @param nBinsY Number of bins along y-axis
 * @param xDomain Range of x-values to include in histogram
 * @param yDomain Range of y-values to include in histogram
 * @param xGetter Function to extract x-coordinate from a sample
 * @param yGetter Function to extract y-coordinate from a sample
 * @return HeatMapGrid containing the histogram counts
 */
public fun <T, X, Y> generateHistogram2D(
    samples: List<T>,
    nBinsX: Int,
    nBinsY: Int,
    xDomain: ClosedRange<X>,
    yDomain: ClosedRange<Y>,
    xGetter: (T) -> X,
    yGetter: (T) -> Y,
): HeatMapGrid<Int> where X : Comparable<X>, X : Number, Y : Comparable<Y>, Y : Number {
    require(nBinsX > 0 && nBinsY > 0) { "Number of bins must be positive." }

    val xRange = xDomain.endInclusive.toFloat() - xDomain.start.toFloat()
    val yRange = yDomain.endInclusive.toFloat() - yDomain.start.toFloat()

    val bins = Array(nBinsX) { Array<Int>(nBinsY) { 0 } }
    for (sample in samples) {
        val x = xGetter(sample).toFloat()
        val y = yGetter(sample).toFloat()

        val ix = floor(nBinsX * (x - xDomain.start.toFloat()) / xRange).toInt()
        val iy = floor(nBinsY * (y - yDomain.start.toFloat()) / yRange).toInt()

        if (ix !in 0 until nBinsX) continue
        if (iy !in 0 until nBinsY) continue

        bins[ix][iy]++
    }
    return bins
}
