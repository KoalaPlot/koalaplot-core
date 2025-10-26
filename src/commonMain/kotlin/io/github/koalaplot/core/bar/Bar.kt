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
import io.github.koalaplot.core.xygraph.XYGraphScope

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
@Deprecated(
    message = "Delegates to vertical solid bar. Use explicitly dedicated factory function.",
    replaceWith = ReplaceWith("verticalSolidBar(color, shape, border)")
)
public fun <X, Y> solidBar(
    color: Color,
    shape: Shape = RectangleShape,
    border: BorderStroke? = null,
): DefaultVerticalBarComposable<X, Y> = verticalSolidBar(color, shape, border)

/**
 * Factory function to create a Composable that emits a solid colored bar.
 */
public fun <X, Y> verticalSolidBar(
    color: Color,
    shape: Shape = RectangleShape,
    border: BorderStroke? = null,
): DefaultVerticalBarComposable<X, Y> = { _, _, _ ->
    DefaultBar(SolidColor(color), shape = shape, border = border)
}

/**
 * Factory function to create a Composable that emits a solid colored bar.
 */
public fun <X, Y> horizontalSolidBar(
    color: Color,
    shape: Shape = RectangleShape,
    border: BorderStroke? = null,
): DefaultHorizontalBarComposable<X, Y> = { _, _, _ ->
    DefaultBar(SolidColor(color), shape = shape, border = border)
}

/**
 * Factory function to create a Composable that emits a solid colored bar.
 * Each bar features a planar starting side and a convex ending side.
 */
public fun <X> XYGraphScope<X, Float>.verticalPlanoConvexBar(
    color: Color,
    border: BorderStroke? = null,
): DefaultVerticalBarComposable<X, Float> = { _, index, value ->
    DefaultBar(
        brush = SolidColor(color),
        shape = VerticalPlanoConvexShape(this@verticalPlanoConvexBar, index, value),
        border = border
    )
}

/**
 * Factory function to create a Composable that emits a solid colored bar.
 * Each bar features a convex shape on both its starting and ending sides.
 * There's an additional convex cutout at the bottom of the bar.
 */
public fun <X> XYGraphScope<X, Float>.verticalBiConvexBar(
    color: Color,
    border: BorderStroke? = null,
): DefaultVerticalBarComposable<X, Float> = { _, index, value ->
    DefaultBar(
        brush = SolidColor(color),
        shape = VerticalBiConvexShape(this@verticalBiConvexBar, index, value),
        border = border
    )
}

/**
 * Factory function to create a Composable that emits a solid colored bar.
 * Each bar features a planar shape at one end and a convex shape at the other.
 */
public fun <X> XYGraphScope<Float, X>.horizontalPlanoConvexBar(
    color: Color,
    border: BorderStroke? = null,
): DefaultHorizontalBarComposable<Float, X> = { _, index, value ->
    DefaultBar(
        brush = SolidColor(color),
        shape = HorizontalPlanoConvexShape(this@horizontalPlanoConvexBar, index, value),
        border = border
    )
}

/**
 * Factory function to create a Composable that emits a solid colored bar.
 * Each bar features a convex shape on both its starting and ending sides.
 * There's an additional convex cutout at the bottom of the bar.
 */
public fun <X> XYGraphScope<Float, X>.horizontalBiConvexBar(
    color: Color,
    border: BorderStroke? = null,
): DefaultHorizontalBarComposable<Float, X> = { _, index, value ->
    DefaultBar(
        brush = SolidColor(color),
        shape = HorizontalBiConvexShape(this@horizontalBiConvexBar, index, value),
        border = border
    )
}
