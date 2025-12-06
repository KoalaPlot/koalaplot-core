package io.github.koalaplot.core.xygraph

import androidx.compose.runtime.Composable

/**
 * Defines the content and styling for an axis on an [XYGraph].
 *
 * @param T The data type for the axis.
 * @param style The [AxisStyle] to apply to the axis.
 * @param labels A composable for rendering the axis labels.
 * @param title An optional title for the axis.
 */
public data class AxisContent<T>(
    val labels: @Composable AxisLabelScope<T>.(T) -> Unit,
    val title: @Composable () -> Unit,
    val style: AxisStyle,
)
