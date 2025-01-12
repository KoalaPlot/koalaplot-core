package io.github.koalaplot.demo.util

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

public object ColorUtil {

    private val random = Random(0)

    public fun createRandomColor(): Color {
        return Color(random.nextInt(256), random.nextInt(256), random.nextInt(256))
    }
}
