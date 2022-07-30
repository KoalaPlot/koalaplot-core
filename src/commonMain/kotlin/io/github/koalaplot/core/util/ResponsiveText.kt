package io.github.koalaplot.core.util

import androidx.compose.foundation.Canvas
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontLoader
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp

private const val MinTextSize = 10f

/**
 * A Text composable that will auto-scale the font size up and down to fit in the
 * containing element.
 */
@Composable
public fun ResponsiveText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    maxLines: Int = Int.MAX_VALUE,
    ellipsis: Boolean = false
) {
    var lastSize by remember { mutableStateOf(Size(0f, 0f)) }
    var lastTextSize by remember { mutableStateOf(0.0f) }

    val fontLoader = LocalFontLoader.current
    val density = LocalDensity.current

    fun findTextSize(
        constraints: Constraints,
        minTextSize: Float,
        maxTextSize: Float
    ): Float {
        if (maxTextSize - minTextSize < 1) {
            return minTextSize
        }

        val sizeCheck = if (maxTextSize.isInfinite()) {
            // Protect against a tiny minTextSize
            if (minTextSize < 1f) {
                MinTextSize
            } else {
                minTextSize * 2f
            }
        } else {
            (maxTextSize + minTextSize) / 2.0f
        }

        val paragraph = Paragraph(
            text = text,
            style = style.copy(fontSize = sizeCheck.sp),
            maxLines = maxLines,
            width = constraints.maxWidth.toFloat(),
            density = density,
            resourceLoader = fontLoader,
            ellipsis = ellipsis
        )

        return if (paragraph.didExceedMaxLines ||
            paragraph.height > constraints.maxHeight ||
            paragraph.width > constraints.maxWidth
        ) {
            findTextSize(constraints, minTextSize, sizeCheck)
        } else {
            findTextSize(constraints, sizeCheck, maxTextSize)
        }
    }

    Canvas(modifier) {
        val constraints = Constraints.fixed(size.width.toInt(), size.height.toInt())

        lastTextSize = if (size != lastSize) {
            var minTextSize: Float
            var maxTextSize: Float

            if (size.width > lastSize.width && size.height > lastSize.height) {
                minTextSize = lastTextSize
                maxTextSize = Float.POSITIVE_INFINITY
            } else if (size.width < lastSize.width && size.height < lastSize.height) {
                minTextSize = 0f
                maxTextSize = lastTextSize
            } else {
                minTextSize = 0f
                maxTextSize = Float.POSITIVE_INFINITY
            }

            lastSize = size
            findTextSize(constraints, minTextSize, maxTextSize)
        } else {
            lastTextSize
        }

        val paragraph = Paragraph(
            text = text,
            style = style.copy(fontSize = lastTextSize.sp),
            maxLines = maxLines,
            width = constraints.maxWidth.toFloat(),
            density = this,
            resourceLoader = fontLoader,
            ellipsis = ellipsis
        )

        drawIntoCanvas { canvas ->
            paragraph.paint(canvas)
        }
    }
}
