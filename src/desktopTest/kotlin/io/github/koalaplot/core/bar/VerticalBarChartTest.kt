package io.github.koalaplot.core.bar

import androidx.compose.ui.test.junit4.createComposeRule
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.XYGraph
import io.github.koalaplot.core.xygraph.rememberFloatLinearAxisModel
import org.junit.Rule
import org.junit.Test

class VerticalBarChartTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Test creating a bar chart with only 1 data value.
     */
    @OptIn(ExperimentalKoalaPlotApi::class)
    @Test
    fun oneBarTest() {
        composeTestRule.setContent {
            XYGraph(
                rememberFloatLinearAxisModel(0f..10f),
                rememberFloatLinearAxisModel(0f..10f),
            ) {
                VerticalBarPlot(
                    listOf(
                        DefaultVerticalBarPlotEntry(
                            5f,
                            DefaultBarPosition(0f, 10f)
                        )
                    ),
                    bar = { _, _, _ -> }
                )
            }
        }
    }
}
