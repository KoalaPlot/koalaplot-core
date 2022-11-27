package io.github.koalaplot.core.pie

import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import io.github.koalaplot.core.theme.KoalaPlotTheme
import io.github.koalaplot.core.util.DegreesFullCircle
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.util.HoverableElementArea
import io.github.koalaplot.core.util.HoverableElementAreaScope
import io.github.koalaplot.core.util.circumscribedSquareSize
import io.github.koalaplot.core.util.generateHueColorPalette
import io.github.koalaplot.core.util.lineTo
import io.github.koalaplot.core.util.moveTo
import io.github.koalaplot.core.util.pol2Cart
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

private const val DefaultLabelDiameterScale = 1.1f

internal data class PieSliceData(
    val startAngle: Float,
    val angleExtent: Float,
    val interactionSource: MutableInteractionSource = MutableInteractionSource()
)

private fun makePieSliceData(
    data: List<Float>,
    beta: Float
): List<PieSliceData> {
    val total = data.sumOf {
        it.toDouble()
    }.toFloat()

    return buildList {
        var startAngle = AngleCCWTop
        for (i in data.indices) {
            val extent = data[i] / total * DegreesFullCircle * beta
            add(PieSliceData(startAngle, extent))
            startAngle += extent
        }
    }
}

/**
 * Scope for Pie slices.
 * @property startAngle The angle where the slice starts
 * @property angle The angular width of the slice
 * @property innerRadius The inside radius of the slice, as a fraction of its constraint's width,
 * must be between 0 and 1 inclusive
 * @property outerRadius The outside radius of the slice, as a fraction of its constraint's width,
 * must be between 0 and 1 inclusive
 */
public interface PieSliceScope : HoverableElementAreaScope {
    public val startAngle: Float
    public val angle: Float
    public val innerRadius: Float
    public val outerRadius: Float
}

private data class PieSliceScopeImpl(
    override val startAngle: Float,
    override val angle: Float,
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
    public val startAngle: MutableState<Float>

    /**
     * The angle of the label at the end of the connector. This is an angle that is normal to the
     * connector's bounding box at the [endPosition]
     */
    public val endAngle: MutableState<Float>
}

internal data class LabelConnectorScopeImpl(
    override val startPosition: MutableState<Offset> = mutableStateOf(Offset.Zero),
    override val endPosition: MutableState<Offset> = mutableStateOf(Offset.Zero),
    override val startAngle: MutableState<Float> = mutableStateOf(0f),
    override val endAngle: MutableState<Float> = mutableStateOf(0f)
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
 * @param animationSpec Specifies the animation to use when the pie chart is first drawn.
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
    holeContent: @Composable () -> Unit = {},
    minPieDiameter: Dp = 100.dp,
    maxPieDiameter: Dp = 300.dp,
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec
) {
    require(holeSize in 0f..1f) { "holeSize must be between 0 and 1" }
    require(labelSpacing >= 1f) { "labelSpacing must be greater than 1" }
    require(maxPieDiameter != Dp.Unspecified) { "maxPieDiameter cannot be Unspecified" }

    val beta = remember(values) { Animatable(0f) }
    val labelAlpha = remember(values) { Animatable(0f) }

    LaunchedEffect(values) {
        beta.animateTo(1f, animationSpec = animationSpec)
        // fade in labels after pie animation is complete
        labelAlpha.animateTo(
            1f,
            animationSpec = tween(LabelFadeInDuration, 0, LinearOutSlowInEasing)
        )
    }

    // pieSliceData that gets animated - used for drawing the pie
    val pieSliceData = remember(values, beta.value) { makePieSliceData(values, beta.value) }

    // pieSliceData when the animation is complete - used for sizing & label layout/positioning
    val finalPieSliceData = remember(values) { makePieSliceData(values, 1f) }

    val pieMeasurePolicy = remember(finalPieSliceData, holeSize, labelSpacing, minPieDiameter) {
        PieMeasurePolicy(finalPieSliceData, labelSpacing, InitOuterRadius)
    }

    HoverableElementArea(modifier = modifier) {
        SubcomposeLayout(modifier = Modifier.clipToBounds()) { constraints ->
            val pieMeasurable = subcompose("pie") { Pie(pieSliceData, slice, holeSize) }[0]

            val labelMeasurables = subcompose("labels") {
                pieSliceData.indices.forEach {
                    // Wrapping in box ensures there is 1 measurable element
                    // emitted per label & applies fade animation
                    Box(modifier = Modifier.alpha(labelAlpha.value)) {
                        label(it)
                    }
                }
            }

            val (pieDiameter, piePlaceable, labelPlaceables) = pieMeasurePolicy.measure(
                pieMeasurable,
                labelMeasurables,
                constraints,
                minPieDiameter.toPx(),
                maxPieDiameter.toPx()
            )

            val labelOffsets = pieMeasurePolicy.computeLabelOffsets(pieDiameter, labelPlaceables)

            val size = pieMeasurePolicy.computeSize(labelPlaceables, labelOffsets.map { it.position }, pieDiameter)
                .run {
                    // add one due to later float to int conversion dropping the fraction part
                    copy(
                        (width + 1).coerceAtMost(constraints.maxWidth.toFloat()),
                        (height + 1).coerceAtMost(constraints.maxHeight.toFloat())
                    )
                }

            val labelConnectorTranslations = pieMeasurePolicy.computeLabelConnectorScopes(labelOffsets, pieDiameter)

            val holeEdgeLength =
                circumscribedSquareSize(pieDiameter * holeSize.toDouble()).toInt()
            val holePlaceable = subcompose("hole") {
                Box { holeContent() } // wrap in box to ensure 1 and only 1 element emitted
            }[0].measure(Constraints.fixed(holeEdgeLength, holeEdgeLength))

            val connectorPlaceables = subcompose("connectors") {
                pieSliceData.indices.forEach {
                    Box(modifier = Modifier.fillMaxSize().alpha(labelAlpha.value)) {
                        with(labelConnectorTranslations[it].second) {
                            labelConnector(it)
                        }
                    }
                }
            }.map { it.measure(constraints) }

            with(pieMeasurePolicy) {
                layoutPie(
                    size,
                    labelOffsets,
                    labelConnectorTranslations.map { it.first },
                    pieDiameter,
                    PieMeasurePolicy.PiePlaceables(
                        piePlaceable,
                        holePlaceable,
                        labelPlaceables,
                        connectorPlaceables
                    )
                )
            }
        }
    }
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
                        sliceData.startAngle,
                        sliceData.angleExtent,
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
        startAngle + gap,
        angle - 2 * gap,
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
                    moveTo(center + pol2Cart(holeRadius, startAngle))
                    lineTo(center + pol2Cart(radius, startAngle))

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
                    lineTo(center + pol2Cart(holeRadius, startAngle + angle))

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
    connectorColor: Color = MaterialTheme.colors.onBackground,
    connectorStroke: Stroke = Stroke(width = 1f)
) {
    val length by remember(startPosition.value, endPosition.value) {
        val delta = startPosition.value - endPosition.value
        mutableStateOf(sqrt(delta.x.pow(2) + delta.y.pow(2)))
    }

    val path = Path().apply {
        moveTo(startPosition.value)

        // control point 1
        val cp1 = startPosition.value + pol2Cart(length / 2, startAngle.value)

        // control point 2
        val cp2 = endPosition.value + pol2Cart(length / 2, endAngle.value)

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
    connectorColor: Color = MaterialTheme.colors.onBackground,
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
