package io.github.koalaplot.core.pie

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import org.junit.Rule
import org.junit.Test

class PieChartTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Test creating a pie chart with data of all zeros.
     */
    @OptIn(ExperimentalKoalaPlotApi::class)
    @Test
    fun zeroDataTest() {
        composeTestRule.setContent {
            PieChart(listOf(0f, 0f))
        }
    }

    /**
     * Test creating a pie chart with 5 values, last one zero.
     */
    @OptIn(ExperimentalKoalaPlotApi::class)
    @Test
    fun lastZeroDataTest() {
        composeTestRule.setContent {
            @Suppress("MagicNumber")
            PieChart(listOf(5f, 6f, 7f, 8f, 0f))
        }
    }

    @OptIn(ExperimentalKoalaPlotApi::class)
    @Test
    fun removeSliceTest() {
        @Suppress("MagicNumber")
        val pieData = mutableStateListOf(1f, 2f, 3f)

        composeTestRule.setContent {
            println(pieData.map { it })

            PieChart(
                pieData,
                label = {
                    println("label: $it, $pieData")
                    assert(it <= pieData.lastIndex) {
                        "$it not less than or equal to ${pieData.lastIndex}"
                    }
                },
            )
        }

        composeTestRule.waitForIdle()
        pieData.removeAt(2)
        composeTestRule.waitForIdle()
    }

    @OptIn(ExperimentalKoalaPlotApi::class)
    @Test
    fun addSliceTest() {
        @Suppress("MagicNumber")
        val pieData = mutableStateListOf(1f, 2f, 3f)
        composeTestRule.setContent {
            println(pieData.map { it })

            PieChart(
                pieData,
                label = {
                    println("label: $it, $pieData")
                    assert(it <= pieData.lastIndex) {
                        "$it not less than or equal to ${pieData.lastIndex}"
                    }
                },
            )
        }
        composeTestRule.waitForIdle()
        @Suppress("MagicNumber")
        pieData.add(4f)
        composeTestRule.waitForIdle()
    }

    /**
     * Test that a pie measured with tight constraints still fills the space it was given. Labels must not
     * be measured with the chart's own minimum constraints, which would size each of them to the whole
     * chart and leave no room for the pie.
     */
    @OptIn(ExperimentalKoalaPlotApi::class)
    @Test
    fun tightConstraintsTest() {
        val chartSize = 400.dp
        val minPieDiameter = 100.dp
        val holeSize = 0.5f

        composeTestRule.setContent {
            Box(modifier = Modifier.size(chartSize)) {
                PieChart(
                    values = listOf(1f, 1f, 1f, 1f),
                    modifier = Modifier.fillMaxSize(),
                    holeSize = holeSize,
                    minPieDiameter = minPieDiameter,
                    label = { Box(modifier = Modifier.size(20.dp)) },
                    holeContent = { Box(modifier = Modifier.fillMaxSize().testTag(HoleTag)) },
                )
            }
        }
        composeTestRule.waitForIdle()

        val collapsedHoleSize = with(composeTestRule.density) { (minPieDiameter * holeSize).roundToPx() }
        val hole = composeTestRule.onNodeWithTag(HoleTag).fetchSemanticsNode()

        assert(hole.size.width > collapsedHoleSize) {
            "pie collapsed to its minimum diameter: hole is ${hole.size.width}px, expected wider than " +
                "$collapsedHoleSize px"
        }
        assert(!hole.boundsInRoot.isEmpty) {
            "pie was placed outside of the chart's bounds: hole bounds are ${hole.boundsInRoot}"
        }
    }

    private companion object {
        const val HoleTag = "hole"
    }
}
