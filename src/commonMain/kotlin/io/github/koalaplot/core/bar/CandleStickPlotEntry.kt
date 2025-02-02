package io.github.koalaplot.core.bar

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import io.github.koalaplot.core.style.KoalaPlotTheme
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.XYGraphScope

/**
 * Represents a set of data for a single candle in a [CandleStickPlot].
 *
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 */
public interface CandleStickPlotEntry<X, Y> {
    /**
     * The x-axis value of this [CandleStickPlotEntry].
     */
    public val x: X

    /**
     * The opening price of the candle.
     */
    public val open: Y

    /**
     * The closing price of the candle.
     */
    public val close: Y

    /**
     * The highest price of the candle.
     */
    public val high: Y

    /**
     * The lowest price of the candle.
     */
    public val low: Y
}

/**
 * Returns an instance of a [CandleStickPlotEntry] using the provided data.
 */
public fun <X, Y> candleStickPlotEntry(x: X, open: Y, close: Y, high: Y, low: Y): CandleStickPlotEntry<X, Y> =
    object : CandleStickPlotEntry<X, Y> {
        override val x = x
        override val open = open
        override val close = close
        override val high = high
        override val low = low
    }

public const val WICKWIDTH: Float = 0.02f

@ExperimentalKoalaPlotApi
@Composable
public fun <X, Y : Comparable<Y>> XYGraphScope<X, Y>.CandleStickPlot(
    defaultCandle: @Composable BarScope.(entry: CandleStickPlotEntry<X, Y>) -> Unit = { _ -> },
    modifier: Modifier = Modifier,
    candleWidth: Float = 0.5f,
    wickWidth: Float = WICKWIDTH,
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec,
    colorForIncrease: Color = Color.Green,
    colorForDecrease: Color = Color.Red,
    content: CandleStickPlotScope<X, Y>.() -> Unit
) {
    val scope = remember(content, defaultCandle) { CandleStickPlotScopeImpl<X, Y>(defaultCandle) }
    val data = remember(scope) {
        scope.content()
        scope.data
    }

    val candleBodyEntries = data.map { entry ->
        verticalBarPlotEntry(entry.x, entry.open, entry.close)
    }

    val candleWickEntries = data.map { entry ->
        verticalBarPlotEntry(entry.x, entry.low, entry.high)
    }

    VerticalBarPlot(
        data = candleBodyEntries,
        modifier = modifier,
        barWidth = candleWidth,
        animationSpec = animationSpec,
        bar = { index ->
            val entry = data[index]
            val candleColor = if (entry.close >= entry.open) colorForIncrease else colorForDecrease
            DefaultCandleBody(
                color = candleColor,
                hoverElement = {
                    scope.candleContent.invoke(this, entry)
                }
            )
        }
    )

    VerticalBarPlot(
        data = candleWickEntries,
        modifier = modifier,
        barWidth = wickWidth,
        animationSpec = animationSpec,
        bar = { index ->
            val entry = data[index]
            val wickColor = if (entry.close >= entry.open) colorForIncrease else colorForDecrease
            DefaultCandleWick(
                color = wickColor,
                width = wickWidth,
                hoverElement = {
                    scope.candleContent.invoke(this, entry)
                }
            )
        }
    )
}

/**
 * Scope item to allow adding items to a [CandleStickPlot].
 */
public interface CandleStickPlotScope<X, Y> {

    public fun item(
        entry: CandleStickPlotEntry<X, Y>,
        candleContent: (@Composable BarScope.(entry: CandleStickPlotEntry<X, Y>) -> Unit)? = null
    )
}

internal class CandleStickPlotScopeImpl<X, Y>(
    private val defaultCandle: @Composable BarScope.(entry: CandleStickPlotEntry<X, Y>) -> Unit
) :
    CandleStickPlotScope<X, Y> {
    val data: MutableList<CandleStickPlotEntry<X, Y>> = mutableListOf()
    var candleContent: @Composable BarScope.(entry: CandleStickPlotEntry<X, Y>) -> Unit = defaultCandle

    override fun item(
        entry: CandleStickPlotEntry<X, Y>,
        candleContent: (
        @Composable BarScope.(entry: CandleStickPlotEntry<X, Y>) -> Unit
        )?
    ) {
        data.add(entry)
        if (candleContent != null) {
            this.candleContent = candleContent
        }
    }
}

/**
 * A default implementation of a candle body for candle stick charts.
 * @param brush The brush to paint the candle body with
 * @param shape An optional shape for the candle body.
 * @param border An optional border for the candle body.
 * @param hoverElement An optional Composable to be displayed over the candle body when hovered over by the
 * mouse or pointer.
 */
@Composable
public fun BarScope.DefaultCandleBody(
    brush: Brush,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    border: BorderStroke? = null,
    hoverElement: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier.fillMaxSize()
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .background(brush = brush, shape = shape)
            .clip(shape)
            .hoverableElement(hoverElement)
    )
}

/**
 * A simplified DefaultCandleBody that uses a Solid Color [color] and default [RectangleShape].
 */
@Composable
public fun BarScope.DefaultCandleBody(
    color: Color,
    shape: Shape = RectangleShape,
    border: BorderStroke? = null,
    hoverElement: @Composable () -> Unit = {}
) {
    DefaultCandleBody(SolidColor(color), shape = shape, border = border, hoverElement = hoverElement)
}

/**
 * A default implementation of a candle wick for candle stick charts.
 * @param color The color to paint the candle wick with
 * @param width The width of the candle wick
 * @param shape An optional shape for the candle wick.
 * @param border An optional border for the candle wick.
 * @param hoverElement An optional Composable to be displayed over the candle wick when hovered over by the
 * mouse or pointer.
 */
@Composable
public fun BarScope.DefaultCandleWick(
    color: Color,
    width: Float,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    border: BorderStroke? = null,
    hoverElement: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier.fillMaxSize()
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .background(color = color, shape = shape)
            .clip(shape)
            .hoverableElement(hoverElement)
    )
}