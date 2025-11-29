package io.github.koalaplot.core.xygraph

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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

@Composable
public fun <T> rememberAxisContent(
    labels: @Composable AxisLabelScope<T>.(T) -> Unit = {
        Text(
            it.toString(),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(2.dp)
        )
    },
    title: @Composable () -> Unit = {},
    style: AxisStyle = rememberAxisStyle(),
): AxisContent<T> = remember(labels, title, style) {
    AxisContent(
        labels = labels,
        title = title,
        style = style,
    )
}

/**
 * Creates and remembers an [AxisContent] for an axis using default [Text] Composables.
 * This is a convenience overload for cases where axis labels and titles are simple strings.
 *
 * @param T The data type for the axis.
 * @param label A function to convert a tick value of type [T] to a String.
 * @param title An optional string for the axis title.
 * @param style The [AxisStyle] to apply, defaults to [rememberAxisStyle].
 */
@Composable
public fun <T> rememberStringAxisContent(
    label: (T) -> String = { it.toString() },
    title: String? = null,
    style: AxisStyle = rememberAxisStyle(),
): AxisContent<T> = remember(label, title, style) {
    AxisContent(
        style = style,
        labels = {
            Text(
                label(it),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(2.dp)
            )
        },
        title = {
            if (title != null) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        title,
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    )
}
