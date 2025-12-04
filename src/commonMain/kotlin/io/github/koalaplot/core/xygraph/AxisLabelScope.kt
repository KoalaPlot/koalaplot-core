package io.github.koalaplot.core.xygraph

public interface AxisLabelScope<T> {
    public val tickValues: TickValues<T>
}

internal data class DefaultAxisLabelScope<T>(
    override val tickValues: TickValues<T>,
) : AxisLabelScope<T>
