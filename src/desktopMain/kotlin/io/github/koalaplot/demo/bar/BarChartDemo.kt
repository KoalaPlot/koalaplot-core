package io.github.koalaplot.demo.bar

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.koalaplot.core.bar.GroupedVerticalBarPlot
import io.github.koalaplot.core.bar.solidBar
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.CategoryAxisModel
import io.github.koalaplot.core.xygraph.XYGraph
import io.github.koalaplot.core.xygraph.rememberAxisStyle
import io.github.koalaplot.core.xygraph.rememberFloatLinearAxisModel
import io.github.koalaplot.demo.util.ColorUtil

private const val ITEM_WIDTH = 50f

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
internal fun BarChartDemo() {
    val graph = List(6) { BarChartModel((it + 1).toString(), List(3) { StackValue((it + 1).toFloat()) }) }
    val maxYValue = graph.maxOf { it.stackValues.maxOf { it.counter } }
    val width = graph.size * ITEM_WIDTH

    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(vertical = 16.dp).height(400.dp).width(width.dp)
    ) {
        XYGraph(
            xAxisModel = remember { CategoryAxisModel(graph.map { it.xLabel }) },
            yAxisModel = rememberFloatLinearAxisModel(0f..maxYValue, minorTickCount = 0),
            yAxisLabels = { it.toInt().toString() },
            yAxisStyle = rememberAxisStyle(
                color = MaterialTheme.colorScheme.primary,
                lineWidth = 2.dp,
                labelRotation = 0,
            ),
            xAxisStyle = rememberAxisStyle(
                color = MaterialTheme.colorScheme.primary,
                lineWidth = 2.dp,
                labelRotation = 45,
            ),
            panEnabled = true,
            zoomEnabled = true,
            allowIndependentZoom = true,
        ) {
            GroupedVerticalBarPlot {
                graph.forEach { item ->
                    item.stackValues.forEachIndexed { i, stackValue ->
                        series(
                            solidBar(
                                color = ColorUtil.createRandomColor(),
                                shape = RoundedCornerShape(
                                    topStart = 6.dp,
                                    topEnd = 6.dp,
                                )
                            )
                        ) {
                            item(item.xLabel, 0f, stackValue.counter)
                        }
                    }
                }
            }
        }
    }
}

internal data class BarChartModel(
    val xLabel: String,
    val stackValues: List<StackValue>,
)

internal data class StackValue(
    val counter: Float,
)
