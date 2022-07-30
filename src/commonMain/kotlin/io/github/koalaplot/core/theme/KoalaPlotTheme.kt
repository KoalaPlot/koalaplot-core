package io.github.koalaplot.core.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.koalaplot.core.legend.LegendLocation
import io.github.koalaplot.core.xychart.LineStyle
import io.github.koalaplot.core.xychart.TickPosition

/**
 * KiwkCharts components use values provided here when retrieving default values.
 *
 * Use this to configure the overall theme of elements within this KoalaPlotTheme.
 *
 * Any values that are not set will inherit the current value from the theme, falling back to the
 * defaults if there is no parent KoalaPlotTheme. This allows using a KoalaPlotTheme at the top
 * of your application, and then separate KoalaPlotTheme(s) for different screens / parts of your
 * UI, overriding only the parts of the theme definition that need to change.
 *
 * @param sizes Defines sizes for various elements
 * @param animationSpec How graph generation should be animated
 * @param axis Axis and grid visual characteristics
 * @param legendLocation Where legends should be located within Charts
 */
@Composable
public fun KoalaPlotTheme(
    sizes: Sizes = KoalaPlotTheme.sizes,
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec,
    axis: Axis = KoalaPlotTheme.axis,
    legendLocation: LegendLocation = KoalaPlotTheme.legendLocation,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalSizes provides sizes,
        LocalAnimationSpec provides animationSpec,
        LocalAxis provides axis,
        LocalLegendLocation provides legendLocation
    ) {
        content()
    }
}

/**
 * Contains functions to access the current theme values provided at the call site's position in
 * the hierarchy.
 */
public object KoalaPlotTheme {
    public val sizes: Sizes
        @Composable
        @ReadOnlyComposable
        get() = LocalSizes.current

    public val animationSpec: AnimationSpec<Float>
        @Composable
        @ReadOnlyComposable
        get() = LocalAnimationSpec.current

    public val axis: Axis
        @Composable
        @ReadOnlyComposable
        get() = LocalAxis.current

    public val legendLocation: LegendLocation
        @Composable
        @ReadOnlyComposable
        get() = LocalLegendLocation.current
}

/**
 * Theme sizes.
 *
 * @property symbol The size for symbols
 * @property gap The size of gaps, margins, and padding
 */
public data class Sizes(
    val symbol: Dp = 8.dp,
    val gap: Dp = 8.dp,
    val barWidth: Float = 0.8f
)

/**
 * CompositionLocal used to pass [Axis] theme properties down the tree.
 *
 * Setting the value here is typically done as part of [KoalaPlotTheme].
 * To retrieve the current value of this CompositionLocal, use [KoalaPlotTheme.axis].
 */
internal val LocalSizes = staticCompositionLocalOf { Sizes() }

/**
 * Theme properties for Axes and grid lines.
 *
 * @property color Axis color
 * @property majorTickSize The length of major tick marks
 * @property minorTickSize The length of minor tick marks
 * @property lineThickness The line thickness of axes
 * @property majorGridlineStyle The style for major gridlines, set to null to turn off
 * @property minorGridlineStyle The style for minor gridlines, set to null to turn off
 */
public data class Axis(
    val color: Color = Color.LightGray,
    val majorTickSize: Dp = 7.dp,
    val minorTickSize: Dp = 3.dp,
    val lineThickness: Dp = 0.dp,
    val tickPosition: TickPosition = TickPosition.Outside,
    val majorGridlineStyle: LineStyle? = LineStyle(
        SolidColor(Color.LightGray),
        strokeWidth = 0.dp
    ),
    val minorGridlineStyle: LineStyle? = LineStyle(
        SolidColor(Color.LightGray),
        strokeWidth = 0.dp
    )
)

internal val LocalAxis = staticCompositionLocalOf { Axis() }

internal val LocalAnimationSpec = staticCompositionLocalOf<AnimationSpec<Float>> { spring(2f) }

internal val LocalLegendLocation = staticCompositionLocalOf { LegendLocation.LEFT }
