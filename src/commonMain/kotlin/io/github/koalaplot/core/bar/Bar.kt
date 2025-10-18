package io.github.koalaplot.core.bar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor

/**
 * A default implementation of a bar for bar charts.
 * @param brush The brush to paint the bar with
 * @param shape An optional shape for the bar.
 * @param border An optional border for the bar.
 * @param hoverElement An optional Composable to be displayed over the bar when hovered over by the
 * mouse or pointer.
 */
@Composable
public fun BarScope.DefaultBar(
    brush: Brush,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    border: BorderStroke? = null,
    hoverElement: @Composable () -> Unit = {},
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
 * A simplified DefaultVerticalBar that uses a Solid Color [color] and default [RectangleShape].
 */
@Composable
public fun BarScope.DefaultBar(
    color: Color,
    shape: Shape = RectangleShape,
    border: BorderStroke? = null,
) {
    DefaultBar(SolidColor(color), shape = shape, border = border)
}

/**
 * Factory function to create a Composable that emits a solid colored bar.
 */
public fun solidBar(
    color: Color,
    shape: Shape = RectangleShape,
    border: BorderStroke? = null,
): @Composable BarScope.() -> Unit = {
    DefaultBar(SolidColor(color), shape = shape, border = border)
}
