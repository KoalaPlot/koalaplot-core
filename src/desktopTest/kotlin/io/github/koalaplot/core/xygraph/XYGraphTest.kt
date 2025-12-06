package io.github.koalaplot.core.xygraph

import androidx.compose.ui.test.junit4.createComposeRule
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFailsWith

class XYGraphTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalKoalaPlotApi::class)
    @Test
    fun testTickIncrementLessThanRangeSucceeds() {
        val x = 1709868843600L
        val points = listOf(DefaultPoint(x - 2000, 0.0), DefaultPoint(x, 0.0))

        composeTestRule.setContent {
            XYGraph(
                xAxisModel = LongLinearAxisModel(
                    0L..2000L,
                    minorTickCount = 0,
                    minimumMajorTickIncrement = 1000,
                ),
                yAxisModel = DoubleLinearAxisModel(
                    points.autoScaleYRange(),
                    minorTickCount = 2,
                ),
                xAxisContent = AxisContent({}, {}, rememberAxisStyle()),
                yAxisContent = AxisContent({}, {}, rememberAxisStyle()),
            ) {
            }

            XYGraph(
                xAxisModel = IntLinearAxisModel(
                    0..2000,
                    minorTickCount = 0,
                    minimumMajorTickIncrement = 1000,
                ),
                yAxisModel = DoubleLinearAxisModel(
                    points.autoScaleYRange(),
                    minorTickCount = 2,
                ),
                xAxisContent = AxisContent({}, {}, rememberAxisStyle()),
                yAxisContent = AxisContent({}, {}, rememberAxisStyle()),
            ) {
            }

            XYGraph(
                xAxisModel = FloatLinearAxisModel(
                    0f..2000f,
                    minorTickCount = 0,
                    minimumMajorTickIncrement = 1000f,
                ),
                yAxisModel = DoubleLinearAxisModel(
                    points.autoScaleYRange(),
                    minorTickCount = 2,
                ),
                xAxisContent = AxisContent({}, {}, rememberAxisStyle()),
                yAxisContent = AxisContent({}, {}, rememberAxisStyle()),
            ) {
            }

            XYGraph(
                xAxisModel = DoubleLinearAxisModel(
                    0.0..2000.0,
                    minorTickCount = 0,
                    minimumMajorTickIncrement = 1000.0,
                ),
                yAxisModel = DoubleLinearAxisModel(
                    points.autoScaleYRange(),
                    minorTickCount = 2,
                ),
                xAxisContent = AxisContent({}, {}, rememberAxisStyle()),
                yAxisContent = AxisContent({}, {}, rememberAxisStyle()),
            ) {
            }
        }
    }

    @OptIn(ExperimentalKoalaPlotApi::class)
    @Test
    fun testTickIncrementGreaterThanRangeThrowsIllegalArgumentException() {
        val x = 1709868843600L
        val points = listOf(DefaultPoint(x - 2000, 0.0), DefaultPoint(x, 0.0))

        composeTestRule.setContent {
            assertFailsWith<IllegalArgumentException> {
                XYGraph(
                    xAxisModel = LongLinearAxisModel(
                        0L..2000L,
                        minorTickCount = 0,
                        minimumMajorTickIncrement = 5000,
                    ),
                    yAxisModel = DoubleLinearAxisModel(
                        points.autoScaleYRange(),
                        minorTickCount = 2,
                    ),
                    xAxisContent = AxisContent({}, {}, rememberAxisStyle()),
                    yAxisContent = AxisContent({}, {}, rememberAxisStyle()),
                ) {
                }
            }

            assertFailsWith<IllegalArgumentException> {
                XYGraph(
                    xAxisModel = IntLinearAxisModel(
                        0..2000,
                        minorTickCount = 0,
                        minimumMajorTickIncrement = 5000,
                    ),
                    yAxisModel = DoubleLinearAxisModel(
                        points.autoScaleYRange(),
                        minorTickCount = 2,
                    ),
                    xAxisContent = AxisContent({}, {}, rememberAxisStyle()),
                    yAxisContent = AxisContent({}, {}, rememberAxisStyle()),
                ) {
                }
            }

            assertFailsWith<IllegalArgumentException> {
                XYGraph(
                    xAxisModel = FloatLinearAxisModel(
                        0f..2000f,
                        minorTickCount = 0,
                        minimumMajorTickIncrement = 5000f,
                    ),
                    yAxisModel = DoubleLinearAxisModel(
                        points.autoScaleYRange(),
                        minorTickCount = 2,
                    ),
                    xAxisContent = AxisContent({}, {}, rememberAxisStyle()),
                    yAxisContent = AxisContent({}, {}, rememberAxisStyle()),
                ) {
                }
            }

            assertFailsWith<IllegalArgumentException> {
                XYGraph(
                    xAxisModel = DoubleLinearAxisModel(
                        0.0..2000.0,
                        minorTickCount = 0,
                        minimumMajorTickIncrement = 5000.0,
                    ),
                    yAxisModel = DoubleLinearAxisModel(
                        points.autoScaleYRange(),
                        minorTickCount = 2,
                    ),
                    xAxisContent = AxisContent({}, {}, rememberAxisStyle()),
                    yAxisContent = AxisContent({}, {}, rememberAxisStyle()),
                ) {
                }
            }
        }
    }

    @OptIn(ExperimentalKoalaPlotApi::class)
    @Test
    fun testNoRoomForMinorTicks() {
        composeTestRule.setContent {
            XYGraph(
                xAxisModel = LongLinearAxisModel(
                    0L..10L,
                    minorTickCount = 0,
                    minimumMajorTickIncrement = 2L,
                ),
                // yAxis doesn't have room for 4 minor ticks (default value)
                yAxisModel = LongLinearAxisModel(
                    0L..10L,
                ),
                xAxisContent = AxisContent({}, {}, rememberAxisStyle()),
                yAxisContent = AxisContent({}, {}, rememberAxisStyle()),
            ) {
            }
        }
    }

    @OptIn(ExperimentalKoalaPlotApi::class)
    @Test
    fun testZoomRangeLimit() {
        composeTestRule.setContent {
            XYGraph(
                xAxisModel = LongLinearAxisModel(
                    0L..1L,
                    minorTickCount = 0,
                    minimumMajorTickIncrement = 1L,
                ),
                yAxisModel = LongLinearAxisModel(
                    0L..10L,
                ),
                xAxisContent = AxisContent({}, {}, rememberAxisStyle()),
                yAxisContent = AxisContent({}, {}, rememberAxisStyle()),
            ) {
            }
        }
    }
}
