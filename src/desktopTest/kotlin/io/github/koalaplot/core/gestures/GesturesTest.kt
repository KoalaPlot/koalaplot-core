package io.github.koalaplot.core.gestures

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.util.ZoomFactor
import io.github.koalaplot.core.xygraph.FloatLinearAxisModel
import io.github.koalaplot.core.xygraph.XYGraph
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs
import kotlin.test.assertEquals

class GesturesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testApplyPanLocks() {
        val testPanXValue = 110f
        val testPanYValue = 150f
        val testPan = Offset(testPanXValue, testPanYValue)

        val actual1 = testPan.applyPanLocks(panXEnabled = false, panYEnabled = false)
        val actual2 = testPan.applyPanLocks(panXEnabled = false, panYEnabled = true)
        val actual3 = testPan.applyPanLocks(panXEnabled = true, panYEnabled = false)
        val actual4 = testPan.applyPanLocks(panXEnabled = true, panYEnabled = true)

        assertEquals(actual1.x, DefaultPanValue)
        assertEquals(actual1.y, DefaultPanValue)

        assertEquals(actual2.x, DefaultPanValue)
        assertEquals(actual2.y, testPanYValue)

        assertEquals(actual3.x, testPanXValue)
        assertEquals(actual3.y, DefaultPanValue)

        assertEquals(actual4.x, testPanXValue)
        assertEquals(actual4.y, testPanYValue)
    }

    @Test
    fun testApplyZoomLocks() {
        val testZoomXValue = 3.4f
        val testZoomYValue = 1.7f
        val testZoomFactor = ZoomFactor(testZoomXValue, testZoomYValue)

        val actual1 = testZoomFactor.applyZoomLocks(zoomXEnabled = false, zoomYEnabled = false)
        val actual2 = testZoomFactor.applyZoomLocks(zoomXEnabled = false, zoomYEnabled = true)
        val actual3 = testZoomFactor.applyZoomLocks(zoomXEnabled = true, zoomYEnabled = false)
        val actual4 = testZoomFactor.applyZoomLocks(zoomXEnabled = true, zoomYEnabled = true)

        assertEquals(actual1, ZoomFactor.Neutral)

        assertEquals(actual2.x, ZoomFactor.NeutralPoint)
        assertEquals(actual2.y, testZoomYValue)
        assertEquals(actual2.y, testZoomFactor.y)

        assertEquals(actual3.x, testZoomXValue)
        assertEquals(actual3.x, testZoomFactor.x)
        assertEquals(actual3.y, ZoomFactor.NeutralPoint)

        assertEquals(actual4.x, testZoomXValue)
        assertEquals(actual4.x, testZoomFactor.x)
        assertEquals(actual4.y, testZoomYValue)
        assertEquals(actual4.y, testZoomFactor.y)
    }

    @Test
    fun testGetMaxZoomDeviation() {
        val testCases = listOf(
            ZoomFactor(-1.0f, 2.0f),
            ZoomFactor(2.0f, -1.0f),
            ZoomFactor(0.5f, 1.5f),
            ZoomFactor(3.0f, 1.0f),
            ZoomFactor(-0.5f, 2.5f),
            ZoomFactor(0.0f, 0.0f),
            ZoomFactor(1.0f, 1.0f),
            ZoomFactor(-2.0f, 4.0f),
            ZoomFactor(2.5f, -0.5f),
            ZoomFactor(-1.5f, -3.5f)
        )

        for (testCase in testCases) {
            val expected = if (abs(testCase.x - ZoomFactor.NeutralPoint) > abs(testCase.y - ZoomFactor.NeutralPoint)) {
                testCase.x
            } else {
                testCase.y
            }

            val actual = getMaxZoomDeviation(testCase.x, testCase.y)
            assertEquals(expected, actual, "Failed for $testCase")
        }
    }

    @OptIn(ExperimentalKoalaPlotApi::class)
    @Test
    fun testGestureConfig() {
        val gestureConfig1 = GestureConfig(
            panXEnabled = true,
            panYEnabled = true,
            panXConsumptionEnabled = true,
            panYConsumptionEnabled = true,
            zoomXEnabled = true,
            zoomYEnabled = true,
            independentZoomEnabled = true
        )

        val gestureConfig2 = GestureConfig(
            panXEnabled = false,
            panYEnabled = false,
            panXConsumptionEnabled = false,
            panYConsumptionEnabled = false,
            zoomXEnabled = false,
            zoomYEnabled = false,
            independentZoomEnabled = false
        )

        val gestureConfig3 = GestureConfig(
            panXEnabled = true,
            panYEnabled = false,
            panXConsumptionEnabled = true,
            panYConsumptionEnabled = false,
            zoomXEnabled = true,
            zoomYEnabled = false,
            independentZoomEnabled = true
        )

        val gestureConfig4 = GestureConfig(
            panXEnabled = false,
            panYEnabled = true,
            panXConsumptionEnabled = false,
            panYConsumptionEnabled = true,
            zoomXEnabled = false,
            zoomYEnabled = true,
            independentZoomEnabled = false
        )

        composeTestRule.setContent {
            @Composable
            fun testXYGraphWithGestureConfig(gestureConfig: GestureConfig) {
                XYGraph(
                    xAxisModel = FloatLinearAxisModel(
                        -100f..100f,
                    ),
                    yAxisModel = FloatLinearAxisModel(
                        range = -100f..100f
                    ),
                    gestureConfig = gestureConfig
                ) {
                }
            }

            testXYGraphWithGestureConfig(gestureConfig = gestureConfig1)
            testXYGraphWithGestureConfig(gestureConfig = gestureConfig2)
            testXYGraphWithGestureConfig(gestureConfig = gestureConfig3)
            testXYGraphWithGestureConfig(gestureConfig = gestureConfig4)
        }
    }
}
