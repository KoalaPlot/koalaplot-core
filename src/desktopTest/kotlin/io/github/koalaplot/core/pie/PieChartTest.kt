package io.github.koalaplot.core.pie

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.junit4.createComposeRule
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
}
