package io.github.koalaplot.core.bar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.koalaplot.core.DiamondShape
import io.github.koalaplot.core.Symbol
import io.github.koalaplot.core.style.KoalaPlotTheme
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.Axis
import io.github.koalaplot.core.xygraph.AxisDelegate
import io.github.koalaplot.core.xygraph.AxisStyle
import io.github.koalaplot.core.xygraph.LinearAxisModel
import io.github.koalaplot.core.xygraph.rememberAxisStyle
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val DefaultSizeFraction = 0.75f
private const val FeaturedMeasureDefaultSize = 0.33f

private val DefaultRangeShades = listOf(
    listOf(0.65f),
    listOf(0.65f, 0.9f),
    listOf(0.6f, 0.75f, 0.9f),
    listOf(0.5f, 0.65f, 0.8f, 0.9f),
    listOf(0.5f, 0.65f, 0.8f, 0.9f, 0.97f),
)

// Max number of qualitative ranges supported by the DefaultRangeShades above
private const val MaxQualitativeRanges = 5

private const val MinRangeShade = 0.99f

/**
 * A scope for configuring a bullet graph.
 *
 * @param axisModel Sets the LinearAxisModel to use for the graph.
 *
 */
@ExperimentalKoalaPlotApi
@BulletGraphDslMarker
public class BulletBuilderScope<T>(
    private val axisModel: LinearAxisModel<T>,
) where T : Comparable<T>, T : Number {
    internal class ComparativeMeasure<T>(
        val value: T,
        val indicator: @Composable () -> Unit,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as ComparativeMeasure<*>

            if (value != other.value) return false
            if (indicator != other.indicator) return false

            return true
        }

        override fun hashCode(): Int {
            var result = value.hashCode()
            result = 31 * result + indicator.hashCode()
            return result
        }
    }

    internal data class FeaturedMeasure<T>(
        val value: T,
        val type: FeaturedMeasureType,
        val indicator: @Composable () -> Unit,
    ) where T : Comparable<T>, T : Number

    internal val comparativeMeasures: MutableList<ComparativeMeasure<T>> = mutableListOf()
    internal var featuredMeasure: FeaturedMeasure<T>? = null
    internal var rangesScope: RangesScope<T> = RangesScope<T>()

    /**
     * Content for the bullet graph's label.
     */
    public val label: Slot<@Composable () -> Unit> = Slot {}

    /**
     * Specifies the axis settings and content. See [AxisSettings].
     */
    public val axis: Slot<AxisSettings<T>.() -> Unit> = Slot {}

    /**
     * Sets the comparative measure value and the composable used to draw the indicator. The composable
     * will be centered on the value with a width and height equal to the height of the
     * BulletGraph background fill area.
     */
    public fun comparativeMeasure(
        value: T,
        indicator: @Composable () -> Unit = { LineIndicator() },
    ) {
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
        value: T,
        indicator: @Composable () -> Unit = {
            HorizontalBarIndicator(SolidColor(MaterialTheme.colorScheme.primary), fraction = FeaturedMeasureDefaultSize)
        },
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
        value: T,
        indicator: @Composable () -> Unit = { DiamondIndicator() },
    ) {
        featuredMeasure = FeaturedMeasure(value, FeaturedMeasureType.SYMBOL, indicator)
    }

    internal enum class FeaturedMeasureType {
        BAR,
        SYMBOL,
    }

    /**
     * Defines the qualitative ranges starting at the specified [start] value. See [RangesScope] for how to
     * configure individual range values and their graphical representation. If the default visual representation is
     * desired then the overloaded version of this function that takes only a variable number of Float values may be
     * used instead.
     */
    public fun ranges(
        start: T,
        block: RangesScope<T>.() -> Unit,
    ) {
        rangesScope = RangesScope<T>().apply {
            range(start)
            block()
        }
    }

    /**
     * Defines the qualitative ranges using the default visual representation. If the visual representation needs
     * to be customized then use the overloaded version of this function.
     */
    public fun ranges(vararg values: T) {
        // Subtract 1 because first value is the start of first range, not a range itself
        require(values.size - 1 <= MaxQualitativeRanges) {
            "A maximum of $MaxQualitativeRanges qualitative ranges is supported by this function. If more are " +
                "required, use the overloaded version of this function."
        }
        rangesScope = RangesScope<T>().apply {
            values.forEach {
                ranges.add(Range<T>(it, null))
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BulletBuilderScope<*>

        if (comparativeMeasures != other.comparativeMeasures) return false
        if (featuredMeasure != other.featuredMeasure) return false
        if (rangesScope != other.rangesScope) return false
        if (label != other.label) return false
        return axis == other.axis
    }

    override fun hashCode(): Int {
        var result = comparativeMeasures.hashCode()
        result = 31 * result + featuredMeasure.hashCode()
        result = 31 * result + rangesScope.hashCode()
        result = 31 * result + label.hashCode()
        result = 31 * result + axis.hashCode()
        return result
    }

    /**
     * A scope for setting range boundaries. See [range] for specifying a specific range value and its graphical
     * representation.
     */
    public class RangesScope<T> {
        internal val ranges: MutableList<Range<T>> = mutableListOf()

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
        public fun range(
            endValue: T,
            indicator: (@Composable () -> Unit)? = null,
        ) {
            ranges.add(Range(endValue, indicator))
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as RangesScope<*>

            if (ranges != other.ranges) return false

            return true
        }

        override fun hashCode(): Int = ranges.hashCode()
    }

    internal data class Range<T>(
        val value: T,
        val indicator: (@Composable () -> Unit)? = null,
    )

    private enum class Slots {
        AXIS_LABELS,
        AXIS,
        RANGES,
        FEATURE,
        COMPARATIVE,
    }

    @Composable
    internal fun createBulletGraphBuilder(slotId: Int): BulletGraphBuilder = BulletGraphBuilder(
        slotId,
        AxisSettings.fromScope(this),
    )

    internal inner class BulletGraphBuilder
        @OptIn(ExperimentalKoalaPlotApi::class)
        constructor(
            private val slotId: Int,
            private val axisSettings: AxisSettings<T>,
        ) {
            internal var bulletHeight: Int = 0

            var labelPlaceable: Placeable? = null
            var axis: AxisDelegate<T>? = null
            var axisLabelPlaceables: List<Placeable>? = null
            private var axisHeight: Int? = null
            private var axisPlaceable: Placeable? = null
            private val axisLabelsHeight: Int by lazy {
                requireNotNull(axisLabelPlaceables)
                axisLabelPlaceables?.let { placeables ->
                    placeables.maxOfOrNull { it.height }
                } ?: 0
            }
            private val rangeHeight: Int by lazy {
                requireNotNull(axisHeight) { "axisHeight must not be null in order to calculate rangeHeight" }
                (bulletHeight - axisHeight!! - axisLabelsHeight).coerceAtLeast(0)
            }
            private var rangePlaceables: List<Placeable>? = null
            private var featurePlaceable: Placeable? = null
            private var comparativeMeasurePlaceables: List<Placeable>? = null

            @OptIn(ExperimentalKoalaPlotApi::class)
            fun measureLabel(
                scope: SubcomposeMeasureScope,
                labelWidthMaxConstraint: Int,
            ) {
                labelPlaceable = scope.measureLabel(
                    this@BulletBuilderScope,
                    Constraints(maxWidth = labelWidthMaxConstraint, maxHeight = bulletHeight),
                )
            }

            private fun slotId(slot: Slots): String = "$slotId.${slot.name}"

            /**
             * @param width of the non-label graph area: constraints.maxWidth - labelWidth
             */
            @OptIn(ExperimentalKoalaPlotApi::class)
            fun measureAxisLabels(
                scope: SubcomposeMeasureScope,
                width: Int,
            ) {
                with(scope) {
                    val axis = AxisDelegate.createHorizontalAxis(
                        this@BulletBuilderScope.axisModel,
                        axisSettings.style!!,
                        width.toDp(),
                    )
                    axisHeight = (axis.thicknessDp - axis.axisOffset).toPx().roundToInt()

                    val measurable = subcompose(slotId(Slots.AXIS_LABELS)) {
                        axis.majorTickValues.forEach { Box { axisSettings.Label(it) } }
                    }
                    axisLabelPlaceables = measurable.map {
                        it.measure(
                            Constraints(
                                maxWidth =
                                    (width / axis.majorTickValues.size.coerceAtLeast(1)).coerceAtLeast(0),
                                maxHeight = bulletHeight,
                            ),
                        )
                    }

                    this@BulletGraphBuilder.axis = axis
                }
            }

            /**
             * @param rangeWidth Width of the axis
             */
            fun measureAxis(
                scope: SubcomposeMeasureScope,
                rangeWidth: Int,
            ) {
                requireNotNull(axis) { "axis must not be null" }
                axisPlaceable =
                    scope.subcompose(slotId(Slots.AXIS)) { Axis(axis!!) }[0].measure(Constraints.fixedWidth(rangeWidth))
            }

            @OptIn(ExperimentalKoalaPlotApi::class)
            fun measureRanges(
                scope: SubcomposeMeasureScope,
                rangeWidth: Int,
            ) {
                val rangeMeasurables: List<Measurable> =
                    scope.subcompose(slotId(Slots.RANGES)) {
                        // RangeIndicators(this@BulletBuilderScope)
                        val builtScope = this@BulletBuilderScope
                        for (rangeIndex in 1 until builtScope.rangesScope.ranges.size) {
                            val range = builtScope.rangesScope.ranges[rangeIndex]
                            if (range.indicator == null) {
                                val shadeIndex = min(builtScope.rangesScope.ranges.size - 2, DefaultRangeShades.lastIndex)
                                    .coerceIn(0..DefaultRangeShades.lastIndex)
                                val numShades = DefaultRangeShades[shadeIndex].size
                                val shade = if (rangeIndex - 1 >= numShades) {
                                    MinRangeShade
                                } else {
                                    DefaultRangeShades[shadeIndex][rangeIndex - 1]
                                }
                                HorizontalBarIndicator(SolidColor(Color(shade, shade, shade)))
                            } else {
                                Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize()) {
                                    range.indicator.invoke()
                                }
                            }
                        }
                    }

                val ranges = this@BulletBuilderScope.rangesScope.ranges
                rangePlaceables = rangeMeasurables.mapIndexed { rangeIndex, measurable ->
                    val maxWidth = (
                        (rangeWidth * axisModel.computeOffset(ranges[rangeIndex + 1].value)).roundToInt() -
                            (rangeWidth * axisModel.computeOffset(ranges[rangeIndex].value)).roundToInt()
                    ).absoluteValue
                    measurable.measure(Constraints.fixed(maxWidth, rangeHeight))
                }
            }

//            @OptIn(ExperimentalKoalaPlotApi::class)
//            @Composable
//            private fun <T> RangeIndicators(builtScope: BulletBuilderScope<T>) where T : Comparable<T>, T : Number {
//                for (rangeIndex in 1 until builtScope.rangesScope.ranges.size) {
//                    val range = builtScope.rangesScope.ranges[rangeIndex]
//                    if (range.indicator == null) {
//                        val shadeIndex = min(builtScope.rangesScope.ranges.size - 2, DefaultRangeShades.lastIndex)
//                            .coerceIn(0..DefaultRangeShades.lastIndex)
//                        val numShades = DefaultRangeShades[shadeIndex].size
//                        val shade = if (rangeIndex - 1 >= numShades) {
//                            MinRangeShade
//                        } else {
//                            DefaultRangeShades[shadeIndex][rangeIndex - 1]
//                        }
//                        HorizontalBarIndicator(SolidColor(Color(shade, shade, shade)))
//                    } else {
//                        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize()) {
//                            range.indicator.invoke()
//                        }
//                    }
//                }
//            }

            @OptIn(ExperimentalKoalaPlotApi::class)
            fun measureFeature(
                scope: SubcomposeMeasureScope,
                rangeWidth: Int,
                animationSpec: AnimationSpec<Float>,
            ) {
                this@BulletBuilderScope.featuredMeasure?.let { featuredMeasure ->
                    val origin = computeFeatureBarOrigin(featuredMeasure.value, axisModel.range)
                    val featureWidth = rangeWidth * abs(
                        axisModel.computeOffset(featuredMeasure.value) - axisModel.computeOffset(
                            origin,
                        ),
                    )

                    val measurable = scope.subcompose(slotId(Slots.FEATURE)) {
                        // Animation scale factor
                        val beta = remember(featuredMeasure) { Animatable(0f) }
                        LaunchedEffect(featuredMeasure) {
                            beta.animateTo(1f, animationSpec = animationSpec)
                        }

                        with(scope) {
                            val sizeModifier =
                                if (featuredMeasure.type == FeaturedMeasureType.BAR) {
                                    Modifier.size((beta.value * featureWidth).toDp(), rangeHeight.toDp())
                                } else {
                                    Modifier.size(rangeHeight.toDp(), rangeHeight.toDp())
                                }

                            Box(contentAlignment = Alignment.Center, modifier = sizeModifier) {
                                featuredMeasure.indicator()
                            }
                        }
                    }[0]

                    featurePlaceable = measurable.measure(Constraints(maxWidth = rangeWidth, maxHeight = rangeHeight))
                }
            }

            @OptIn(ExperimentalKoalaPlotApi::class)
            fun measureComparativeMeasures(scope: SubcomposeMeasureScope) {
                val measurables: List<Measurable> =
                    scope.subcompose(slotId(Slots.COMPARATIVE)) {
                        this@BulletBuilderScope.comparativeMeasures.forEach {
                            Box(contentAlignment = Alignment.Center) { it.indicator() }
                        }
                    }

                comparativeMeasurePlaceables = measurables.map {
                    it.measure(Constraints.fixed(rangeHeight, rangeHeight))
                }
            }

            @OptIn(ExperimentalKoalaPlotApi::class)
            fun Placeable.PlacementScope.layout(
                yPos: Int,
                labelWidth: Int,
                firstAxisLabelWidth: Int,
                rangeWidth: Int,
            ) {
                requireNotNull(axis) { "axis must not be null during layout" }
                requireNotNull(axisLabelPlaceables) { "axis label placeables must not be null during layout" }
                requireNotNull(labelPlaceable) { "labelPlacesable must not be null during layout" }
                requireNotNull(axisPlaceable) { "axisPlaceable must not be null during layout" }
                requireNotNull(rangePlaceables) { "rangePlaceables must not be null during layout" }
                requireNotNull(comparativeMeasurePlaceables) {
                    "comparativeMeasurePlaceables must not be null during layout"
                }

                val axis = axis!!
                val axisLabelPlaceables = axisLabelPlaceables!!
                val labelPlaceable = labelPlaceable!!
                val axisPlaceable = axisPlaceable!!
                val rangePlaceables = rangePlaceables!!
                val comparativeMeasurePlaceables = comparativeMeasurePlaceables!!

                val rangeStart = labelWidth + firstAxisLabelWidth / 2

                labelPlaceable.place(
                    labelWidth - labelPlaceable.width,
                    (yPos + bulletHeight / 2 - labelPlaceable.height / 2).coerceAtLeast(0),
                )

                axisPlaceable.place(rangeStart, yPos + rangeHeight)

                // place axis labels
                axis.majorTickValues.forEachIndexed { index, fl ->
                    axisLabelPlaceables[index].place(
                        (
                            rangeStart + rangeWidth * axisModel.computeOffset(fl) - axisLabelPlaceables[index].width / 2
                        ).roundToInt(),
                        yPos + (bulletHeight - axisLabelsHeight).coerceAtLeast(0),
                    )
                }

                // place range indicators
                rangePlaceables.forEachIndexed { index, placeable ->
                    val xPos =
                        rangeStart +
                            rangeWidth * axisModel.computeOffset(this@BulletBuilderScope.rangesScope.ranges[index].value)
                    placeable.place(xPos.roundToInt(), yPos)
                }

                // place comparative measures
                comparativeMeasurePlaceables.forEachIndexed { index, placeable ->
                    val xPos =
                        rangeStart +
                            rangeWidth * axisModel.computeOffset(this@BulletBuilderScope.comparativeMeasures[index].value) -
                            placeable.width / 2
                    placeable.place(xPos.roundToInt(), yPos)
                }

                // place feature
                featurePlaceable?.let {
                    val value = this@BulletBuilderScope.featuredMeasure!!.value
                    val xPos = rangeStart + if (this@BulletBuilderScope.featuredMeasure!!.type == FeaturedMeasureType.BAR) {
                        val origin = computeFeatureBarOrigin(value, axisModel.range)
                        if (value.toDouble() < 0) {
                            rangeWidth * axisModel.computeOffset(origin) - it.width
                        } else {
                            rangeWidth * axisModel.computeOffset(origin)
                        }
                    } else {
                        rangeWidth * axisModel.computeOffset(value) - it.width / 2
                    }
                    it.place(xPos.roundToInt(), yPos)
                }
            }

            /**
             * Technically per the BulletGraph spec a bar is not to be used if the origin of the axis does not include
             * 0, but this will force the feature bar to have an origin of 0 or a bound of the axis in order to display
             * in a reasonable way.
             * @param featuredMeasureValue The value of the featured measure
             * @param axisRange the range of the axis
             */
            private fun computeFeatureBarOrigin(
                featuredMeasureValue: T,
                axisRange: ClosedRange<T>,
            ): T {
                val r = if (featuredMeasureValue.toDouble() >= 0) {
                    max(0.0, axisRange.start.toDouble())
                } else {
                    min(0.0, axisRange.endInclusive.toDouble())
                }

                return when (featuredMeasureValue) {
                    is Int -> {
                        r.toInt()
                    }

                    is Long -> {
                        r.toLong()
                    }

                    is Double -> {
                        r
                    }

                    is Float -> {
                        r.toFloat()
                    }

                    is Short -> {
                        r.toInt().toShort()
                    }

                    is Byte -> {
                        r.toInt().toByte()
                    }

                    else -> {
                        throw IllegalArgumentException("Unexpected Number type")
                    }
                } as T
            }
        }
}

@OptIn(ExperimentalKoalaPlotApi::class)
internal fun <T> SubcomposeMeasureScope.measureLabel(
    bulletScope: BulletBuilderScope<T>,
    constraints: Constraints,
): Placeable where T : Comparable<T>, T : Number = with(bulletScope) {
    val labelMeasurable = subcompose(label) { Box { label.value() } }[0]
    labelMeasurable.measure(constraints)
}

/**
 * A diamond-shaped indicator that may be used as a feature marker.
 * @param color The color for the marker
 * @param size The size for the marker in Dp
 */
@ExperimentalKoalaPlotApi
@Composable
@Deprecated("Use DiamondIndicatorDp instead", replaceWith = ReplaceWith("DiamondIndicatorDp"))
public fun DiamondIndicator(
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp,
) {
    Symbol(shape = DiamondShape, size = size, fillBrush = SolidColor(color))
}

/**
 * A diamond-shaped indicator that may be used as a feature marker.
 * @param color The color for the marker
 * @param size The size for the marker in Dp
 */
@ExperimentalKoalaPlotApi
@Composable
public fun DiamondIndicatorDp(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = KoalaPlotTheme.sizes.symbol,
) {
    Symbol(modifier, shape = DiamondShape, size = size, fillBrush = SolidColor(color))
}

/**
 * A diamond-shaped indicator that may be used as a feature marker.
 * @param color The color for the marker
 * @param sizeFraction The size for the marker as a fraction of the bullet graph height.
 */
@ExperimentalKoalaPlotApi
@Composable
public fun DiamondIndicator(
    color: Color = MaterialTheme.colorScheme.primary,
    sizeFraction: Float = DefaultSizeFraction,
) {
    Symbol(sizeFraction, shape = DiamondShape, fillBrush = SolidColor(color))
}

/**
 * A line for comparative measure indicators.
 * @param heightFraction The fraction of the overall bullet graph bar area height for the extent of the line
 * @param width The thickness of the line, defaults to 2.dp
 */
@Deprecated(
    "Use LineIndicator with a modifier parameter instead",
    replaceWith = ReplaceWith("LineIndicator(modifier, color, heightFraction, width)"),
)
@ExperimentalKoalaPlotApi
@Composable
public fun LineIndicator(
    color: Color = MaterialTheme.colorScheme.primary,
    heightFraction: Float = DefaultSizeFraction,
    width: Dp = 2.dp,
) {
    LineIndicator(modifier = Modifier, color = color, heightFraction = heightFraction, width = width)
}

/**
 * A line for comparative measure indicators.
 * @param heightFraction The fraction of the overall bullet graph bar area height for the extent of the line
 * @param width The thickness of the line, defaults to 2.dp
 */
@ExperimentalKoalaPlotApi
@Composable
public fun LineIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    heightFraction: Float = DefaultSizeFraction,
    width: Dp = 2.dp,
) {
    val shape = LineShape(heightFraction, width)
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val outline = shape.createOutline(this.size, layoutDirection, this)
                drawOutline(outline, SolidColor(color), style = Fill)
            }.clip(shape),
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
    border: BorderStroke? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(fraction)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .background(brush = brush, shape = shape)
            .clip(shape),
    )
}

/**
 * A line shape for comparative measure indicators.
 * @param heightFraction The fraction of the overall shape height for the extent of the line
 * @param width The fraction of the overall shape width for the thickness of the line
 */
private class LineShape(
    val heightFraction: Float,
    val width: Dp,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ) = Outline.Generic(
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
        },
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
public data class FixedFraction(
    internal val fraction: Float,
) : LabelWidth()

/**
 * Set the label width as a fixed [size] in Dp. If the label is smaller than the size it will be
 * right-justified to the graph.
 */
@ExperimentalKoalaPlotApi
public data class Fixed(
    internal val size: Dp,
) : LabelWidth()

/**
 * Set the label width as a variable [fraction] of the overall graph width. If the label is smaller
 * than the fraction the graph will grow to occupy the available space.
 */
@ExperimentalKoalaPlotApi
public data class VariableFraction(
    internal val fraction: Float,
) : LabelWidth()

/**
 * A slot to hold a configurable value in the bullet graph dsl.
 * @param default The default value held by the Slot
 * @param T The type of the value held by the Slot
 */
@ExperimentalKoalaPlotApi
@BulletGraphDslMarker
public open class Slot<T>(
    default: T,
) {
    internal var value: T = default

    /**
     * Sets the Slot's value.
     * @param setValue The value to set.
     */
    public operator fun invoke(setValue: T) {
        value = setValue
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Slot<*>

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int = value?.hashCode() ?: 0
}

/**
 * Specifies the settings for an Axis.
 */
@ExperimentalKoalaPlotApi
@BulletGraphDslMarker
public class AxisSettings<T> where T : Comparable<T>, T : Number {
    /**
     * Content for the bullet graph's axis labels.
     */
    public val labels: Slot<@Composable (T) -> Unit> = Slot {}

    /**
     * Shortcut to call the label Composable stored in the labels Slot.
     */
    @Composable
    internal infix fun Label(f: T) {
        labels.value(f)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as AxisSettings<*>

        if (labels != other.labels) return false
        return style == other.style
    }

    override fun hashCode(): Int {
        var result = labels.hashCode()
        result = 31 * result + (style?.hashCode() ?: 0)
        return result
    }

    /**
     * Optional styling for the bullet graph axis. If not provided, default styling will be used.
     */
    public var style: AxisStyle? = null

    internal companion object {
        @Composable
        fun <T> fromScope(bulletScope: BulletBuilderScope<T>): AxisSettings<T> where T : Comparable<T>, T : Number {
            val settings = AxisSettings<T>().apply(bulletScope.axis.value)
            // settings.model = settings.model ?: createLinearAxisModel(bulletScope)
            settings.style = settings.style ?: rememberAxisStyle()
            return settings
        }

//        private fun <T> createLinearAxisModel(builtScope: BulletBuilderScope<T>):
        //        FloatLinearAxisModel where T : Comparable<T>, T : Number {
//            val range = (
//                builtScope.rangesScope.ranges.map { it.value } +
//                    builtScope.featuredMeasure.value +
//                    builtScope.comparativeMeasures.map { it.value }
//                ).autoScaleRange()
//
//            return FloatLinearAxisModel(range, allowZooming = false, allowPanning = false)
//        }
    }
}

@DslMarker
public annotation class BulletGraphDslMarker
