package io.github.koalaplot.core.xygraph

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Suppress("MagicNumber")
class CategoryAxisModelTest {
    @Test
    fun testComputeOffsetFull() {
        val categories = listOf("A", "B", "C")
        val model = CategoryAxisModel(categories, CategoryAxisOffset.Full)

        assertEquals(0.25f, model.computeOffset("A"), 0.001f)
        assertEquals(0.5f, model.computeOffset("B"), 0.001f)
        assertEquals(0.75f, model.computeOffset("C"), 0.001f)
    }

    @Test
    fun testComputeOffsetHalf() {
        val categories = listOf("A", "B", "C")
        val model = CategoryAxisModel(categories, CategoryAxisOffset.Half)

        assertEquals(1f / 6f, model.computeOffset("A"), 0.001f)
        assertEquals(1f / 6f + 1f / 3f, model.computeOffset("B"), 0.001f)
        assertEquals(5f / 6f, model.computeOffset("C"), 0.001f)
    }

    @Test
    fun testComputeOffsetNone() {
        val categories = listOf("A", "B", "C")
        val model = CategoryAxisModel(categories, CategoryAxisOffset.None)

        assertEquals(0.0f, model.computeOffset("A"), 0.001f)
        assertEquals(0.5f, model.computeOffset("B"), 0.001f)
        assertEquals(1.0f, model.computeOffset("C"), 0.001f)
    }

    @Test
    fun testComputeOffsetCustom() {
        val categories = listOf("A", "B", "C")
        val model = CategoryAxisModel(categories, CategoryAxisOffset.Custom(0.25f))

        assertEquals(0.1f, model.computeOffset("A"), 0.001f)
        assertEquals(0.5f, model.computeOffset("B"), 0.001f)
        assertEquals(0.9f, model.computeOffset("C"), 0.001f)
    }

    @Test
    fun testComputeOffsetSingleCategory() {
        val categories = listOf("A")
        val modelFull = CategoryAxisModel(categories, CategoryAxisOffset.Full)
        assertEquals(0.5f, modelFull.computeOffset("A"), 0.001f)

        val modelNone = CategoryAxisModel(categories, CategoryAxisOffset.None)
        assertEquals(0.0f, modelNone.computeOffset("A"), 0.001f)
    }

    @Test
    fun testComputeOffsetThrows() {
        val categories = listOf("A", "B", "C")
        val model = CategoryAxisModel(categories)

        assertFailsWith<IllegalArgumentException> {
            model.computeOffset("D")
        }
    }
}
