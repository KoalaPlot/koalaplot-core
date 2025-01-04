package io.github.koalaplot.core.pie

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.withSaveLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.koalaplot.core.animation.StartAnimationUseCase
import io.github.koalaplot.core.style.KoalaPlotTheme
import io.github.koalaplot.core.util.AngularValue
import io.github.koalaplot.core.util.DegreesFullCircle
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.util.HoverableElementArea
import io.github.koalaplot.core.util.HoverableElementAreaScope
import io.github.koalaplot.core.util.circumscribedSquareSize
import io.github.koalaplot.core.util.deg
import io.github.koalaplot.core.util.generateHueColorPalette
import io.github.koalaplot.core.util.lineTo
import io.github.koalaplot.core.util.moveTo
import io.github.koalaplot.core.util.polarToCartesian
import io.github.koalaplot.core.util.toDegrees
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

private const val DefaultLabelDiameterScale = 1.1f

/**
 * Defines the angular position of a single Pie Slice in terms of the starting angle for the slice, [startAngle], and
 * the angular size of the slice, [angle]. 0-degrees is to the right and angles increase in the clockwise
 * direction.
 */
public data class PieSliceData(
    val startAngle: AngularValue,
    val angle: AngularValue,
)

private const val AngleCCWTop = -90f

private fun makePieSliceData(
    data: List<Float>,
    beta: Float,
    pieStartAngle: AngularValue,
    pieExtendAngle: AngularValue,
): List<PieSliceData> {
    val total = data.sumOf {
        it.toDouble()
    }.toFloat().let {
        if (it == 0F) {
            1F
        } else {
            it
        }
    }

    return buildList {
        var startAngle = pieStartAngle.toDegrees().value
        val pieExtend = pieExtendAngle.toDegrees().value
        for (i in data.indices) {
            val extent = data[i] / total * pieExtend * beta
            add(PieSliceData(startAngle.deg, extent.deg))
            startAngle += extent
        }
    }
}

/**
 * Scope for Pie slices.
 * @property pieSliceData [PieSliceData] describing the slice
 * @property innerRadius The inside radius of the slice, as a fraction of its constraint's width,
 * must be between 0 and 1 inclusive
 * @property outerRadius The outside radius of the slice, as a fraction of its constraint's width,
 * must be between 0 and 1 inclusive
 */
public interface PieSliceScope : HoverableElementAreaScope {
    public val pieSliceData: PieSliceData
    public val innerRadius: Float
    public val outerRadius: Float
}

private data class PieSliceScopeImpl(
    override val pieSliceData: PieSliceData,
    override val innerRadius: Float,
    override val outerRadius: Float,
    val hoverableElementAreaScope: HoverableElementAreaScope
) : PieSliceScope, HoverableElementAreaScope by hoverableElementAreaScope

/**
 * The LabelConnectorScope provides geometry information to LabelConnector implementations so they
 * can draw the connector from the graph element to the label.
 */
public interface LabelConnectorScope {
    /**
     * The starting position of the connector at the graph element.
     */
    public val startPosition: MutableState<Offset>

    /**
     * The ending position of the connector at the label.
     */
    public val endPosition: MutableState<Offset>

    /**
     * The angle of the graph element at the start of the connector. For a Pie chart this is
     * the angle of the slice it connects to.
     */
    public val startAngle: MutableState<AngularValue>

    /**
     * The angle of the label at the end of the connector. This is an angle that is normal to the
     * connector's bounding box at the [endPosition]
     */
    public val endAngle: MutableState<AngularValue>
}

internal data class LabelConnectorScopeImpl(
    override val startPosition: MutableState<Offset> = mutableStateOf(Offset.Zero),
    override val endPosition: MutableState<Offset> = mutableStateOf(Offset.Zero),
    override val startAngle: MutableState<AngularValue> = mutableStateOf(0.deg),
    override val endAngle: MutableState<AngularValue> = mutableStateOf(0.deg)
) : LabelConnectorScope

// Initial outer radius as a fraction of size before hover expansion
private const val InitOuterRadius = 0.95f

private const val LabelFadeInDuration = 1000

/**
 * Creates a Pie Chart or, optionally, a Donut Chart if holeSize is nonZero, with optional
 * hole content to place at the center of the donut hole. Pie slices are drawn starting at
 * -90 degrees (top center), progressing clockwise around the pie. Each slice occupies a fraction
 * of the overall pie according to its data value relative to the sum of all values.
 *
 * @param values The data values for each pie slice
 * @param labelPositionProvider A provider of label offsets that can be used to implement different label
 * placement strategies. See the [PieChart] override for a version that uses labels placed around the circumference
 * of the pie.
 * @param modifier Compose Modifiers to be applied to the overall PieChart
 * @param slice Composable for a pie slice.
 * @param label Composable for a pie slice label placed around the perimeter of the pie
 * @param labelConnector Composable for label connectors connecting the pie slice to the label
 * @param holeSize A relative size for an inner hole of the pie, creating a donut chart, with a
 * value between 0 and 1.
 * @param holeContent Optional content that may be placed in the space of the donut hole. To safely draw the content
 * within the hole without intersecting with the chart, apply the passed `contentPadding` to your content composable.
 * @param minPieDiameter Minimum diameter allowed for the pie.
 * @param maxPieDiameter Maximum diameter allowed for the pie. May be Infinity but not Unspecified.
 * @param forceCenteredPie If true, will force the pie to be centered within its parent, by adjusting (decreasing) the
 * pie size to accommodate label sizes and positions. If false, will maximize the pie diameter.
 * @param pieAnimationSpec Specifies the animation to use when the pie chart is first drawn.
 * @param labelAnimationSpec Specifies the animation to use when the labels are drawn. Drawing of the labels begins
 * after the pie animation is complete.
 * @param pieStartAngle Sets an angle for the pie data to start at. Defaults to the top of the pie.
 * @param pieExtendAngle Sets a max angle for the pie to extend to, with a value between 1 and 360.
 * Defaults to [DegreesFullCircle].
 */
@ExperimentalKoalaPlotApi
@Composable
public fun PieChart(
    values: List<Float>,
    labelPositionProvider: LabelPositionProvider,
    modifier: Modifier = Modifier,
    slice: @Composable PieSliceScope.(Int) -> Unit = {
        val colors = remember(values.size) { generateHueColorPalette(values.size) }
        DefaultSlice(colors[it])
    },
    label: @Composable (Int) -> Unit = {},
    labelConnector: @Composable LabelConnectorScope.(Int) -> Unit = { StraightLineConnector() },
    holeSize: Float = 0f,
    holeContent: @Composable BoxScope.(contentPadding: PaddingValues) -> Unit = {},
    minPieDiameter: Dp = 100.dp,
    maxPieDiameter: Dp = 300.dp,
    forceCenteredPie: Boolean = false,
    pieAnimationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec,
    labelAnimationSpec: AnimationSpec<Float> = tween(LabelFadeInDuration, 0, LinearOutSlowInEasing),
    pieStartAngle: AngularValue = AngleCCWTop.deg,
    pieExtendAngle: AngularValue = DegreesFullCircle.deg,
) {
    PieChart(
        values = values,
        labelPositionProvider = labelPositionProvider,
        modifier = modifier,
        slice = slice,
        label = label,
        labelConnector = labelConnector,
        holeSize = holeSize,
        holeContent = holeContent,
        minPieDiameter = minPieDiameter,
        maxPieDiameter = maxPieDiameter,
        forceCenteredPie = forceCenteredPie,
        pieStartAngle = pieStartAngle,
        pieExtendAngle = pieExtendAngle,
        startAnimationUseCase = StartAnimationUseCase(
            executionType = StartAnimationUseCase.ExecutionType.Default,
            pieAnimationSpec,
            labelAnimationSpec,
        )
    )
}

/**
 * Creates a Pie Chart or, optionally, a Donut Chart if holeSize is nonZero, with optional
 * hole content to place at the center of the donut hole. Pie slices are drawn starting at
 * -90 degrees (top center), progressing clockwise around the pie. Each slice occupies a fraction
 * of the overall pie according to its data value relative to the sum of all values.
 *
 * @param values The data values for each pie slice
 * @param labelPositionProvider A provider of label offsets that can be used to implement different label
 * placement strategies. See the [PieChart] override for a version that uses labels placed around the circumference
 * of the pie.
 * @param modifier Compose Modifiers to be applied to the overall PieChart
 * @param slice Composable for a pie slice.
 * @param label Composable for a pie slice label placed around the perimeter of the pie
 * @param labelConnector Composable for label connectors connecting the pie slice to the label
 * @param holeSize A relative size for an inner hole of the pie, creating a donut chart, with a
 * value between 0 and 1.
 * @param holeContent Optional content that may be placed in the space of the donut hole. To safely draw the content
 * within the hole without intersecting with the chart, apply the passed `contentPadding` to your content composable.
 * @param minPieDiameter Minimum diameter allowed for the pie.
 * @param maxPieDiameter Maximum diameter allowed for the pie. May be Infinity but not Unspecified.
 * @param forceCenteredPie If true, will force the pie to be centered within its parent, by adjusting (decreasing) the
 * pie size to accommodate label sizes and positions. If false, will maximize the pie diameter.
 * @param startAnimationUseCase Controls the animation.
 * @param pieStartAngle Sets an angle for the pie data to start at. Defaults to the top of the pie.
 * @param pieExtendAngle Sets a max angle for the pie to extend to, with a value between 1 and 360.
 * Defaults to [DegreesFullCircle].
 */
@ExperimentalKoalaPlotApi
@Composable
public fun PieChart(
    values: List<Float>,
    labelPositionProvider: LabelPositionProvider,
    modifier: Modifier = Modifier,
    slice: @Composable PieSliceScope.(Int) -> Unit = {
        val colors = remember(values.size) { generateHueColorPalette(values.size) }
        DefaultSlice(colors[it])
    },
    label: @Composable (Int) -> Unit = {},
    labelConnector: @Composable LabelConnectorScope.(Int) -> Unit = { StraightLineConnector() },
    holeSize: Float = 0f,
    holeContent: @Composable BoxScope.(contentPadding: PaddingValues) -> Unit = {},
    minPieDiameter: Dp = 100.dp,
    maxPieDiameter: Dp = 300.dp,
    forceCenteredPie: Boolean = false,
    startAnimationUseCase: StartAnimationUseCase =
        StartAnimationUseCase(
            executionType = StartAnimationUseCase.ExecutionType.Default,
            /* chart animation */ KoalaPlotTheme.animationSpec,
            /* label animation */  tween(LabelFadeInDuration, 0, LinearOutSlowInEasing)
        ),
    pieStartAngle: AngularValue = AngleCCWTop.deg,
    pieExtendAngle: AngularValue = DegreesFullCircle.deg,
) {
    require(holeSize in 0f..1f) { "holeSize must be between 0 and 1" }
    require(maxPieDiameter != Dp.Unspecified) { "maxPieDiameter cannot be Unspecified" }
    require(pieExtendAngle.toDegrees().value > 0f && pieExtendAngle.toDegrees().value <= DegreesFullCircle) {
        "pieExtendAngle must be between 0 and 360, exclusive of 0"
    }
    require(startAnimationUseCase.animatables.size == 2) { "startAnimationUseCase must have 2 animatables" }

    val currentValues by rememberUpdatedState(values)
    val beta = remember(values) { startAnimationUseCase.animatables[0] }
    val labelAlpha = remember(values) { startAnimationUseCase.animatables[1] }

    startAnimationUseCase(key = values)

    // pieSliceData that gets animated - used for drawing the pie
    val pieSliceData by remember(beta.value) {
        derivedStateOf { makePieSliceData(currentValues, beta.value, pieStartAngle, pieExtendAngle) }
    }

    // pieSliceData when the animation is complete - used for sizing & label layout/positioning
    val finalPieSliceData by remember {
        derivedStateOf { makePieSliceData(currentValues, 1f, pieStartAngle, pieExtendAngle) }
    }

    HoverableElementArea(modifier = modifier) {
        SubcomposeLayout(modifier = Modifier.clipToBounds()) { constraints ->
            val pieMeasurePolicy =
                PieMeasurePolicy(finalPieSliceData, holeSize, labelPositionProvider, InitOuterRadius, forceCenteredPie)

            val pieMeasurable = subcompose("pie") { Pie(pieSliceData, slice, holeSize) }[0]

            val labelMeasurables = subcompose("labels") {
                pieSliceData.indices.forEach { index ->
                    // Wrapping in box ensures there is 1 measurable element
                    // emitted per label & applies fade animation
                    Box(modifier = Modifier.alpha(labelAlpha.value)) { label(index) }
                }
            }

            val (pieDiameter, piePlaceable, labelPlaceables) = pieMeasurePolicy.measure(
                pieMeasurable,
                labelMeasurables,
                constraints,
                minPieDiameter.toPx(),
                maxPieDiameter.toPx()
            )

            val labelPositions =
                labelPositionProvider.computeLabelPositions(
                    pieDiameter * InitOuterRadius,
                    holeSize,
                    labelPlaceables,
                    finalPieSliceData
                )

            val size = pieMeasurePolicy.computeSize(labelPlaceables, labelPositions, pieDiameter).run {
                // add one due to later float to int conversion dropping the fraction part
                copy(
                    (width + 1).coerceAtMost(constraints.maxWidth.toFloat()),
                    (height + 1).coerceAtMost(constraints.maxHeight.toFloat())
                )
            }

            val labelConnectorTranslations = pieMeasurePolicy.computeLabelConnectorScopes(labelPositions, pieDiameter)

            val holeDiameter = pieDiameter * holeSize.toDouble()
            val holeSafeEdgeLength = circumscribedSquareSize(holeDiameter)
            val holePlaceable = subcompose("hole") {
                Box(modifier = Modifier.clip(CircleShape)) {
                    holeContent(PaddingValues((holeDiameter - holeSafeEdgeLength).toInt().dp))
                }
            }[0].measure(Constraints.fixed(holeDiameter.toInt(), holeDiameter.toInt()))

            val connectorPlaceables = pieSliceData.mapIndexed { index, _ ->
                subcompose("connector $index") {
                    Box(modifier = Modifier.fillMaxSize().alpha(labelAlpha.value)) {
                        labelConnectorTranslations[index]?.let {
                            with(it.second) { labelConnector(index) }
                        }
                    }
                }.map { it.measure(constraints) }
            }.flatten()

            with(pieMeasurePolicy) {
                layoutPie(
                    size,
                    labelPositions,
                    labelConnectorTranslations.map { it?.first },
                    pieDiameter,
                    PieMeasurePolicy.PiePlaceables(piePlaceable, holePlaceable, labelPlaceables, connectorPlaceables)
                )
            }
        }
    }
}

/**
 * Creates a Pie Chart or, optionally, a Donut Chart if holeSize is nonZero, with optional
 * hole content to place at the center of the donut hole. Pie slices are drawn starting at
 * -90 degrees (top center), progressing clockwise around the pie. Each slice occupies a fraction
 * of the overall pie according to its data value relative to the sum of all values. Places labels around the pie
 * at a minimum distance set by [labelSpacing].
 *
 * @param values The data values for each pie slice
 * @param modifier Compose Modifiers to be applied to the overall PieChart
 * @param slice Composable for a pie slice.
 * @param label Composable for a pie slice label placed around the perimeter of the pie
 * @param labelConnector Composable for label connectors connecting the pie slice to the label
 * @param labelSpacing A value greater than 1 specifying the distance from the center of
 * the pie at which to place the labels relative to the overall diameter of the pie, where a value
 * of 1 is at the outer edge of the pie. Values between 1.05 and 1.4 tend to work well depending
 * on the size of the labels and overall pie diameter.
 * @param holeSize A relative size for an inner hole of the pie, creating a donut chart, with a
 * value between 0 and 1.
 * @param holeContent Optional content that may be placed in the space of the donut hole.
 * @param minPieDiameter Minimum diameter allowed for the pie.
 * @param maxPieDiameter Maximum diameter allowed for the pie. May be Infinity but not Unspecified.
 * @param forceCenteredPie If true, will force the pie to be centered within its parent, by adjusting (decreasing) the
 * pie size to accommodate label sizes and positions. If false, will maximize the pie diameter.
 * @param pieAnimationSpec Specifies the animation to use when the pie chart is first drawn.
 * @param labelAnimationSpec Specifies the animation to use when the labels are drawn. Drawing of the labels begins
 * after the pie animation is complete.
 */
@ExperimentalKoalaPlotApi
@Composable
public fun PieChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    slice: @Composable PieSliceScope.(Int) -> Unit = {
        val colors = remember(values.size) { generateHueColorPalette(values.size) }
        DefaultSlice(colors[it])
    },
    label: @Composable (Int) -> Unit = {},
    labelConnector: @Composable LabelConnectorScope.(Int) -> Unit = { StraightLineConnector() },
    labelSpacing: Float = DefaultLabelDiameterScale,
    holeSize: Float = 0f,
    holeContent: @Composable BoxScope.(contentPadding: PaddingValues) -> Unit = {},
    minPieDiameter: Dp = 100.dp,
    maxPieDiameter: Dp = 300.dp,
    forceCenteredPie: Boolean = false,
    pieAnimationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec,
    labelAnimationSpec: AnimationSpec<Float> = tween(LabelFadeInDuration, 0, LinearOutSlowInEasing),
) {
    PieChart(
        values = values,
        modifier = modifier,
        slice = slice,
        label = label,
        labelConnector = labelConnector,
        labelSpacing = labelSpacing,
        holeSize = holeSize,
        holeContent = holeContent,
        minPieDiameter = minPieDiameter,
        maxPieDiameter = maxPieDiameter,
        forceCenteredPie = forceCenteredPie,
        startAnimationUseCase =
        StartAnimationUseCase(
            executionType = StartAnimationUseCase.ExecutionType.Default,
            pieAnimationSpec,
            labelAnimationSpec,
        ),
    )
}

/**
 * Creates a Pie Chart or, optionally, a Donut Chart if holeSize is nonZero, with optional
 * hole content to place at the center of the donut hole. Pie slices are drawn starting at
 * -90 degrees (top center), progressing clockwise around the pie. Each slice occupies a fraction
 * of the overall pie according to its data value relative to the sum of all values. Places labels around the pie
 * at a minimum distance set by [labelSpacing].
 *
 * @param values The data values for each pie slice
 * @param modifier Compose Modifiers to be applied to the overall PieChart
 * @param slice Composable for a pie slice.
 * @param label Composable for a pie slice label placed around the perimeter of the pie
 * @param labelConnector Composable for label connectors connecting the pie slice to the label
 * @param labelSpacing A value greater than 1 specifying the distance from the center of
 * the pie at which to place the labels relative to the overall diameter of the pie, where a value
 * of 1 is at the outer edge of the pie. Values between 1.05 and 1.4 tend to work well depending
 * on the size of the labels and overall pie diameter.
 * @param holeSize A relative size for an inner hole of the pie, creating a donut chart, with a
 * value between 0 and 1.
 * @param holeContent Optional content that may be placed in the space of the donut hole.
 * @param minPieDiameter Minimum diameter allowed for the pie.
 * @param maxPieDiameter Maximum diameter allowed for the pie. May be Infinity but not Unspecified.
 * @param forceCenteredPie If true, will force the pie to be centered within its parent, by adjusting (decreasing) the
 * pie size to accommodate label sizes and positions. If false, will maximize the pie diameter.
 * @param startAnimationUseCase Controls the animation.
 */
@ExperimentalKoalaPlotApi
@Composable
public fun PieChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    slice: @Composable PieSliceScope.(Int) -> Unit = {
        val colors = remember(values.size) { generateHueColorPalette(values.size) }
        DefaultSlice(colors[it])
    },
    label: @Composable (Int) -> Unit = {},
    labelConnector: @Composable LabelConnectorScope.(Int) -> Unit = { StraightLineConnector() },
    labelSpacing: Float = DefaultLabelDiameterScale,
    holeSize: Float = 0f,
    holeContent: @Composable BoxScope.(contentPadding: PaddingValues) -> Unit = {},
    minPieDiameter: Dp = 100.dp,
    maxPieDiameter: Dp = 300.dp,
    forceCenteredPie: Boolean = false,
    startAnimationUseCase: StartAnimationUseCase =
        StartAnimationUseCase(
            executionType = StartAnimationUseCase.ExecutionType.Default,
            KoalaPlotTheme.animationSpec,
            tween(LabelFadeInDuration, 0, LinearOutSlowInEasing)
        ),
) {
    require(labelSpacing >= 1f) { "labelSpacing must be greater than 1" }
    PieChart(
        values,
        CircularLabelPositionProvider(labelSpacing),
        modifier,
        slice,
        label,
        labelConnector,
        holeSize,
        holeContent,
        minPieDiameter,
        maxPieDiameter,
        forceCenteredPie,
        startAnimationUseCase = startAnimationUseCase,
    )
}

@Composable
private fun HoverableElementAreaScope.Pie(
    internalPieData: List<PieSliceData>,
    slice: @Composable PieSliceScope.(Int) -> Unit,
    holeSize: Float,
) {
    val sliceScopes: List<PieSliceScope> = remember(internalPieData, holeSize) {
        buildList {
            internalPieData.forEach { sliceData ->
                add(
                    PieSliceScopeImpl(
                        sliceData,
                        holeSize,
                        InitOuterRadius,
                        this@Pie
                    )
                )
            }
        }
    }

    Layout(content = {
        BoxWithConstraints {
            with(LocalDensity.current) {
                val diameter = min(constraints.maxWidth, constraints.maxHeight)
                val sizeModifier = Modifier.size(diameter.toDp())
                Box(modifier = sizeModifier) {
                    internalPieData.forEachIndexed { index, _ ->
                        with(sliceScopes[index]) {
                            slice(index)
                        }
                    }
                }
            }
        }
    }) { measurables, constraints ->
        val chart = measurables[0].measure(constraints)

        layout(constraints.maxWidth, constraints.maxHeight) {
            chart.place(0, 0)
        }
    }
}

/**
 * A default pie chart slice implementation that can form full slices as well as slices
 * with a "hole" for donut charts.
 *
 * @receiver Provides drawing and interaction parameters for the slice scope
 * @param color The Color of the Slice
 * @param modifier The modifier to be applied to this item
 * @param border The border stroke for the Slice
 * @param hoverExpandFactor Amount the slice expands when hovered. 1 is no expansion, values greater
 * than 1 expand outward from the pie, and values less than 1 shrink. If expansion on hover is
 * desired, a good starting value is 1.05.
 * @param hoverElement Content to show when the mouse/pointer hovers over the slice
 * @param clickable If clicking should be enabled.
 * @param antiAlias Set to true if the slice should be drawn with anti-aliasing, false otherwise
 * @param gap Specifies the gap between slices. It is the angular distance, in degrees, between the
 * start/stop values the slice represents and where the slice is actually drawn.
 * @param onClick handler of clicks on the slice
 */
@ExperimentalKoalaPlotApi
@Composable
public fun PieSliceScope.DefaultSlice(
    color: Color,
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
    hoverExpandFactor: Float = 1.0f,
    hoverElement: @Composable () -> Unit = {},
    clickable: Boolean = false,
    antiAlias: Boolean = false,
    gap: Float = 0.0f,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val targetOuterRadius by animateFloatAsState(outerRadius * if (isHovered) hoverExpandFactor else 1f)

    val shape = Slice(
        pieSliceData.startAngle.toDegrees().value.toFloat() + gap,
        pieSliceData.angle.toDegrees().value.toFloat() - 2 * gap,
        innerRadius,
        targetOuterRadius
    )

    Box(
        modifier = modifier.fillMaxSize()
            .drawWithContent {
                drawIntoCanvas {
                    val path = (shape.createOutline(size, layoutDirection, this) as Outline.Generic).path

                    // draw slice
                    it.drawPath(
                        path,
                        Paint().apply {
                            isAntiAlias = antiAlias
                            this.color = color
                        }
                    )

                    if (border != null) {
                        it.withSaveLayer(Rect(Offset.Zero, size), Paint().apply { isAntiAlias = antiAlias }) {
                            val clip = Path().apply {
                                addRect(Rect(Offset.Zero, size))
                                op(this, path, PathOperation.Difference)
                            }

                            it.drawPath(
                                path,
                                Paint().apply {
                                    isAntiAlias = antiAlias
                                    strokeWidth = border.width.toPx()
                                    style = PaintingStyle.Stroke
                                    border.brush.applyTo(size, this, 1f)
                                }
                            )

                            // Remove part of border drawn outside of the slice bounds
                            it.drawPath(
                                clip,
                                Paint().apply {
                                    isAntiAlias = antiAlias
                                    blendMode = BlendMode.Clear
                                }
                            )
                        }
                    }
                }
                drawContent()
            }.clip(shape)
            .then(
                if (clickable) {
                    Modifier.clickable(
                        enabled = true,
                        role = Role.Button,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .hoverableElement(hoverElement)
            .hoverable(interactionSource)
    ) {}
}

/**
 * Creates a pie chart slice shape with a total angular extent of [angle] degrees with an
 * optional holeSize that is specified as a percentage of the overall slice radius.
 * The pie diameter is equal to the Shape's size width. The slice is positioned with its vertex
 * at the center.
 */
private class Slice(
    private val startAngle: Float,
    private val angle: Float,
    private val innerRadius: Float = 0.5f,
    private val outerRadius: Float = 1.0f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val radius = size.width / 2f * outerRadius
        val holeRadius = size.width / 2f * innerRadius
        val center = Offset(size.width / 2f, size.width / 2f)

        return Outline.Generic(
            Path().apply {
                if (angle == DegreesFullCircle && holeRadius != 0f) {
                    // Outer circle
                    addArc(
                        Rect(
                            size.width / 2f - radius,
                            size.width / 2f - radius,
                            size.width / 2f + radius,
                            size.width / 2f + radius
                        ),
                        startAngle,
                        angle
                    )
                    // Inner circle
                    addArc(
                        Rect(
                            size.width / 2f - holeRadius,
                            size.width / 2f - holeRadius,
                            size.width / 2f + holeRadius,
                            size.width / 2f + holeRadius
                        ),
                        startAngle,
                        -angle
                    )
                } else {
                    // First line segment from start point to first outer corner
                    moveTo(center + polarToCartesian(holeRadius, startAngle.deg))
                    lineTo(center + polarToCartesian(radius, startAngle.deg))

                    // Outer arc
                    addArc(
                        Rect(
                            size.width / 2f - radius,
                            size.width / 2f - radius,
                            size.width / 2f + radius,
                            size.width / 2f + radius
                        ),
                        startAngle,
                        angle
                    )

                    // Line from second outer corner to inner corner or center
                    lineTo(center + polarToCartesian(holeRadius, (startAngle + angle).deg))

                    if (holeRadius != 0f) {
                        // Inner arc
                        arcTo(
                            Rect(
                                size.width / 2f - holeRadius,
                                size.height / 2f - holeRadius,
                                size.width / 2f + holeRadius,
                                size.height / 2f + holeRadius
                            ),
                            startAngle + angle,
                            -angle,
                            false,
                        )
                    }
                }
            }
        )
    }
}

/**
 * A label connector that uses a Bezier curve.
 *
 * @param connectorColor The color of the connector line.
 * @param connectorStroke The stroke used to draw the connector line.
 */
@Composable
public fun LabelConnectorScope.BezierLabelConnector(
    modifier: Modifier = Modifier,
    connectorColor: Color = MaterialTheme.colorScheme.onBackground,
    connectorStroke: Stroke = Stroke(width = 1f)
) {
    val length by remember(startPosition.value, endPosition.value) {
        val delta = startPosition.value - endPosition.value
        mutableStateOf(sqrt(delta.x.pow(2) + delta.y.pow(2)))
    }

    val path = Path().apply {
        moveTo(startPosition.value)

        // control point 1
        val cp1 = startPosition.value + polarToCartesian(length / 2, startAngle.value)

        // control point 2
        val cp2 = endPosition.value + polarToCartesian(length / 2, endAngle.value)

        cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, endPosition.value.x, endPosition.value.y)
    }

    Box(
        modifier = modifier.fillMaxSize().drawBehind {
            drawPath(
                path = path,
                color = connectorColor,
                style = connectorStroke
            )
        }
    ) {}
}

/**
 * A label connector that uses a straight line.
 *
 * @param connectorColor The color of the connector line.
 * @param connectorStroke The stroke used to draw the connector line.
 */
@Composable
public fun LabelConnectorScope.StraightLineConnector(
    modifier: Modifier = Modifier,
    connectorColor: Color = MaterialTheme.colorScheme.onBackground,
    connectorStroke: Stroke = Stroke(width = 1f)
) {
    val path = Path().apply {
        moveTo(startPosition.value)
        lineTo(endPosition.value)
    }
    Box(
        modifier = modifier.fillMaxSize().drawBehind {
            drawPath(path = path, color = connectorColor, style = connectorStroke)
        }
    ) {}
}
