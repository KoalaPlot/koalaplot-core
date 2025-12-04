package io.github.koalaplot.core.bar

import io.github.koalaplot.core.util.HoverableElementAreaScope

public interface BarScope : HoverableElementAreaScope

internal class BarScopeImpl(
    val hoverableElementAreaScope: HoverableElementAreaScope,
) : BarScope,
    HoverableElementAreaScope by hoverableElementAreaScope
