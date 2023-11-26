package io.github.koalaplot.core.polar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * A model for a radial axis that transforms from axis space to drawing space.
 */
public interface RadialAxisModel<T> {
    /**
     * Provides a list of tick values that should be rendered for the axis.
     */
    public val tickValues: List<T>

    /**
     * Transforms the provided [point] in this axis space to the fraction representing the point's distance
     * from the origin relative to the overall axis length.
     */
    public fun computeOffset(point: T): Float
}

/**
 * A model for a radial axis that uses Float values and is linear.
 *
 * @param tickValues Values for each tick on the axis. Must have at least 2 values.
 */
public class FloatRadialAxisModel constructor(
    override val tickValues: List<Float>,
) : RadialAxisModel<Float> {
    init {
        require(tickValues.size >= 2) { "tickValues must have at least 2 values " }
    }

    private val sortedTickValues = tickValues.sorted()
    private val range = sortedTickValues.last() - sortedTickValues.first()

    public override fun computeOffset(point: Float): Float {
        return (point - sortedTickValues.first()) / range
    }
}

@Composable
public fun rememberFloatRadialAxisModel(tickValues: List<Float>): FloatRadialAxisModel =
    remember(tickValues) { FloatRadialAxisModel(tickValues) }
