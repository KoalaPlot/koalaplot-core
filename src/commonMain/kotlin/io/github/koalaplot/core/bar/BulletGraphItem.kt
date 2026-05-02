package io.github.koalaplot.core.bar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.unit.Constraints
import io.github.koalaplot.core.bar.BulletBuilderScope.FeaturedMeasureType
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.Axis
import io.github.koalaplot.core.xygraph.AxisDelegate
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Stable
internal interface BulletGraphItem {
    fun measureLabel(
        scope: SubcomposeMeasureScope,
        constraints: Constraints,
    ): Placeable

    fun measureAxisLabels(
        scope: SubcomposeMeasureScope,
        width: Int,
        bulletHeight: Int,
    ): AxisMeasurement<*>

    fun measureContent(
        scope: SubcomposeMeasureScope,
        axisInfo: AxisMeasurement<*>,
        rangeWidth: Int,
        bulletHeight: Int,
        animationSpec: AnimationSpec<Float>,
    ): BulletContentMeasurement
}

@Immutable
internal data class AxisMeasurement<T>(
    val axisDelegate: AxisDelegate<T>,
    val labelPlaceables: List<Placeable>,
    val labelsHeight: Int,
    val axisHeight: Int,
    val tickOffsets: List<Float>,
)

@Immutable
internal data class BulletContentMeasurement(
    val axisPlaceable: Placeable,
    val rangesMeasurement: RangesMeasurement,
    val rangeHeight: Int,
    val featureResult: FeaturedMeasureResult?,
    val comparativeMeasuresMeasurement: ComparativeMeasuresMeasurement,
)

@Immutable
internal data class RangesMeasurement(
    val rangePlaceables: List<Placeable>,
    val rangeOffsets: List<Float>,
)

@Immutable
internal data class FeaturedMeasureResult(
    val featurePlaceable: Placeable,
    val x: Int,
)

@Immutable
internal data class ComparativeMeasuresMeasurement(
    val comparativeMeasurePlaceables: List<Placeable>,
    val comparativeMeasureOffsets: List<Float>,
)

internal data class BulletLayoutPosition(
    val labelWidth: Int,
    val rangeStart: Int,
    val rangeWidth: Int,
    val yPos: Int,
    val bulletHeight: Int,
)

/**
 *
 */
@OptIn(ExperimentalKoalaPlotApi::class)
internal fun Placeable.PlacementScope.layoutBullet(
    bulletLayoutPosition: BulletLayoutPosition,
    labelPlaceable: Placeable,
    axisMeasurement: AxisMeasurement<*>,
    bulletContentMeasurement: BulletContentMeasurement,
) {
    labelPlaceable.place(
        bulletLayoutPosition.labelWidth - labelPlaceable.width,
        (bulletLayoutPosition.yPos + bulletLayoutPosition.bulletHeight / 2 - labelPlaceable.height / 2).coerceAtLeast(0),
    )

    bulletContentMeasurement.axisPlaceable.place(
        bulletLayoutPosition.rangeStart,
        bulletLayoutPosition.yPos + bulletContentMeasurement.rangeHeight,
    )

    // place axis labels
    axisMeasurement.tickOffsets.forEachIndexed { index, fl ->
        axisMeasurement.labelPlaceables[index].place(
            (
                bulletLayoutPosition.rangeStart + bulletLayoutPosition.rangeWidth * fl - axisMeasurement.labelPlaceables[index].width / 2
            ).roundToInt(),
            bulletLayoutPosition.yPos + (bulletLayoutPosition.bulletHeight - axisMeasurement.labelsHeight).coerceAtLeast(0),
        )
    }

    // place range indicators
    bulletContentMeasurement.rangesMeasurement.rangePlaceables.forEachIndexed { index, placeable ->
        val xPos =
            bulletLayoutPosition.rangeStart +
                bulletLayoutPosition.rangeWidth * bulletContentMeasurement.rangesMeasurement.rangeOffsets[index]
        placeable.place(xPos.roundToInt(), bulletLayoutPosition.yPos)
    }

    // place comparative measures
    bulletContentMeasurement.comparativeMeasuresMeasurement.comparativeMeasurePlaceables.forEachIndexed { index, placeable ->
        val xPos =
            bulletLayoutPosition.rangeStart +
                bulletLayoutPosition.rangeWidth * bulletContentMeasurement.comparativeMeasuresMeasurement.comparativeMeasureOffsets[index] -
                placeable.width / 2
        placeable.place(xPos.roundToInt(), bulletLayoutPosition.yPos)
    }

    // place feature
    bulletContentMeasurement.featureResult?.let {
        it.featurePlaceable.place(bulletLayoutPosition.rangeStart + it.x, bulletLayoutPosition.yPos)
    }
}

private enum class Slots {
    LABEL,
    AXIS_LABELS,
    AXIS,
    RANGES,
    FEATURE,
    COMPARATIVE,
}

internal class BulletGraphItemImpl<T>
    @OptIn(ExperimentalKoalaPlotApi::class)
    constructor(
        private val bbScope: BulletBuilderScope<T>,
        private val axisSettings: AxisSettings<T>,
    ) : BulletGraphItem where T : Comparable<T>, T : Number {
    @OptIn(ExperimentalKoalaPlotApi::class)
    override fun measureLabel(
        scope: SubcomposeMeasureScope,
        constraints: Constraints,
    ): Placeable = scope
        .subcompose(this to Slots.LABEL) {
            Box { bbScope.label.value() }
        }.first()
        .measure(constraints)

    @OptIn(ExperimentalKoalaPlotApi::class)
    override fun measureAxisLabels(
        scope: SubcomposeMeasureScope,
        width: Int,
        bulletHeight: Int,
    ): AxisMeasurement<T> {
        val axis = AxisDelegate.createHorizontalAxis(bbScope.axisModel, axisSettings.style!!, scope.run { width.toDp() })
        val axisHeight = scope.run { (axis.thicknessDp - axis.axisOffset).toPx().roundToInt() }
        val measurables = axis.majorTickValues.flatMap { tick ->
            scope.subcompose(this to tick) { Box { axisSettings.Label(tick) } }
        }
        val placeables = measurables.map {
            it.measure(
                Constraints(
                    maxWidth = (width / axis.majorTickValues.size.coerceAtLeast(1)).coerceAtLeast(0),
                    maxHeight = bulletHeight,
                ),
            )
        }
        return AxisMeasurement(
            axisDelegate = axis,
            labelPlaceables = placeables,
            labelsHeight = placeables.maxOfOrNull { it.height } ?: 0,
            axisHeight = axisHeight,
            tickOffsets = axis.majorTickValues.map { bbScope.axisModel.computeOffset(it) },
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun measureContent(
        scope: SubcomposeMeasureScope,
        axisInfo: AxisMeasurement<*>,
        rangeWidth: Int,
        bulletHeight: Int,
        animationSpec: AnimationSpec<Float>,
    ): BulletContentMeasurement {
        val axis = axisInfo.axisDelegate as AxisDelegate<T>
        val rangeHeight = (bulletHeight - axisInfo.axisHeight - axisInfo.labelsHeight).coerceAtLeast(0)

        // Axis
        val axisPlaceable = scope
            .subcompose(this to "axis") { Axis(axis) }
            .first()
            .measure(Constraints.fixedWidth(rangeWidth))

        val rangesMeasurement = measureRanges(scope, rangeWidth, rangeHeight)
        val featureMeasurement = measureFeature(scope, rangeWidth, rangeHeight, animationSpec)
        val comparativeMeasuresMeasurement = measureComparativeMeasures(scope, rangeHeight)

        return BulletContentMeasurement(
            axisPlaceable = axisPlaceable,
            rangesMeasurement = rangesMeasurement,
            rangeHeight = rangeHeight,
            featureResult = featureMeasurement,
            comparativeMeasuresMeasurement = comparativeMeasuresMeasurement,
        )
    }

    @OptIn(ExperimentalKoalaPlotApi::class)
    private fun measureRanges(
        scope: SubcomposeMeasureScope,
        rangeWidth: Int,
        rangeHeight: Int,
    ): RangesMeasurement {
        val rangeMeasurables: List<Measurable> = buildList {
            for (rangeIndex in 1 until bbScope.rangesScope.ranges.size) {
                val range = bbScope.rangesScope.ranges[rangeIndex]
                add(
                    scope
                        .subcompose(this to range) {
                            if (range.indicator == null) {
                                val shadeIndex = min(bbScope.rangesScope.ranges.size - 2, DefaultRangeShades.lastIndex)
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
                        }.first(),
                )
            }
        }

        val rangeOffsets = bbScope.rangesScope.ranges.map { bbScope.axisModel.computeOffset(it.value) }

        val rangePlaceables = rangeMeasurables.mapIndexed { rangeIndex, measurable ->
            val maxWidth = (
                (rangeWidth * rangeOffsets[rangeIndex + 1]).roundToInt() -
                    (rangeWidth * rangeOffsets[rangeIndex]).roundToInt()
            ).absoluteValue
            measurable.measure(Constraints.fixed(maxWidth, rangeHeight))
        }

        return RangesMeasurement(rangePlaceables, rangeOffsets)
    }

    @OptIn(ExperimentalKoalaPlotApi::class)
    private fun measureFeature(
        scope: SubcomposeMeasureScope,
        rangeWidth: Int,
        rangeHeight: Int,
        animationSpec: AnimationSpec<Float>,
    ): FeaturedMeasureResult? = bbScope.featuredMeasure?.let { featuredMeasure ->
        val origin = computeFeatureBarOrigin(featuredMeasure.value, bbScope.axisModel.range)
        val featureBarOriginOffset = bbScope.axisModel.computeOffset(origin)
        val featureBarValueOffset = bbScope.axisModel.computeOffset(featuredMeasure.value)
        val featureWidth = rangeWidth * abs(featureBarValueOffset - featureBarOriginOffset)

        val measurable = scope.subcompose(this to Slots.FEATURE) {
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

        val featurePlaceable = measurable.measure(Constraints(maxWidth = rangeWidth, maxHeight = rangeHeight))

        val value = featuredMeasure.value

        // xPos is the offset from rangeStart
        val xPos = if (featuredMeasure.type == FeaturedMeasureType.BAR) {
            if (value.toDouble() < 0) {
                rangeWidth * featureBarOriginOffset - featurePlaceable.width
            } else {
                rangeWidth * featureBarOriginOffset
            }
        } else {
            rangeWidth * featureBarValueOffset - featurePlaceable.width / 2
        }

        FeaturedMeasureResult(featurePlaceable, xPos.roundToInt())
    }

    @OptIn(ExperimentalKoalaPlotApi::class)
    private fun measureComparativeMeasures(
        scope: SubcomposeMeasureScope,
        rangeHeight: Int,
    ): ComparativeMeasuresMeasurement {
        val measurables = bbScope.comparativeMeasures.map {
            scope
                .subcompose(this to it) {
                    Box(contentAlignment = Alignment.Center) { it.indicator() }
                }.first() to bbScope.axisModel.computeOffset(it.value)
        }

        val comparativeMeasurePlaceables = measurables.map {
            // square space, so use rangeHeight for both width and height
            it.first.measure(Constraints.fixed(rangeHeight, rangeHeight)) to it.second
        }

        return ComparativeMeasuresMeasurement(comparativeMeasurePlaceables.map { it.first }, comparativeMeasurePlaceables.map { it.second })
    }
}

/**
 * Technically per the BulletGraph spec a bar is not to be used if the origin of the axis does not include
 * 0, but this will force the feature bar to have an origin of 0 or a bound of the axis in order to display
 * in a reasonable way.
 * @param featuredMeasureValue The value of the featured measure
 * @param axisRange the range of the axis
 */
private fun <T> computeFeatureBarOrigin(
    featuredMeasureValue: T,
    axisRange: ClosedRange<T>,
): T where T : Comparable<T>, T : Number {
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

private val DefaultRangeShades = listOf(
    listOf(0.65f),
    listOf(0.65f, 0.9f),
    listOf(0.6f, 0.75f, 0.9f),
    listOf(0.5f, 0.65f, 0.8f, 0.9f),
    listOf(0.5f, 0.65f, 0.8f, 0.9f, 0.97f),
)

private const val MinRangeShade = 0.99f
