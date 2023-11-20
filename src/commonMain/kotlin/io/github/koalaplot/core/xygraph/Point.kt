package io.github.koalaplot.core.xygraph

/**
 * Represents a point on a 2-d plot.
 * @param X The data type of the first dimension/axis
 * @param Y The data type of the second dimension/axis
 */
public interface Point<X, Y> {
    /**
     * The x-axis value of this Point.
     */
    public val x: X

    /**
     * The y-axis value of this Point.
     */
    public val y: Y
}

/**
 * Creates a new DefaultPoint at the specified coordinates.
 * @param X The type of the x-axis value
 * @param Y The type of the y-axis value
 */
@Suppress("FunctionNaming")
public fun <X, Y> Point(x: X, y: Y): Point<X, Y> = DefaultPoint(x, y)

/**
 * Default implementation of the Point interface.
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 */
public data class DefaultPoint<X, Y>(override val x: X, override val y: Y) : Point<X, Y>
