package io.github.koalaplot.core.polar

/**
 * A model for a radial axis that uses Float values and is linear.
 *
 * @param tickValues Values for each tick on the axis. Must have at least 2 values.
 */
public class RadialAxisModel constructor(
    internal val tickValues: List<Float>,
) {
    init {
        require(tickValues.size >= 2) { "tickValues must have at least 2 values " }
    }

    private val sortedTickValues = tickValues.sorted()
    private val range = sortedTickValues.last() - sortedTickValues.first()

    public fun computeOffset(point: Float): Float {
        return (point - sortedTickValues.first()) / range
    }
}
