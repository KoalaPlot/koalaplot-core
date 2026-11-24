package io.github.koalaplot.core.bar

import androidx.compose.animation.core.snap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import io.github.koalaplot.core.animation.StartAnimationUseCase
import io.github.koalaplot.core.xygraph.CategoryAxisModel
import io.github.koalaplot.core.xygraph.FloatLinearAxisModel
import kotlin.test.Test
import kotlin.test.assertEquals

private val itemCategories = listOf("A", "B", "C")

/**
 * Checks that bar plots pass the index of the data point to their bar composables, so that a bar set for a single
 * item of a bar plot DSL is used for exactly that item.
 */
@OptIn(ExperimentalTestApi::class)
class BarPlotItemBarTest {
    @Test
    fun verticalBarPlotUsesItemBarOnlyForItsItem() {
        val bars = composeBars { recorder ->
            with(TestGraphScope(CategoryAxisModel(itemCategories), FloatLinearAxisModel(0f..10f))) {
                VerticalBarPlot(defaultBar = recorder.verticalBar("default"), startAnimationUseCase = noAnimation()) {
                    item("A", 0f, 3f)
                    item("B", 0f, 4f, recorder.verticalBar("item"))
                    item("C", 0f, 5f)
                }
            }
        }

        assertEquals(setOf("default" to "A", "item" to "B", "default" to "C"), bars)
    }

    @Test
    fun horizontalBarPlotUsesItemBarOnlyForItsItem() {
        val bars = composeBars { recorder ->
            with(TestGraphScope(FloatLinearAxisModel(0f..10f), CategoryAxisModel(itemCategories))) {
                HorizontalBarPlot(defaultBar = recorder.horizontalBar("default"), startAnimationUseCase = noAnimation()) {
                    item("A", 0f, 3f)
                    item("B", 0f, 4f, recorder.horizontalBar("item"))
                    item("C", 0f, 5f)
                }
            }
        }

        assertEquals(setOf("default" to "A", "item" to "B", "default" to "C"), bars)
    }

    @Test
    fun stackedVerticalBarPlotPassesDataIndex() {
        val data = itemCategories.map { DefaultVerticalBarPlotStackedPointEntry(it, 0f, listOf(1f, 3f)) }
        val bars = composeBars { recorder ->
            with(TestGraphScope(CategoryAxisModel(itemCategories), FloatLinearAxisModel(0f..10f))) {
                StackedVerticalBarPlot(
                    data,
                    bar = { xIndex, barIndex, value -> recorder.Record("$xIndex/$barIndex", value.x) },
                    startAnimationUseCase = noAnimation(),
                )
            }
        }

        assertEquals(
            setOf("0/0" to "A", "0/1" to "A", "1/0" to "B", "1/1" to "B", "2/0" to "C", "2/1" to "C"),
            bars,
        )
    }

    @Test
    fun stackedHorizontalBarPlotPassesDataIndex() {
        val data = itemCategories.map { DefaultHorizontalBarPlotStackedPointEntry(it, 0f, listOf(1f, 3f)) }
        val bars = composeBars { recorder ->
            with(TestGraphScope(FloatLinearAxisModel(0f..10f), CategoryAxisModel(itemCategories))) {
                StackedHorizontalBarPlot(
                    data,
                    bar = { yIndex, barIndex, value -> recorder.Record("$yIndex/$barIndex", value.y) },
                    startAnimationUseCase = noAnimation(),
                )
            }
        }

        assertEquals(
            setOf("0/0" to "A", "0/1" to "A", "1/0" to "B", "1/1" to "B", "2/0" to "C", "2/1" to "C"),
            bars,
        )
    }

    @Test
    fun stackedVerticalBarPlotUsesItemBarOnlyForItsItem() {
        val bars = composeBars { recorder ->
            with(TestGraphScope(CategoryAxisModel(itemCategories), FloatLinearAxisModel(0f..10f))) {
                StackedVerticalBarPlot(startAnimationUseCase = noAnimation()) {
                    series(recorder.verticalBar("lower")) {
                        itemCategories.forEach { item(it, 1f) }
                    }
                    series(recorder.verticalBar("upper")) {
                        item("A", 2f)
                        item("B", 2f, recorder.verticalBar("item"))
                        item("C", 2f)
                    }
                }
            }
        }

        assertEquals(
            setOf("lower" to "A", "lower" to "B", "lower" to "C", "upper" to "A", "item" to "B", "upper" to "C"),
            bars,
        )
    }

    @Test
    fun stackedHorizontalBarPlotUsesItemBarOnlyForItsItem() {
        val bars = composeBars { recorder ->
            with(TestGraphScope(FloatLinearAxisModel(0f..10f), CategoryAxisModel(itemCategories))) {
                StackedHorizontalBarPlot(startAnimationUseCase = noAnimation()) {
                    series(recorder.horizontalBar("lower")) {
                        itemCategories.forEach { item(1f, it) }
                    }
                    series(recorder.horizontalBar("upper")) {
                        item(2f, "A")
                        item(2f, "B", recorder.horizontalBar("item"))
                        item(2f, "C")
                    }
                }
            }
        }

        assertEquals(
            setOf("lower" to "A", "lower" to "B", "lower" to "C", "upper" to "A", "item" to "B", "upper" to "C"),
            bars,
        )
    }

    @Test
    fun stackedVerticalBarPlotAllowsSeriesWithoutItemForCategory() {
        val bars = composeBars { recorder ->
            with(TestGraphScope(CategoryAxisModel(itemCategories), FloatLinearAxisModel(0f..10f))) {
                StackedVerticalBarPlot(startAnimationUseCase = noAnimation()) {
                    series(recorder.verticalBar("lower")) {
                        itemCategories.forEach { item(it, 1f) }
                    }
                    // The stack of the first category only holds the lower series
                    series(recorder.verticalBar("upper")) {
                        item("B", 2f)
                        item("C", 2f)
                    }
                }
            }
        }

        assertEquals(setOf("lower" to "A", "lower" to "B", "lower" to "C", "upper" to "B", "upper" to "C"), bars)
    }

    @Test
    fun stackedHorizontalBarPlotAllowsSeriesWithoutItemForCategory() {
        val bars = composeBars { recorder ->
            with(TestGraphScope(FloatLinearAxisModel(0f..10f), CategoryAxisModel(itemCategories))) {
                StackedHorizontalBarPlot(startAnimationUseCase = noAnimation()) {
                    series(recorder.horizontalBar("lower")) {
                        itemCategories.forEach { item(1f, it) }
                    }
                    // The stack of the first category only holds the lower series
                    series(recorder.horizontalBar("upper")) {
                        item(2f, "B")
                        item(2f, "C")
                    }
                }
            }
        }

        assertEquals(setOf("lower" to "A", "lower" to "B", "lower" to "C", "upper" to "B", "upper" to "C"), bars)
    }

    private fun composeBars(content: @Composable (BarRecorder) -> Unit): Set<Pair<String, Any?>> {
        val recorder = BarRecorder()
        runDesktopComposeUiTest(width = 300, height = 200) {
            setContent { content(recorder) }
            waitForIdle()
        }
        return recorder.bars
    }

    private fun noAnimation() = StartAnimationUseCase(StartAnimationUseCase.ExecutionType.None, snap())
}

/**
 * Collects which bar composable was used for which category.
 */
private class BarRecorder {
    val bars = mutableSetOf<Pair<String, Any?>>()

    @Composable
    fun Record(
        name: String,
        category: Any?,
    ) {
        SideEffect { bars.add(name to category) }
    }

    fun <X, Y> verticalBar(name: String): DefaultVerticalBarComposable<X, Y> = { _, _, value -> Record(name, value.x) }

    fun <X, Y> horizontalBar(name: String): DefaultHorizontalBarComposable<X, Y> = { _, _, value -> Record(name, value.y) }
}
