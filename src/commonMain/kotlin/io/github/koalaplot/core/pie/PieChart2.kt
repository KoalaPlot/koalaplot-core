package io.github.koalaplot.core.pie

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.koalaplot.core.animation.StartAnimationUseCase
import io.github.koalaplot.core.style.KoalaPlotTheme
import io.github.koalaplot.core.util.AngularValue
import io.github.koalaplot.core.util.DegreesFullCircle
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.util.circumscribedSquareSize
import io.github.koalaplot.core.util.deg
import io.github.koalaplot.core.util.generateHueColorPalette
import kotlin.math.min

/**
 * Scope for configuring a [PieChart].
 */
@ExperimentalKoalaPlotApi
public interface PieChartScope {
    /**
     * Adds an item to the [PieChart].
     * @param value The numerical value of the slice. The slice's angular extent will be proportional to this value
     * relative to the sum of all values in the chart.
     * @param key A stable, unique key representing this item. Using stable keys ensures that animations (like value
     * transitions and initial grow) work correctly when items are added, removed, or updated.
     * Defaults to the item's index.
     * @param label Composable for the slice label.
     * @param connector Composable for the label connector. Defaults to [StraightLineConnector].
     * @param slice Composable to customize the slice appearance. If null, a [DefaultSlice] with an
     * automatically generated color will be used.
     */
    public fun item(
        value: Float,
        key: Any? = null,
        label: @Composable () -> Unit = {},
        connector: @Composable LabelConnectorScope.() -> Unit = { StraightLineConnector() },
        slice: (@Composable PieSliceScope.() -> Unit)? = null,
    )
}

internal class PieChartItem(
    val value: Float,
    val key: Any,
    val label: @Composable () -> Unit,
    val connector: @Composable LabelConnectorScope.() -> Unit,
    val slice: (@Composable PieSliceScope.() -> Unit)?,
)

@OptIn(ExperimentalKoalaPlotApi::class)
private class PieChartScopeImpl : PieChartScope {
    val items = mutableListOf<PieChartItem>()

    override fun item(
        value: Float,
        key: Any?,
        label: @Composable () -> Unit,
        connector: @Composable LabelConnectorScope.() -> Unit,
        slice: (@Composable PieSliceScope.() -> Unit)?,
    ) {
        val fallbackKey = items.size
        items.add(PieChartItem(value, key ?: fallbackKey, label, connector, slice))
    }
}

/**
 * Creates a Pie Chart or, optionally, a Donut Chart if [holeSize] is non-zero, with optional
 * [holeContent] to place at the center of the donut hole.
 *
 * This version of [PieChart] uses a DSL-based approach via [PieChartScope] to define chart items.
 * It features smooth transitions for both slice values and the overall pie diameter when the data
 * or the parent container size changes.
 *
 * Pie slices are drawn starting at [pieStartAngle], progressing clockwise around the pie.
 * Each slice occupies a fraction of the overall pie according to its [PieChartScope.item] value
 * relative to the sum of all values.
 *
 * @param modifier Compose Modifiers to be applied to the overall PieChart.
 * @param labelPositionProvider A provider of label offsets that can be used to implement different label
 * placement strategies. Defaults to [CircularLabelPositionProvider].
 * @param holeSize A relative size for an inner hole of the pie, creating a donut chart, with a
 * value between 0 and 1.
 * @param holeContent Optional content that may be placed in the space of the donut hole. To safely draw the content
 * within the hole without intersecting with the chart, apply the passed `contentPadding` to your content composable.
 * @param minPieDiameter Minimum diameter allowed for the pie.
 * @param maxPieDiameter Maximum diameter allowed for the pie. May be Infinity but not Unspecified.
 * @param centeredAlignment If true, will center the pie within its parent, by adjusting (decreasing) the
 * pie size to accommodate label sizes and positions. If false, will maximize the pie diameter and place it in the top-left most corner
 * of the parent. For the best visual experience when animating the pie due to slice value changes, set centeredAlignment to true.
 * @param startAnimationUseCase Controls the initial grow and fade-in animations of the chart.
 * @param pieStartAngle Sets an angle for the pie data to start at. Defaults to the top center of the pie.
 * @param pieExtendAngle Sets a max angle for the pie to extend to, with a value between 1 and 360 degrees.
 * Defaults to [DegreesFullCircle].
 * @param content A DSL block to define the items in the pie chart.
 */
@ExperimentalKoalaPlotApi
@Composable
public fun PieChart(
    modifier: Modifier = Modifier,
    labelPositionProvider: LabelPositionProvider = CircularLabelPositionProvider(DefaultLabelDiameterScale),
    holeSize: Float = 0f,
    holeContent: @Composable BoxScope.(contentPadding: PaddingValues) -> Unit = {},
    minPieDiameter: Dp = 100.dp,
    maxPieDiameter: Dp = 300.dp,
    centeredAlignment: Boolean = true,
    startAnimationUseCase: StartAnimationUseCase = StartAnimationUseCase(
        executionType = StartAnimationUseCase.ExecutionType.Default,
        KoalaPlotTheme.animationSpec,
        tween(LabelFadeInDuration, 0, LinearOutSlowInEasing),
    ),
    pieStartAngle: AngularValue = AngleCCWTop.deg,
    pieExtendAngle: AngularValue = DegreesFullCircle.deg,
    content: PieChartScope.() -> Unit,
) {
    require(holeSize in (0f..1f)) { "holeSize must be between 0 and 1" }
    require(maxPieDiameter != Dp.Unspecified) { "maxPieDiameter cannot be Unspecified" }

    val pieItems = PieChartScopeImpl().apply(content).items
    val colors = remember(pieItems.size) { generateHueColorPalette(pieItems.size) }
    val state = rememberPieChartState(pieItems, startAnimationUseCase, pieStartAngle, pieExtendAngle)

    val targetDiameterState = remember { mutableFloatStateOf(0f) }
    val animatedDiameter by animateFloatAsState(
        targetValue = targetDiameterState.value,
        animationSpec = KoalaPlotTheme.animationSpec,
        label = "pieDiameterAnimation",
    )

    SubcomposeLayout(modifier = modifier.clipToBounds()) { constraints ->
        val pieMeasurePolicy = PieMeasurePolicy(
            state.finalPieSliceData,
            holeSize,
            labelPositionProvider,
            InitOuterRadius,
            centeredAlignment,
        )

        val pieMeasurable = subcomposePie(state.pieSliceData, pieItems, holeSize, colors)
        val labelMeasurables = subcomposeLabels(pieItems, state.labelAlpha)

        val (pieDiameter, labelPlaceables) = pieMeasurePolicy.measure(
            labelMeasurables,
            constraints,
            minPieDiameter.toPx(),
            maxPieDiameter.toPx(),
        )

        targetDiameterState.value = pieDiameter

        val effectivePieDiameter = if (state.beta < 1f) {
            animatedDiameter
        } else {
            if (animatedDiameter == 0f) {
                pieDiameter
            } else {
                // Allow it to be larger than target (to shrink) but not larger than window (to avoid clipping)
                min(animatedDiameter, min(constraints.maxWidth, constraints.maxHeight).toFloat())
            }
        }

        val effectivePiePlaceable = pieMeasurable.measure(
            Constraints.fixed(effectivePieDiameter.toInt(), effectivePieDiameter.toInt()),
        )

        val labelPositions = labelPositionProvider.computeLabelPositions(
            effectivePieDiameter * InitOuterRadius,
            holeSize,
            labelPlaceables,
            state.pieSliceData,
        )

        val size = pieMeasurePolicy.computeSize(labelPlaceables, labelPositions, effectivePieDiameter, constraints)

        val labelConnectorTranslations =
            pieMeasurePolicy.computeLabelConnectorScopes(labelPositions, effectivePieDiameter, state.pieSliceData)

        val holePlaceable = subcomposeHole(effectivePieDiameter, holeSize, holeContent)
        val connectorPlaceables = subcomposeConnectors(pieItems, state.labelAlpha, labelConnectorTranslations)

        with(pieMeasurePolicy) {
            layoutPie(
                size,
                labelPositions,
                labelConnectorTranslations.map { it?.first },
                effectivePieDiameter,
                PieMeasurePolicy.PiePlaceables(
                    effectivePiePlaceable,
                    holePlaceable,
                    labelPlaceables,
                    connectorPlaceables,
                ),
            )
        }
    }
}

private class PieChartState(
    val pieSliceData: List<PieSliceData>,
    val finalPieSliceData: List<PieSliceData>,
    val labelAlpha: Float,
    val beta: Float,
)

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
private fun rememberPieChartState(
    pieItems: List<PieChartItem>,
    startAnimationUseCase: StartAnimationUseCase,
    pieStartAngle: AngularValue,
    pieExtendAngle: AngularValue,
): PieChartState {
    val currentPieItems by rememberUpdatedState(pieItems)
    val itemKeys = remember(pieItems) { pieItems.map { it.key } }
    startAnimationUseCase(key = itemKeys)

    val beta = remember(itemKeys) { startAnimationUseCase.animatables[0] }
    val labelAlpha = remember(itemKeys) { startAnimationUseCase.animatables[1] }

    val valueAnimatables = remember { mutableMapOf<Any, Animatable<Float, AnimationVector1D>>() }

    val animationSpec = KoalaPlotTheme.animationSpec
    pieItems.forEach { item ->
        val animatable = valueAnimatables.getOrPut(item.key) {
            Animatable(if (beta.value < 1f) item.value else 0f)
        }
        LaunchedEffect(item.value, animationSpec) {
            animatable.animateTo(item.value, animationSpec)
        }
    }

    SideEffect {
        val currentKeys = pieItems.asSequence().map { it.key }.toSet()
        valueAnimatables.keys.retainAll(currentKeys)
    }

    val pieSliceData by remember(pieStartAngle, pieExtendAngle) {
        derivedStateOf {
            val values = currentPieItems.map { item ->
                valueAnimatables[item.key]?.value ?: item.value
            }
            makePieSliceData(values, beta.value, pieStartAngle, pieExtendAngle)
        }
    }

    val finalPieSliceData by remember(pieStartAngle, pieExtendAngle) {
        derivedStateOf {
            val values = currentPieItems.map { it.value }
            makePieSliceData(values, 1f, pieStartAngle, pieExtendAngle)
        }
    }

    return PieChartState(pieSliceData, finalPieSliceData, labelAlpha.value, beta.value)
}

@OptIn(ExperimentalKoalaPlotApi::class)
private fun androidx.compose.ui.layout.SubcomposeMeasureScope.subcomposePie(
    pieSliceData: List<PieSliceData>,
    pieItems: List<PieChartItem>,
    holeSize: Float,
    colors: List<androidx.compose.ui.graphics.Color>,
): androidx.compose.ui.layout.Measurable = subcompose("pie") {
    Pie(
        pieSliceData,
        { index ->
            val item = pieItems[index]
            if (item.slice != null) {
                item.slice.invoke(this)
            } else {
                DefaultSlice(colors[index])
            }
        },
        holeSize,
    )
}[0]

@OptIn(ExperimentalKoalaPlotApi::class)
private fun androidx.compose.ui.layout.SubcomposeMeasureScope.subcomposeLabels(
    pieItems: List<PieChartItem>,
    labelAlpha: Float,
): List<androidx.compose.ui.layout.Measurable> = subcompose("labels") {
    pieItems.forEach { item ->
        Box(modifier = Modifier.alpha(labelAlpha)) {
            item.label()
        }
    }
}

@OptIn(ExperimentalKoalaPlotApi::class)
private fun androidx.compose.ui.layout.SubcomposeMeasureScope.subcomposeHole(
    pieDiameter: Float,
    holeSize: Float,
    holeContent: @Composable BoxScope.(contentPadding: PaddingValues) -> Unit,
): androidx.compose.ui.layout.Placeable {
    val holeDiameter = (pieDiameter * holeSize)
    val holeSafeEdgeLength = circumscribedSquareSize(holeDiameter)
    return subcompose("hole") {
        Box(modifier = Modifier.clip(CircleShape)) {
            holeContent(PaddingValues(((holeDiameter - holeSafeEdgeLength) / 2.0f).toDp()))
        }
    }[0].measure(Constraints.fixed(holeDiameter.toInt(), holeDiameter.toInt()))
}

@OptIn(ExperimentalKoalaPlotApi::class)
private fun androidx.compose.ui.layout.SubcomposeMeasureScope.subcomposeConnectors(
    pieItems: List<PieChartItem>,
    labelAlpha: Float,
    labelConnectorTranslations: List<Pair<androidx.compose.ui.geometry.Offset, LabelConnectorScope>?>,
): List<androidx.compose.ui.layout.Placeable> = subcompose("connectors") {
    pieItems.forEachIndexed { index, item ->
        Box(modifier = Modifier.fillMaxSize().alpha(labelAlpha)) {
            labelConnectorTranslations.getOrNull(index)?.let { (_, scope) ->
                item.connector.invoke(scope)
            }
        }
    }
}.map { it.measure(Constraints()) }
