package io.github.koalaplot.core.xygraph

import androidx.compose.ui.unit.Dp

internal val TickRatios = listOf(0.1f, 0.2f, 0.5f, 1f, 2f)

internal const val ZoomRangeLimitDefault = 0.2
internal const val MinimumMajorTickIncrementDefault = 0.1f

public interface ILinearAxisModel<T> : AxisModel<T> where T : Comparable<T>, T : Number {
    public val range: ClosedRange<T>
    public val minimumMajorTickSpacing: Dp
}

internal fun <X, Y> List<Point<X, Y>>.toXList(): List<X> = object : AbstractList<X>() {
    override val size: Int
        get() = this@toXList.size

    override fun get(index: Int): X {
        return this@toXList[index].x
    }
}

internal fun <X, Y> List<Point<X, Y>>.toYList(): List<Y> = object : AbstractList<Y>() {
    override val size: Int
        get() = this@toYList.size

    override fun get(index: Int): Y {
        return this@toYList[index].y
    }
}
