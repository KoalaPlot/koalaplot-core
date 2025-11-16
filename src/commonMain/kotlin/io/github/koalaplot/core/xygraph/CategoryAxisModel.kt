package io.github.koalaplot.core.xygraph

import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt

/**
 * An axis that uses arbitrary category objects instead of numbers as its values. It is a discrete
 * axis.
 * @param T The data type of the axis values.
 * @param categories The objects that represent values on this axis. If objects are duplicated
 * as defined by their equals method, the behavior is undefined.
 * @param categoryAxisOffset Specifies how much space to leave at the beginning and end of the
 * axis. See [CategoryAxisOffset].
 */
public class CategoryAxisModel<T>(
    private val categories: List<T>,
    private val categoryAxisOffset: CategoryAxisOffset = CategoryAxisOffset.Full,
) : AxisModel<T> {

    public companion object {
        /**
         * Creates a [CategoryAxisModel].
         *
         * @param T The data type of the axis values.
         * @param categories The objects that represent values on this axis.
         * @param firstCategoryIsZero If true, the first category will be placed at the beginning
         * of the axis. If false, all categories will be evenly spaced along the axis with a gap
         * at the beginning and end.
         */
        @Deprecated(message = "Use CategoryAxisOffset instead of firstCategoryIsZero", replaceWith = ReplaceWith(""))
        public operator fun <T> invoke(
            categories: List<T>,
            firstCategoryIsZero: Boolean = false
        ): CategoryAxisModel<T> {
            return CategoryAxisModel(
                categories,
                if (firstCategoryIsZero) CategoryAxisOffset.None else CategoryAxisOffset.Full
            )
        }

        /**
         * Creates a [CategoryAxisModel].
         *
         * @param T The data type of the axis values.
         * @param categories The objects that represent values on this axis.
         * @param categoryAxisOffset Specifies how much space to leave at the beginning and end of the
         * axis. See [CategoryAxisOffset].
         */
        public operator fun <T> invoke(
            vararg categories: T,
            categoryAxisOffset: CategoryAxisOffset = CategoryAxisOffset.Full,
        ): CategoryAxisModel<T> {
            return CategoryAxisModel(categories.toList(), categoryAxisOffset)
        }
    }

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

        if (categories.size == 1) {
            return if (categoryAxisOffset == CategoryAxisOffset.None) {
                0f
            } else {
                @Suppress("MagicNumber")
                0.5f
            }
        }

        @Suppress("MagicNumber")
        val offsetValue = when (categoryAxisOffset) {
            is CategoryAxisOffset.Custom -> categoryAxisOffset.offset
            CategoryAxisOffset.Full -> 1.0f
            CategoryAxisOffset.Half -> 0.5f
            CategoryAxisOffset.None -> 0.0f
        }

        // Total divisions are the gaps between categories plus the start/end offsets
        val totalDivisions = (categories.size - 1) + (2 * offsetValue)

        // Position of the category is its index plus the starting offset
        val categoryPosition = index + offsetValue

        return categoryPosition / totalDivisions
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

/**
 * Specifies the amount of space to leave at the beginning and end of a category axis. The
 * offset is defined as a fraction of the space between two adjacent categories.
 */
public sealed interface CategoryAxisOffset {
    /**
     * Places the first category at the start of the axis and the last category at the end.
     */
    public object None : CategoryAxisOffset

    /**
     * Reserves space at the beginning and end of the axis equal to half the interval between
     * categories. This positions the first and last categories inward from the axis ends.
     */
    public object Half : CategoryAxisOffset

    /**
     * Reserves space at the beginning and end of the axis equal to the full interval between
     * categories.
     */
    public object Full : CategoryAxisOffset

    /**
     * Specifies a custom amount of space to reserve at the beginning and end of the axis.
     * The [offset] value is a fraction of the interval between categories.
     *
     * An [offset] of 0.0 is equivalent to [None], 0.5 to [Half], and 1.0 to [Full].
     *
     * @param offset The fractional value for the reserved space, must be between 0.0 and 1.0 (inclusive).
     */
    public data class Custom(val offset: Float) : CategoryAxisOffset {
        init {
            require(offset in 0f..1f)
        }
    }
}
