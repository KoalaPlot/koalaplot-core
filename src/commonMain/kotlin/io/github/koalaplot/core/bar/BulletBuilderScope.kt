package io.github.koalaplot.core.bar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.koalaplot.core.DiamondShape
import io.github.koalaplot.core.Symbol
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.util.max
import io.github.koalaplot.core.util.min
import io.github.koalaplot.core.xychart.AxisStyle
import io.github.koalaplot.core.xychart.LinearAxisModel
import io.github.koalaplot.core.xychart.rememberAxisStyle

private const val DefaultSizeFraction = 0.75f
private const val FeaturedMeasureDefaultSize = 0.33f

/**
 * A scope for configuring a bullet graph.
 */
@ExperimentalKoalaPlotApi
@BulletGraphDslMarker
public class BulletBuilderScope {
    internal data class ComparativeMeasure(val value: Float, val indicator: @Composable () -> Unit)
    internal data class FeaturedMeasure(
        val value: Float,
        val type: FeaturedMeasureType,
        val indicator: @Composable () -> Unit
    )

    internal val comparativeMeasures: MutableList<ComparativeMeasure> = mutableListOf()
    internal var featuredMeasure: FeaturedMeasure = FeaturedMeasure(0f, FeaturedMeasureType.BAR) {}
    internal var rangesScope: RangesScope = RangesScope()

    /**
     * Content for the bullet graph's label.
     */
    public val label: Slot<@Composable () -> Unit> = Slot() {}

    /**
     * Sets the label's width. If the BulletGraph is in a layout of multiple bullet graphs this value will be
     * ignored and instead will take the value specified in [BulletGraphScope.labelWidth].
     */
    public var labelWidth: LabelWidth = FixedFraction(0.25f)

    /**
     * Specifies the axis settings and content. See [AxisSettings].
     */
    public val axis: Slot<AxisSettings.() -> Unit> = Slot {}

    /**
     * Sets the comparative measure value and the composable used to draw the indicator. The composable
     * will be centered on the value with a width and height equal to the height of the
     * BulletGraph background fill area.
     */
    public fun comparativeMeasure(value: Float, indicator: @Composable () -> Unit = { LineIndicator() }) {
        comparativeMeasures += ComparativeMeasure(value, indicator)
    }

    /**
     * Sets the feature measure value and indicator as a bar.
     *
     * @param value The value of the featured measure
     * @param indicator The composable used to render the featured measure. For a bar, the
     * width of the composable represents the extent of the featured measure from 0 to value.
     */
    public fun featuredMeasureBar(
        value: Float,
        indicator: @Composable () -> Unit = {
            HorizontalBarIndicator(SolidColor(MaterialTheme.colors.primary), fraction = FeaturedMeasureDefaultSize)
        }
    ) {
        featuredMeasure = FeaturedMeasure(value, FeaturedMeasureType.BAR, indicator)
    }

    /**
     * Sets the feature measure value and indicator as a symbol.
     *
     * @param value The value of the featured measure
     * @param indicator The composable used to render the featured measure. For a
     * symbol, the composable is centered on value with a height and width equal to the
     * height of the BulletGraph background fill area.
     */
    public fun featuredMeasureSymbol(
        value: Float,
        indicator: @Composable () -> Unit = { DiamondIndicator() }
    ) {
        featuredMeasure = FeaturedMeasure(value, FeaturedMeasureType.SYMBOL, indicator)
    }

    internal enum class FeaturedMeasureType {
        BAR, SYMBOL
    }

    /**
     * Defines the qualitative ranges starting at the specified [start] value. See [RangesScope] for how to
     * configure individual range values and their graphical representation. If the default visual representation is
     * desired then the overloaded version of this function that takes only a variable number of Float values may be
     * used instead.
     */
    public fun ranges(start: Float = 0f, block: RangesScope.() -> Unit) {
        rangesScope = RangesScope().apply {
            range(start)
            block()
        }
    }

    /**
     * Defines the qualitative ranges using the default visual representation. If the visual representation needs
     * to be customized then use the overloaded version of this function.
     */
    public fun ranges(vararg values: Float) {
        rangesScope = RangesScope().apply {
            values.forEach {
                ranges.add(Range(it, null))
            }
        }
    }

    /**
     * A scope for setting range boundaries. See [range] for specifying a specific range value and its graphical
     * representation.
     */
    public class RangesScope {
        internal val ranges: MutableList<Range> = mutableListOf()

        /**
         * Defines a new qualitative range that begins at the end of the previous range's value
         * and extends until [endValue]. The default indicator will be drawn as a shaded bar
         * depending on this range's position among all ranges and the number of ranges, but can
         * be overridden by providing a non-null value for [indicator]. The shading is a percentage of
         * the material theme primary color for the following numbers of ranges:
         * One: 35%
         * Two: 35% and 10%
         * Three: 40%, 25%, and 10%
         * Four: 50%, 35%, 20%, and 10%
         * Five: 50%, 35%, 20%, 10%, and 3%
         * Sixth and higher ranges are shaded at 1%
         */
        public fun range(endValue: Float, indicator: (@Composable () -> Unit)? = null) {
            ranges.add(Range(endValue, indicator))
        }
    }

    internal data class Range(val value: Float, val indicator: (@Composable () -> Unit)? = null)
}

@OptIn(ExperimentalKoalaPlotApi::class)
internal fun SubcomposeMeasureScope.measureLabel(
    bulletScope: BulletBuilderScope,
    constraints: Constraints
): Placeable {
    return with(bulletScope) {
        val labelMeasurable = subcompose(label) { Box { label.value() } }[0]
        labelMeasurable.measure(constraints)
    }
}

/**
 * A diamond-shaped indicator that may be used as a feature marker.
 * @param color The color for the marker
 * @param size The size for the marker in Dp
 */
@ExperimentalKoalaPlotApi
@Composable
public fun DiamondIndicator(color: Color = MaterialTheme.colors.primary, size: Dp) {
    Symbol(shape = DiamondShape, size = size, fillBrush = SolidColor(color))
}

/**
 * A diamond-shaped indicator that may be used as a feature marker.
 * @param color The color for the marker
 * @param sizeFraction The size for the marker as a fraction of the bullet graph height.
 */
@ExperimentalKoalaPlotApi
@Composable
public fun DiamondIndicator(color: Color = MaterialTheme.colors.primary, sizeFraction: Float = DefaultSizeFraction) {
    Symbol(shape = DiamondShape, sizeFraction = sizeFraction, fillBrush = SolidColor(color))
}

/**
 * A line for comparative measure indicators.
 * @param heightFraction The fraction of the overall bullet graph bar area height for the extent of the line
 * @param width The thickness of the line, defaults to 2.dp
 */
@ExperimentalKoalaPlotApi
@Composable
public fun LineIndicator(
    color: Color = MaterialTheme.colors.primary,
    heightFraction: Float = DefaultSizeFraction,
    width: Dp = 2.dp
) {
    val shape = LineShape(heightFraction, width)
    Box(
        modifier = Modifier.fillMaxSize().drawBehind {
            val outline = shape.createOutline(this.size, layoutDirection, this)
            drawOutline(outline, SolidColor(color), style = Fill)
        }.clip(shape)
    )
}

/**
 * A default implementation of a bar indicator that can be used for the featured measure or qualitative ranges.
 * @param fraction The fraction of the overall bullet graph background height the indicator will occupy.
 * @param brush The brush to paint the bar with
 * @param shape An optional shape for the bar.
 * @param border An optional border for the bar.
 */
@ExperimentalKoalaPlotApi
@Composable
public fun HorizontalBarIndicator(
    brush: Brush,
    modifier: Modifier = Modifier,
    fraction: Float = 1f,
    shape: Shape = RectangleShape,
    border: BorderStroke? = null
) {
    Box(
        modifier = modifier.fillMaxWidth().fillMaxHeight(fraction)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .background(brush = brush, shape = shape)
            .clip(shape)
    )
}

/**
 * A line shape for comparative measure indicators.
 * @param heightFraction The fraction of the overall shape height for the extent of the line
 * @param width The fraction of the overall shape width for the thickness of the line
 */
private class LineShape(val heightFraction: Float, val width: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        Outline.Generic(
            Path().apply {
                val widthPx = with(density) {
                    if (width == Dp.Hairline) {
                        1
                    } else {
                        width.roundToPx()
                    }
                }
                val vMargin = (size.height - size.height * heightFraction) / 2f
                val hMargin = (size.width - widthPx) / 2f

                moveTo(hMargin, vMargin)
                lineTo(size.width - hMargin, vMargin)
                lineTo(size.width - hMargin, size.height - vMargin)
                lineTo(hMargin, size.height - vMargin)
                lineTo(hMargin, vMargin)
            }
        )

    override fun toString(): String = "LineShape"
}

/**
 * Sealed class hierarchy to designate the label area width type and values.
 */
@ExperimentalKoalaPlotApi
public sealed class LabelWidth

/**
 * Set the label width as a fixed [fraction] of the overall graph width. If the label is smaller
 * than the fixed fraction it will be right-justified to the graph.
 */
@ExperimentalKoalaPlotApi
public class FixedFraction(internal val fraction: Float) : LabelWidth()

/**
 * Set the label width as a fixed [size] in Dp. If the label is smaller than the size it will be
 * right-justified to the graph.
 */
@ExperimentalKoalaPlotApi
public class Fixed(internal val size: Dp) : LabelWidth()

/**
 * Set the label width as a variable [fraction] of the overall graph width. If the label is smaller
 * than the fraction the graph will grow to occupy the available space.
 */
@ExperimentalKoalaPlotApi
public class VariableFraction(internal val fraction: Float) : LabelWidth()

/**
 * A slot to hold a configurable value in the bullet graph dsl.
 * @param default The default value held by the Slot
 * @param T The type of the value held by the Slot
 */
@ExperimentalKoalaPlotApi
@BulletGraphDslMarker
public open class Slot<T>(default: T) {
    internal var value: T = default

    /**
     * Sets the Slot's value.
     * @param setValue The value to set.
     */
    public operator fun invoke(setValue: T) {
        value = setValue
    }
}

/**
 * Specifies the settings for an Axis.
 */
@ExperimentalKoalaPlotApi
@BulletGraphDslMarker
public class AxisSettings {
    /**
     * Sets a LinearAxisModel to use for the graph. If not provided or set as null, a LinearAxisModel will
     * be created based on the values of the comparativeMeasures, featuredMeasure,
     * and ranges.
     */
    public var model: LinearAxisModel? = null

    /**
     * Content for the bullet graph's axis labels.
     */
    public val labels: Slot<@Composable (Float) -> Unit> = Slot {}

    /**
     * Shortcut to call the label Composable stored in the labels Slot.
     */
    @Composable
    internal infix fun label(f: Float) {
        labels.value(f)
    }

    /**
     * Optional styling for the bullet graph axis. If not provided, default styling will be used.
     */
    public var style: AxisStyle? = null

    internal companion object {
        @Composable
        fun fromScope(bulletScope: BulletBuilderScope): AxisSettings {
            val settings = AxisSettings().apply(bulletScope.axis.value)
            settings.model = settings.model ?: createLinearAxisModel(bulletScope)
            settings.style = settings.style ?: rememberAxisStyle()
            return settings
        }

        private fun createLinearAxisModel(builtScope: BulletBuilderScope): LinearAxisModel {
            // Determine min/max range for axis
            val minRange = min(
                builtScope.rangesScope.ranges.minOfOrNull { it.value } ?: Float.POSITIVE_INFINITY,
                builtScope.featuredMeasure.value,
                builtScope.comparativeMeasures.minOfOrNull { it.value } ?: Float.POSITIVE_INFINITY
            )
            val maxRange = max(
                builtScope.rangesScope.ranges.maxOfOrNull { it.value } ?: Float.NEGATIVE_INFINITY,
                builtScope.featuredMeasure.value,
                builtScope.comparativeMeasures.maxOfOrNull { it.value } ?: Float.NEGATIVE_INFINITY
            )

            val range = if (minRange == maxRange) {
                if (minRange != 0f) {
                    (minRange / 2f)..(maxRange * 2f)
                } else {
                    minRange..minRange + 1f
                }
            } else {
                minRange..maxRange
            }

            val axisModel = LinearAxisModel(range, allowZooming = false, allowPanning = false)
            return axisModel
        }
    }
}

@DslMarker
public annotation class BulletGraphDslMarker
