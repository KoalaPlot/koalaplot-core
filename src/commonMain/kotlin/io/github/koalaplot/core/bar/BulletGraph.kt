package io.github.koalaplot.core.bar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import io.github.koalaplot.core.theme.KoalaPlotTheme
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xychart.Axis
import io.github.koalaplot.core.xychart.AxisDelegate
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val DefaultRangeShades = listOf(
    listOf(0.65f),
    listOf(0.65f, 0.9f),
    listOf(0.6f, 0.75f, 0.9f),
    listOf(0.5f, 0.65f, 0.8f, 0.9f),
    listOf(0.5f, 0.65f, 0.8f, 0.9f, 0.97f),
)
private const val MinRangeShade = 0.99f

private class BulletGraphBuilder @OptIn(ExperimentalKoalaPlotApi::class) constructor(
    val slotId: Int,
    val scope: SubcomposeMeasureScope,
    val bulletScope: BulletBuilderScope,
    val bulletHeight: Int,
    val axisSettings: AxisSettings
) {
    var labelPlaceable: Placeable? = null
    var axis: AxisDelegate<Float>? = null
    var axisLabelPlaceables: List<Placeable>? = null
    var axisHeight: Int? = null
    var axisPlaceable: Placeable? = null
    val axisLabelsHeight: Int by lazy {
        requireNotNull(axisLabelPlaceables)
        axisLabelPlaceables?.let { placeables ->
            placeables.maxOfOrNull {
                it.height
            }
        } ?: 0
    }
    val rangeHeight: Int by lazy {
        requireNotNull(axisHeight) { "axisHeight must not be null in order to calculate rangeHeight" }
        (bulletHeight - axisHeight!! - axisLabelsHeight).coerceAtLeast(0)
    }
    var rangePlaceables: List<Placeable>? = null
    var featurePlaceable: Placeable? = null
    var comparativeMeasurePlaceables: List<Placeable>? = null

    @OptIn(ExperimentalKoalaPlotApi::class)
    fun measureLabel(labelWidthMaxConstraint: Int) {
        labelPlaceable = scope.measureLabel(
            bulletScope,
            Constraints(maxWidth = labelWidthMaxConstraint, maxHeight = bulletHeight)
        )
    }

    private enum class Slots {
        AXIS_LABELS,
        AXIS,
        RANGES,
        FEATURE,
        COMPARATIVE
    }

    private fun slotId(slot: Slots): String {
        return "$slotId.${slot.name}"
    }

    /**
     * @param width of the non-label graph area: constraints.maxWidth - labelWidth
     */
    @OptIn(ExperimentalKoalaPlotApi::class)
    fun measureAxisLabels(width: Int) {
        with(scope) {
            val axis = AxisDelegate.createHorizontalAxis(axisSettings.model!!, axisSettings.style!!, width.toDp())
            axisHeight = (axis.thicknessDp - axis.axisOffset).toPx().roundToInt()

            val measurable = subcompose(slotId(Slots.AXIS_LABELS)) {
                axis.majorTickValues.forEach { Box { axisSettings.label(it) } }
            }
            axisLabelPlaceables = measurable.map {
                it.measure(
                    Constraints(
                        maxWidth =
                        (width / axis.majorTickValues.size).coerceAtLeast(0),
                        maxHeight = bulletHeight,
                    )
                )
            }

            this@BulletGraphBuilder.axis = axis
        }
    }

    /**
     * @param rangeWidth Width of the axis
     */
    fun measureAxis(rangeWidth: Int) {
        requireNotNull(axis) { "axis must not be null" }
        axisPlaceable =
            scope.subcompose(slotId(Slots.AXIS)) { Axis(axis!!) }[0].measure(Constraints.fixedWidth(rangeWidth))
    }

    @OptIn(ExperimentalKoalaPlotApi::class)
    fun measureRanges(rangeWidth: Int) {
        requireNotNull(axisSettings.model) { "Axis model must not be null" }

        val rangeMeasurables: List<Measurable> =
            scope.subcompose(slotId(Slots.RANGES)) {
                RangeIndicators(bulletScope)
            }

        val axisModel = axisSettings.model!!
        val ranges = bulletScope.rangesScope.ranges
        rangePlaceables = rangeMeasurables.mapIndexed { rangeIndex, measurable ->
            val maxWidth = (
                (rangeWidth * axisModel.computeOffset(ranges[rangeIndex + 1].value)).roundToInt() -
                    (rangeWidth * axisModel.computeOffset(ranges[rangeIndex].value)).roundToInt()
                ).absoluteValue
            measurable.measure(Constraints.fixed(maxWidth, rangeHeight))
        }
    }

    @OptIn(ExperimentalKoalaPlotApi::class)
    fun measureFeature(rangeWidth: Int, beta: Float) {
        requireNotNull(axisSettings.model) { "Axis model must not be null" }

        val measurable = scope.subcompose(slotId(Slots.FEATURE)) {
            Box(contentAlignment = Alignment.Center) { bulletScope.featuredMeasure.indicator() }
        }[0]

        val axisModel = axisSettings.model!!

        featurePlaceable = if (bulletScope.featuredMeasure.type == BulletBuilderScope.FeaturedMeasureType.BAR) {
            val origin = computeFeatureBarOrigin(bulletScope.featuredMeasure.value, axisModel.range)

            measurable.measure(
                Constraints.fixed(
                    (
                        rangeWidth *
                            abs(
                                axisModel.computeOffset(bulletScope.featuredMeasure.value) -
                                    axisModel.computeOffset(origin)
                            ) * beta
                        ).roundToInt(),
                    rangeHeight
                )
            )
        } else {
            measurable.measure(Constraints.fixed(rangeHeight, rangeHeight))
        }
    }

    @OptIn(ExperimentalKoalaPlotApi::class)
    fun measureComparativeMeasures() {
        val measurables: List<Measurable> =
            scope.subcompose(slotId(Slots.COMPARATIVE)) {
                bulletScope.comparativeMeasures.forEach {
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
        rangeWidth: Int
    ) {
        requireNotNull(axis) { "axis must not be null during layout" }
        requireNotNull(axisLabelPlaceables) { "axis label placeables must not be null during layout" }
        requireNotNull(labelPlaceable) { "labelPlacesable must not be null during layout" }
        requireNotNull(axisPlaceable) { "axisPlaceable must not be null during layout" }
        requireNotNull(rangePlaceables) { "rangePlaceables must not be null during layout" }
        requireNotNull(comparativeMeasurePlaceables) { "comparativeMeasurePlaceables must not be null during layout" }
        requireNotNull(featurePlaceable) { "featurePlaceable must not be null during layout" }

        val axis = axis!!
        val axisLabelPlaceables = axisLabelPlaceables!!
        val labelPlaceable = labelPlaceable!!
        val axisPlaceable = axisPlaceable!!
        val rangePlaceables = rangePlaceables!!
        val comparativeMeasurePlaceables = comparativeMeasurePlaceables!!
        val featurePlaceable = featurePlaceable!!

        val rangeStart = labelWidth + firstAxisLabelWidth / 2

        labelPlaceable.place(
            labelWidth - labelPlaceable.width,
            (yPos + bulletHeight / 2 - labelPlaceable.height / 2).coerceAtLeast(0)
        )

        axisPlaceable.place(rangeStart, yPos + rangeHeight)

        // place axis labels
        axis.majorTickValues.forEachIndexed { index, fl ->
            axisLabelPlaceables[index].place(
                (
                    rangeStart + rangeWidth * axisSettings.model!!.computeOffset(fl) -
                        axisLabelPlaceables[index].width / 2
                    ).roundToInt(),
                yPos + (bulletHeight - axisLabelsHeight).coerceAtLeast(0)
            )
        }

        // place range indicators
        rangePlaceables.forEachIndexed { index, placeable ->
            val xPos =
                rangeStart +
                    rangeWidth * axisSettings.model!!.computeOffset(bulletScope.rangesScope.ranges[index].value)
            placeable.place(xPos.roundToInt(), yPos)
        }

        // place comparative measures
        comparativeMeasurePlaceables.forEachIndexed { index, placeable ->
            val xPos =
                rangeStart +
                    rangeWidth * axisSettings.model!!.computeOffset(bulletScope.comparativeMeasures[index].value) -
                    placeable.width / 2
            placeable.place(xPos.roundToInt(), yPos)
        }

        // place feature
        val xPos =
            rangeStart + if (bulletScope.featuredMeasure.type == BulletBuilderScope.FeaturedMeasureType.BAR) {
                val value = bulletScope.featuredMeasure.value
                val origin = computeFeatureBarOrigin(bulletScope.featuredMeasure.value, axisSettings.model!!.range)
                if (value < 0) {
                    rangeWidth * axisSettings.model!!.computeOffset(origin) - featurePlaceable.width
                } else {
                    rangeWidth * axisSettings.model!!.computeOffset(origin)
                }
            } else {
                rangeWidth * axisSettings.model!!.computeOffset(bulletScope.featuredMeasure.value) -
                    featurePlaceable.width / 2
            }
        featurePlaceable.place(
            xPos.roundToInt(),
            yPos + rangeHeight / 2 - featurePlaceable.height / 2
        )
    }
}

/**
 * Technically per the BulletGraph spec a bar is not to be used if the origin of the axis does not include
 * 0, but this will force the feature bar to have an origin of 0 or a bound of the axis in order to display
 * in a reasonable way.
 * @param featuredMeasureValue The value of the featured measure
 * @param axisRange the range of the axis
 */
private fun computeFeatureBarOrigin(featuredMeasureValue: Float, axisRange: ClosedFloatingPointRange<Float>): Float {
    return if (featuredMeasureValue >= 0) {
        max(0f, axisRange.start)
    } else {
        min(0f, axisRange.endInclusive)
    }
}

/**
 * Implementation of a bullet graph as defined in
 * http://www.perceptualedge.com/articles/misc/Bullet_Graph_Design_Spec.pdf.
 *
 * See [BulletBuilderScope] for how to configure the bullet graph.
 *
 * @param animationSpec Animation to use for animating feature bar growth
 */
@ExperimentalKoalaPlotApi
@Composable
public fun BulletGraph(
    modifier: Modifier = Modifier,
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec,
    builder: BulletBuilderScope.() -> Unit
) {
    BulletGraphs(modifier, animationSpec = animationSpec) {
        bullet(builder)
    }
}

/**
 * A vertical layout of multiple bullet graphs. Each bullet will equally share the vertical space and be the same
 * height. The left and right edges of all bullets will be aligned horizontally.
 *
 * See [BulletGraphScope] and [BulletBuilderScope] for how to configure the bullet graphs.
 *
 * @param gap Size of a gap to leave between adjacent bullet graphs.
 * @param animationSpec Animation to use for animating feature bar growth
 */
@ExperimentalKoalaPlotApi
@Composable
public fun BulletGraphs(
    modifier: Modifier = Modifier,
    gap: Dp = KoalaPlotTheme.sizes.gap,
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec,
    builder: BulletGraphScope.() -> Unit
) {
    val graphScope = remember(builder) { BulletGraphScope().apply(builder) }

    // Animation scale factor
    val beta = remember(graphScope) { Animatable(0f) }
    LaunchedEffect(graphScope) {
        beta.animateTo(1f, animationSpec = animationSpec)
    }

    if (graphScope.scopes.isEmpty()) {
        return
    }

    val bulletScopes = graphScope.scopes
    val axisSettings = bulletScopes.map { AxisSettings.fromScope(it) }

    SubcomposeLayout(modifier = modifier) { constraints ->
        val bulletHeight =
            ((constraints.maxHeight - gap.roundToPx() * graphScope.scopes.size - 1) / graphScope.scopes.size)
                .coerceAtLeast(0)

        val builders = graphScope.scopes.mapIndexed { index, bulletScope ->
            BulletGraphBuilder(index, this, bulletScope, bulletHeight, axisSettings[index])
        }

        val labelWidthMaxConstraint = calculateLabelWidthMaxConstraint(graphScope, constraints)

        builders.forEach { it.measureLabel(labelWidthMaxConstraint) }

        val labelWidth = calculateLabelWidth(graphScope, labelWidthMaxConstraint, builders.map { it.labelPlaceable!! })

        builders.forEach { it.measureAxisLabels(constraints.maxWidth - labelWidth) }

        val firstAxisLabelWidth = builders.maxOf { it.axisLabelPlaceables!!.first().width }
        val lastAxisLabelWidth = builders.maxOf { it.axisLabelPlaceables!!.last().width }
        val rangeWidth =
            (constraints.maxWidth - firstAxisLabelWidth / 2 - lastAxisLabelWidth / 2 - labelWidth).coerceAtLeast(0)

        builders.forEach {
            it.measureAxis(rangeWidth)
            it.measureRanges(rangeWidth)
            it.measureFeature(rangeWidth, beta.value)
            it.measureComparativeMeasures()
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            var yPos = 0

            builders.forEach {
                with(it) {
                    layout(yPos, labelWidth, firstAxisLabelWidth, rangeWidth)
                }
                yPos += bulletHeight + gap.roundToPx()
            }
        }
    }
}

@OptIn(ExperimentalKoalaPlotApi::class)
private fun Density.calculateLabelWidthMaxConstraint(graphScope: BulletGraphScope, constraints: Constraints): Int {
    val labelWidth = if (graphScope.scopes.size == 1) {
        graphScope.scopes.first().labelWidth
    } else {
        graphScope.labelWidth
    }
    val labelWidthMaxConstraint = when (labelWidth) {
        is FixedFraction -> {
            (constraints.maxWidth * labelWidth.fraction).roundToInt()
        }
        is VariableFraction -> {
            (constraints.maxWidth * labelWidth.fraction).roundToInt()
        }
        is Fixed -> {
            labelWidth.size.roundToPx()
        }
    }
    return labelWidthMaxConstraint
}

@OptIn(ExperimentalKoalaPlotApi::class)
private fun calculateLabelWidth(
    graphScope: BulletGraphScope,
    labelWidthMaxConstraint: Int,
    labelPlaceable: List<Placeable>
): Int {
    val labelWidth = if (graphScope.scopes.size == 1) {
        graphScope.scopes.first().labelWidth
    } else {
        graphScope.labelWidth
    }

    return if (labelWidth is VariableFraction) {
        labelPlaceable.maxOf { it.width }
    } else {
        labelWidthMaxConstraint
    }
}

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
private fun RangeIndicators(builtScope: BulletBuilderScope) {
    val shadeIndex = max(builtScope.rangesScope.ranges.size - 1, DefaultRangeShades.lastIndex)
    val numShades = DefaultRangeShades[shadeIndex].size
    for (rangeIndex in 1 until builtScope.rangesScope.ranges.size) {
        val range = builtScope.rangesScope.ranges[rangeIndex]
        if (range.indicator == null) {
            val shade = if (rangeIndex - 1 >= numShades) {
                MinRangeShade
            } else {
                DefaultRangeShades[shadeIndex][rangeIndex - 1]
            }
            HorizontalBarIndicator(SolidColor(Color(shade, shade, shade)))
        } else {
            Box(modifier = Modifier.fillMaxSize()) { range.indicator.invoke() }
        }
    }
}

/**
 * A scope for constructing displays that layout and align features among multiple bullet graphs.
 */
@ExperimentalKoalaPlotApi
@BulletGraphDslMarker
public class BulletGraphScope {
    internal val scopes: MutableList<BulletBuilderScope> = mutableListOf()

    /**
     * Sets the label width for all BulletGraphs.
     */
    public var labelWidth: LabelWidth = FixedFraction(0.25f)

    /**
     * Creates a new bullet graph and allows its configuration via [BulletBuilderScope]. Bullet graphs
     * are displayed in the order in which they are created from top to bottom.
     */
    public fun bullet(builder: BulletBuilderScope.() -> Unit) {
        scopes.add(BulletBuilderScope().apply(builder))
    }
}
