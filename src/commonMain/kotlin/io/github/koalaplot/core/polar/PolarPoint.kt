package io.github.koalaplot.core.polar

/**
 * Represents a point on a 2-d polar plot.
 * @param R The data type of the radial dimension
 * @param T The data type of the angular dimension
 */
public interface PolarPoint<R, T> {
    /**
     * The radius value of this Point.
     */
    public val r: R

    /**
     * The angular value of this Point.
     */
    public val theta: T
}

/**
 * Creates a new [DefaultPolarPoint] at the specified coordinates.
 * @param R The type of the radius axis value
 * @param T The type of the angular axis value
 */
@Suppress("FunctionNaming")
public fun <R, T> PolarPoint(
    r: R,
    theta: T,
): PolarPoint<R, T> = DefaultPolarPoint(r, theta)

/**
 * Default implementation of the [PolarPoint] interface.
 * @param R The type of the radius axis value
 * @param T The type of the angular axis value
 */
public data class DefaultPolarPoint<R, T>(
    override val r: R,
    override val theta: T,
) : PolarPoint<R, T>
