package io.github.koalaplot.core.polar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.koalaplot.core.util.AngularValue
import io.github.koalaplot.core.util.rad
import kotlin.math.PI

/**
 * An interface for classes representing the angular axis on a polar plot.
 *
 * @param T The data type for the axis values.
 */
public interface AngularAxisModel<T> {
    /**
     * Defines the direction for incrementing angles on a [PolarGraph]. Counter clockwise is the regular convention for
     * mathematics.
     */
    public enum class AngleDirection {
        CLOCKWISE,
        COUNTERCLOCKWISE
    }

    /**
     * Defines the orientation of the Zero angle on a [PolarGraph]. 3 O'Clock is the regular convention for
     * mathematics, while 12 O'Clock is typical for category based charts and spider charts.
     */
    public enum class AngleZero {
        THREE_OCLOCK,
        SIX_OCLOCK,
        NINE_OCLOCK,
        TWELVE_OCLOCK,
    }

    /**
     * Gets the tick values for this axis model.
     */
    public fun getTickValues(): List<T>

    /**
     * Computes the angular offset of the provided point along this axis.
     * This function provides a mechanism to transform from arbitrary units [T] to angular units on the plot.
     * For example, a category axis can convert specific categories to
     * their angular position on the plot.
     */
    public fun computeOffset(point: T): AngularValue

    /**
     * The [AngleDirection] for this axis.
     */
    public val angleDirection: AngleDirection

    /**
     * The [AngleZero] for this axis.
     */
    public val angleZero: AngleZero
}

/**
 * An [AngularAxisModel] that uses category values evenly spaced around the perimeter of the Polar plot. Values
 * plotted on this axis can only exactly match one of the category values.
 *
 * @param categories The category values represented by this axis.
 */
public data class CategoryAngularAxisModel<T>(
    private val categories: List<T>,
    public override val angleDirection: AngularAxisModel.AngleDirection = AngularAxisModel.AngleDirection.CLOCKWISE,
    public override val angleZero: AngularAxisModel.AngleZero = AngularAxisModel.AngleZero.TWELVE_OCLOCK
) : AngularAxisModel<T> {
    override fun getTickValues(): List<T> = categories

    override fun computeOffset(point: T): AngularValue {
        val index = categories.indexOf(point)
        require(index != -1) { "The provided category '$point' is not a valid value for this axis." }
        return ((index * 2.0 * PI) / categories.size).rad
    }
}

/**
 * Creates and remembers a [CategoryAngularAxisModel].
 */
@Composable
public fun <T> rememberCategoryAngularAxisModel(
    categories: List<T>,
    angleDirection: AngularAxisModel.AngleDirection = AngularAxisModel.AngleDirection.CLOCKWISE,
    angleZero: AngularAxisModel.AngleZero = AngularAxisModel.AngleZero.TWELVE_OCLOCK
): CategoryAngularAxisModel<T> =
    remember(categories, angleDirection, angleZero) {
        CategoryAngularAxisModel(categories, angleDirection, angleZero)
    }

/**
 * An [AngularAxisModel] that uses [AngularValue]s of either Radians or Degrees.
 *
 * @param tickValues The angular value of each tick that should be drawn on the plot.
 */
public data class AngularValueAxisModel(
    private val tickValues: List<AngularValue> = buildList {
        @Suppress("MagicNumber")
        for (i in 0..<8) {
            add((PI * i / 4.0).rad)
        }
    },
    override val angleDirection: AngularAxisModel.AngleDirection = AngularAxisModel.AngleDirection.COUNTERCLOCKWISE,
    override val angleZero: AngularAxisModel.AngleZero = AngularAxisModel.AngleZero.THREE_OCLOCK
) : AngularAxisModel<AngularValue> {
    override fun getTickValues(): List<AngularValue> = tickValues

    override fun computeOffset(point: AngularValue): AngularValue = point
}

@Composable
public fun rememberAngularValueAxisModel(
    tickValues: List<AngularValue> = buildList {
        @Suppress("MagicNumber")
        for (i in 0..<8) {
            add((PI * i / 4.0).rad)
        }
    },
    angleDirection: AngularAxisModel.AngleDirection = AngularAxisModel.AngleDirection.COUNTERCLOCKWISE,
    angleZero: AngularAxisModel.AngleZero = AngularAxisModel.AngleZero.THREE_OCLOCK
): AngularValueAxisModel =
    remember(tickValues, angleDirection, angleZero) { AngularValueAxisModel(tickValues, angleDirection, angleZero) }
