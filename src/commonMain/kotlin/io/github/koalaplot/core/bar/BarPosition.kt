package io.github.koalaplot.core.bar

/**
 * An interface that defines the dependent axis position of bars on a bar plot, e.g., y-axis coordinates
 * for a vertical bar plot.
 */
public interface BarPosition<P> {
    /**
     * The lowest value at which the bar begins
     */
    public val start: P

    /**
     * The highest value at which the bar ends
     */
    public val end: P
}

/**
 * A default implementation of the [BarPosition] interface.
 */
public data class DefaultBarPosition<P>(
    public override val start: P,
    public override val end: P,
) : BarPosition<P>

/**
 * Represents a set of points for a [GroupedVerticalBarPlot].
 *
 * @param I The type of the independent-axis values
 * @param D The type of the dependent-axis values
 */
public interface BarPlotGroupedPointEntry<I, D> {
    /**
     * The independent-axis value of the entry.
     */
    public val i: I

    /**
     * The dependent-axis values for each series corresponding to the independent-axis value.
     */
    public val d: List<BarPosition<D>>
}

public data class DefaultBarPlotGroupedPointEntry<I, D>(
    override val i: I,
    override val d: List<BarPosition<D>>,
) : BarPlotGroupedPointEntry<I, D>
