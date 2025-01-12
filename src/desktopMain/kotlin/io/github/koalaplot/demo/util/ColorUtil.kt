package io.github.koalaplot.demo.util

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

public object ColorUtil {

    private val random = Random(0)
    private const val COLOR_RANGE = 256

    public fun createRandomColor(): Color {
        return Color(random.nextInt(COLOR_RANGE), random.nextInt(COLOR_RANGE), random.nextInt(COLOR_RANGE))
    }
}
