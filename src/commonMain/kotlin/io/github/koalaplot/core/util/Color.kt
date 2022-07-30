package io.github.koalaplot.core.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ExperimentalGraphicsApi

/**
 * ColorPaletteGenerator will create [count] colors.
 */
public typealias ColorPaletteGenerator = (count: Int) -> List<Color>

/**
 * Will generate a color palette that varies the hue in even increments to obtain
 * [count] colors. It uses the HSL model with the ability to separately specify
 * the [saturation] and [lightness] of the palette colors.
 */
@OptIn(ExperimentalGraphicsApi::class)
public fun generateHueColorPalette(
    count: Int,
    saturation: Float = 0.5f,
    lightness: Float = 0.5f
): List<Color> {
    val delta = (DegreesFullCircle / count)

    return buildList {
        for (i in 0..count) {
            add(Color.hsl((delta * i), saturation, lightness))
        }
    }
}
