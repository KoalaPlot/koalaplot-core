package io.github.koalaplot.core.xygraph

import androidx.compose.ui.unit.Dp

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

    override fun computeTickValues(axisLength: Dp): io.github.koalaplot.core.xygraph.TickValues<T> {
        return TickValues(categories, listOf())
    }

    override fun computeValue(offset: Float): T {
        // The offset is expected to be in the 0~1 range. Handle exceptions if out of bounds.
        require(offset in 0f..1f) { "Offset ($offset) must be in [0,1]" }

        // When firstCategoryIsZero == true:
        //   computeOffset(point) is index / categories.size
        //   -> index = offset * categories.size
        // When firstCategoryIsZero == false:
        //   computeOffset(point) is (index + 1) / (categories.size + 1)
        //   -> index = offset * (categories.size + 1) - 1
        val rawIndex = if (firstCategoryIsZero) {
            (offset * categories.size)
        } else {
            (offset * (categories.size + 1) - 1)
        }

        // Adjust safely to prevent negative values or exceeding (categories.size - 1)
        val index = rawIndex.toInt().coerceIn(0, categories.size - 1)

        return categories[index]
    }

    private data class TickValues<T>(override val majorTickValues: List<T>, override val minorTickValues: List<T>) :
        io.github.koalaplot.core.xygraph.TickValues<T>
}
