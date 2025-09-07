package io.github.koalaplot.core.xygraph

import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt

/**
 * An axis that uses arbitrary category objects instead of numbers as its values. It is a discrete
 * axis.
 * @param T The data type of the axis values.
 * @param categories The objects that represent values on this axis. If objects are duplicated
 * as defined by their equals method, the behavior is undefined.
 */
public class CategoryAxisModel<T>(
    private val categories: List<T>,
    private val firstCategoryIsZero: Boolean = false,
    // override val minimumMajorTickSpacing: Dp = 50.dp,
) : AxisModel<T> {
    /**
     * Returns the offset of the provided string within this Category axis.
     *
     * @throws IllegalArgumentException if [point] is not a valid category.
     * @param point One of the categories for this axis.
     * @return The offset of the category along the axis, between 0 and 1
     */
    override fun computeOffset(point: T): Float {
        val index = categories.indexOf(point)
        require(index != -1) { "The provided category '$point' is not a valid value for this axis." }
        return if (firstCategoryIsZero) {
            index.toFloat() / categories.size
        } else {
            ((index + 1).toFloat() / (categories.size + 1).toFloat())
        }
    }

    override fun offsetToValue(offset: Float): T {
        return categories[(offset * categories.size).roundToInt().coerceIn(categories.indices)]
    }

    override fun computeTickValues(axisLength: Dp): io.github.koalaplot.core.xygraph.TickValues<T> {
        return TickValues(categories, listOf())
    }

    private data class TickValues<T>(override val majorTickValues: List<T>, override val minorTickValues: List<T>) :
        io.github.koalaplot.core.xygraph.TickValues<T>
}
