package io.github.koalaplot.core.bar

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import io.github.koalaplot.core.style.KoalaPlotTheme
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.LinearAxisModel
import kotlin.math.roundToInt

/**
 * A vertical layout of multiple bullet graphs. Each bullet will equally share the vertical space and be the same
 * height. The left and right edges of all bullets will be aligned horizontally.
 *
 * Bullet graphs are implemented as defined in
 *  * http://www.perceptualedge.com/articles/misc/Bullet_Graph_Design_Spec.pdf.
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

    if (graphScope.scopes.isEmpty()) {
        return
    }

    val builders = graphScope.scopes.mapIndexed { index, scope ->
        scope.createBulletGraphBuilder(index)
    }

    SubcomposeLayout(modifier = modifier) { constraints ->
        val bulletHeight =
            ((constraints.maxHeight - gap.roundToPx() * graphScope.scopes.size - 1) / graphScope.scopes.size)
                .coerceAtLeast(0)

        builders.forEach { it.bulletHeight = bulletHeight }

        val labelWidthMaxConstraint = calculateLabelWidthMaxConstraint(graphScope, constraints)

        builders.forEach { it.measureLabel(this, labelWidthMaxConstraint) }

        val labelWidth =
            calculateLabelWidth(graphScope, labelWidthMaxConstraint, builders.map { it.labelPlaceable!! })

        builders.forEach { it.measureAxisLabels(this, constraints.maxWidth - labelWidth) }

        val firstAxisLabelWidth = builders.maxOf { it.axisLabelPlaceables!!.first().width }
        val lastAxisLabelWidth = builders.maxOf { it.axisLabelPlaceables!!.last().width }
        val rangeWidth =
            (constraints.maxWidth - firstAxisLabelWidth / 2 - lastAxisLabelWidth / 2 - labelWidth).coerceAtLeast(0)

        builders.forEach {
            it.measureAxis(this, rangeWidth)
            it.measureRanges(this, rangeWidth)
            it.measureFeature(this, rangeWidth, animationSpec)
            it.measureComparativeMeasures(this)
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
    val labelWidthMaxConstraint = when (val labelWidth = graphScope.labelWidth) {
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
    val labelWidth = graphScope.labelWidth

    return if (labelWidth is VariableFraction) {
        labelPlaceable.maxOf { it.width }
    } else {
        labelWidthMaxConstraint
    }
}

/**
 * A scope for constructing displays that layout and align features among multiple bullet graphs.
 */
@BulletGraphDslMarker
public data class BulletGraphScope
@OptIn(ExperimentalKoalaPlotApi::class)
constructor(
    internal val scopes: MutableList<BulletBuilderScope<*>> = mutableListOf(),
    public var labelWidth: LabelWidth = VariableFraction(@Suppress("MagicNumber") 0.25f),
) {
    /**
     * Configures a bullet graph.
     *
     * @param axisModel Specifies the LinearAxisModel to use for the bullet.
     * @param builder Callback function used to configure the bullet graph.
     */
    @OptIn(ExperimentalKoalaPlotApi::class)
    public fun <T> bullet(
        axisModel: LinearAxisModel<T>,
        builder: BulletBuilderScope<T>.() -> Unit
    ) where T : Comparable<T>, T : Number {
        val scope = BulletBuilderScope(axisModel)
        scope.builder()
        scopes.add(scope)
    }
}
