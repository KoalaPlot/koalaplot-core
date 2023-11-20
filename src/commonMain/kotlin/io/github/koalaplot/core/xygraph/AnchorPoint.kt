package io.github.koalaplot.core.xygraph

public sealed class AnchorPoint {
    public data object TopLeft : AnchorPoint()
    public data object TopCenter : AnchorPoint()
    public data object TopRight : AnchorPoint()
    public data object RightMiddle : AnchorPoint()
    public data object BottomRight : AnchorPoint()
    public data object BottomCenter : AnchorPoint()
    public data object BottomLeft : AnchorPoint()
    public data object LeftMiddle : AnchorPoint()
    public data object Center : AnchorPoint()
}
