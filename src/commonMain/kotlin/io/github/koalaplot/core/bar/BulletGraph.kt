package io.github.koalaplot.core.bar

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
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
    builder: BulletGraphScope.() -> Unit,
) {
    val graphScope = remember(builder) { BulletGraphScope().apply(builder) }

    if (graphScope.scopes.isEmpty()) {
        return
    }

    val items = graphScope.scopes.map { scope -> key(scope) { scope.generateBulletGraphItem() } }

    SubcomposeLayout(modifier = modifier) { constraints ->
        val bulletHeight =
            (
                (constraints.maxHeight - gap.roundToPx() * graphScope.scopes.size - 1) /
                    graphScope.scopes.size.coerceAtLeast(1)
            ).coerceAtLeast(0)

        // Measure labels
        val labelWidthMaxConstraint = calculateLabelWidthMaxConstraint(graphScope.labelWidth, constraints)
        val labelPlaceables = items.map { it.measureLabel(this, Constraints(maxWidth = labelWidthMaxConstraint, maxHeight = bulletHeight)) }
        val labelWidth = calculateLabelWidth(graphScope.labelWidth, labelWidthMaxConstraint, labelPlaceables)

        // Measure Axis labels
        val graphAreaWidth = constraints.maxWidth - labelWidth
        val axisMeasurements = items.map { it.measureAxisLabels(this, graphAreaWidth, bulletHeight) }

        val firstAxisLabelWidth = axisMeasurements.maxOf { it.labelPlaceables.first().width }
        val lastAxisLabelWidth = axisMeasurements.maxOf { it.labelPlaceables.last().width }
        val rangeWidth = (graphAreaWidth - firstAxisLabelWidth / 2 - lastAxisLabelWidth / 2).coerceAtLeast(0)
        val rangeStart = labelWidth + firstAxisLabelWidth / 2

        // Measure main content
        val contentMeasurements = items.mapIndexed { index, item ->
            item.measureContent(this, axisMeasurements[index], rangeWidth, bulletHeight, animationSpec)
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            var yPos = 0

            val layoutPosition = BulletLayoutPosition(
                labelWidth = labelWidth,
                rangeStart = rangeStart,
                rangeWidth = rangeWidth,
                yPos = yPos,
                bulletHeight = bulletHeight,
            )

            items.forEachIndexed { index, _ ->
                layoutBullet(
                    layoutPosition.copy(yPos = yPos),
                    labelPlaceable = labelPlaceables[index],
                    axisMeasurement = axisMeasurements[index],
                    bulletContentMeasurement = contentMeasurements[index],
                )
                yPos += bulletHeight + gap.roundToPx()
            }
        }
    }
}

@OptIn(ExperimentalKoalaPlotApi::class)
private fun Density.calculateLabelWidthMaxConstraint(
    labelWidth: LabelWidth,
    constraints: Constraints,
): Int {
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
    labelWidth: LabelWidth,
    labelWidthMaxConstraint: Int,
    labelPlaceable: List<Placeable>,
): Int = if (labelWidth is VariableFraction) {
    labelPlaceable.maxOf { it.width }
} else {
    labelWidthMaxConstraint
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
            builder: BulletBuilderScope<T>.() -> Unit,
        ) where T : Comparable<T>, T : Number {
            val scope = BulletBuilderScope(axisModel)
            scope.builder()
            scopes.add(scope)
        }
    }
