package io.github.koalaplot.core.xychart

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * An axis that uses arbitrary category objects instead of numbers as its values. It is a discrete
 * axis.
 * @param T The data type of the axis values.
 * @param categories The objects that represent values on this axis. If objects are duplicated
 * as defined by their equals method, the behavior is undefined.
 */
public class CategoryAxisModel<T>(
    private val categories: List<T>,
    override val minimumMajorTickSpacing: Dp = 50.dp,
) : AxisModel<T> {
    /**
     * Returns the offset of the provided string within this Category axis. Returns NaN if
     * [point] is not a valid category.
     * @param point One of the categories for this axis.
     * @return The offset of the category along the axis, between 0 and 1
     */
    override fun computeOffset(point: T): Float {
        val index = categories.indexOf(point)
        return if (index == -1) {
            Float.NaN
        } else {
            ((index + 1).toFloat() / (categories.size + 1).toFloat())
        }
    }

    override fun computeTickValues(axisLength: Dp): io.github.koalaplot.core.xychart.TickValues<T> {
        return TickValues(categories, listOf())
    }

    private data class TickValues<T>(override val majorTickValues: List<T>, override val minorTickValues: List<T>) :
        io.github.koalaplot.core.xychart.TickValues<T>
}
