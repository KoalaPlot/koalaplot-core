package io.github.koalaplot.core.xygraph

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.koalaplot.core.style.KoalaPlotTheme
import io.github.koalaplot.core.style.LineStyle

/**
 * Defines the styling for an [XYGraph]'s grid lines.
 *
 * @param horizontalMajorStyle The style for major horizontal grid lines.
 * @param horizontalMinorStyle The style for minor horizontal grid lines.
 * @param verticalMajorStyle The style for major vertical grid lines.
 * @param verticalMinorStyle The style for minor vertical grid lines.
 */
public data class GridStyle(
    val horizontalMajorStyle: LineStyle?,
    val horizontalMinorStyle: LineStyle?,
    val verticalMajorStyle: LineStyle?,
    val verticalMinorStyle: LineStyle?,
)

/**
 * Creates and remembers a [GridStyle] instance, providing default values from the current [KoalaPlotTheme].
 */
@Composable
public fun rememberGridStyle(
    horizontalMajorStyle: LineStyle? = KoalaPlotTheme.axis.majorGridlineStyle,
    horizontalMinorStyle: LineStyle? = KoalaPlotTheme.axis.minorGridlineStyle,
    verticalMajorStyle: LineStyle? = KoalaPlotTheme.axis.majorGridlineStyle,
    verticalMinorStyle: LineStyle? = KoalaPlotTheme.axis.minorGridlineStyle,
): GridStyle = remember(
    horizontalMajorStyle,
    horizontalMinorStyle,
    verticalMajorStyle,
    verticalMinorStyle,
) {
    GridStyle(
        horizontalMajorStyle = horizontalMajorStyle,
        horizontalMinorStyle = horizontalMinorStyle,
        verticalMajorStyle = verticalMajorStyle,
        verticalMinorStyle = verticalMinorStyle,
    )
}
