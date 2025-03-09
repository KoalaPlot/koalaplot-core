# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Support for StartAnimationUseCase in bar-chart (GroupedVerticalBarPlot)
- Ability to disable the consumption of gesture events
- Pan fling behavior for Android and iOS platforms (#106)

### Changed

- Move the logic of enabling/disabling pan and zoom to the gesture handler
- A separate object has been created for the gesture configuration GestureConfig
- Removing the gesture logic "pastTouchSlop", in practice it turned out to be inconsistent when capturing panning
  from the parent container

### Fixed

- Discrete panning in X and Y axes (#104)
- Fix an error in the logic of setting the initial zoom direction,
  which is why the orthogonal axis of the first touch movement was not reset

## [0.8.0]

### Added

- viewRange property to LinearAxisModel.
- Support for "inverted" linear axes that plot their values from most to least.
- Support for StartAnimationUseCase in PieChart

### Changed

- Upgrade Compose plugin to 1.7.3
- Upgrade Kotlin to 2.1.0
- Upgrade kotlinx-coroutines to 1.10.1
- Upgrade Dokka to 2.0.0
- Android Gradle Plugin to 8.7.2
- Android compileSDK to 34 and minSDK to 23
- BulletGraph using default qualitative range indicators will accept a max of 5 values when being configured rather
  than throwing a null pointer exception when Composing.

### Fixed

- Panning support for Int and Long linear axis models by introducing ContinuousLinearAxisModel and
  DiscreteLinearAxisModel interfaces
- BulletGraph null pointer exception if more than 4 qualitative range indicators are used.
- BulletGraph qualitative range default shading not according to spec under some conditions.
- Divide by 0 in XYGraph if an axis has no tick labels (#105).
- XYGraph size changes when panning (#101). **Important Note**: This required a change to the XYGraph size
  calculation that may result in clipping of the rightmost x-axis tick label and/or topmost y-axis tick label. Padding
  can be explicitly added to the parent Composable to leave space for labels that extend beyond the graph edge.

## [0.7.1]

### Fixed

- PieChart causing array index out of bounds exceptions if values were added/removed after initial composition.

## [0.7.0]

### Added

- setViewRange function on LinearAxisModel interface.
- zoom and pan support for all platforms within XYGraph
- labelAnimationSpec parameter for PieCharts

### Changed

- Renamed ILinearAxisModel to LinearAxisModel
- Removed minimumMajorTickSpacing from AxisModel interface and added to LinearAxisModel
- Upgrade Kotlin to 2.0.21
- Upgrade com.android.library to 8.5.2
- Upgrade gradle wrapper to 8.10
- Upgrade Compose to 1.7.0
- Upgrade coroutines to 1.9.0
- Rename PieChart's animationSpec parameter to pieAnimationSpec
- zoomRangeLimit in LinearAxisModel implementations replaced with minViewExtent and maxViewExtent to control both the
  allowed upper and lower bounds of the visible range

### Fixed

- #81 Add coerceAtLeast to rememberIntLinearAxisModel to fix Zoom range limit when range is small
- BulletGraph vertical centering of ranges

### Removed

- Removed deprecated LinearAxisModel class and rememberLinearAxisModel function

## [0.6.4]

### Fixed

- Added guards to avoid division by 0 in multiple places.

## [0.6.3]

### Fixed

- XYGraph didn't fill all available width when x-axis label intrinsic widths are greater than tick spacing

## [0.6.2]

### Fixed

- XYGraph wouldn't zoom properly if recomposed with modified axis models.

### Changed

- Upgrade versions: Kotlin to 2.0.0, coroutines to 1.8.1, compose to 1.6.11, dokka to 1.9.20, detekt to 1.23.6
- ZoomRangeLimitDefault applied to Int and Long linear axis models to be consistent with the original
  FloatLinearAxisModel

## [0.6.1]

### Changed

- VerticalBarPlot scales bar width by number of x-axis major ticks when there is only 1 data point.

## [0.6.0]

### Changed

- Replaced LinearAxisModel (deprecated) with FloatLinearAxisModel
- BulletGraphs can use any linear axis model type

### Fixed

- autoScaleRange throws Exception if provide an empty list
- ArithmeticException if linear axis minimumMajorTickIncrement is less than the range extent.
- publishToMavenLocal on Mac will publish all artifacts, not just iOS

### Added

- StairstepPlot variant that accepts a LineStyle for each y-axis value.
- PieChart variant that takes a labelPositionProvider to allow pluggable label placement algorithms.
- LongLinearAxisModel, DoubleLinearAxisModel, & IntLinearAxisModel
- Ability to place labels inside PieChart slices.

### Removed

- BulletGraph function that deferred to BulletGraphs
- labelWidth from BulletBuilderScope, use BulletGraphScope.labelWidth instead
- io.github.koalaplot.core.xychart.* classes and functions which were deprecated in 0.5.0

## [0.5.4]

### Fixed

- VerticalBarPlot can't be updated (#40, #52)

## [0.5.3]

### Changed

- Update Compose to version 1.6.0

## [0.5.2]

### Changed

- Update Compose to version 1.6.0-beta01.
- Update Kotlin to version 1.9.22

## [0.5.1]

### Fixed

- @Deprecated ReplaceWith using incorrect replacement names

## [0.5.0]

### Changed

- Update to Compose version 1.6.0-alpha01
- Update Kotlin version to 1.9.21
- Moved package for AreaStyle, LineStyle, Point, DefaultPoint, and KoalaPlotTheme
- Line/Area chart scaling applied to paths instead of canvas so PathEffects scale the same as drawing on an unscaled
  Canvas
- Deprecated XYChart and renamed to XYGraph - behavior and breaking changes introduced into XYGraph (see below)
- Deprecated LineChart, StairStepChart, and StackedAreaChart and renamed to LinePlot, StairStepPlot, and AreaPlot
- Separated variants of VerticalBarChart, depending on grouping or stacking, into GroupedVerticalBarPlot,
  VerticalBarPlot, and StackedVerticalBarPlot
- Area plots no longer require LineStyle to be non-null
- Separate AreaPlot from LinePlot to simplify null/not-null requirements on parameters to LinePlot
- X and Y-axis titles in XYGraph fill entire width/height of plot so user can choose to align content along the axis
  rather than centered-only. Consequently, default behavior is for titles to be "start" positioned instead of centered.

### Added

- wasm target
- Polar/Radar/Spider plots
- Annotations for XY Charts
- More helper functions for auto scaling axis ranges
- VerticalBarPlot overload for single series taking list of x-axis values and y-axis Floats.
- Vertical bar plot builder DSLs

### Fixed

- LinearAxisModel adding 2 extra minor ticks overlapping the first and last major ticks
- XYGraph width calculation to consume more of the available space

## [0.4.0]

### Changed

- Update Compose version to 1.5.1
- Update Kotlin version to 1.9.10
- Update Kotlin Coroutines to version 1.7.3
- Update Detekt to version 1.23.1
- Update Android Gradle plugin to 8.1.1
- Upgrade Gradle to 8.3

### Added

- forceCenteredPie parameter to PieChart that always places the pie in the center of the component, adjusting its size
  as needed to accommodate the labels.
- Support for ios
- Step chart
- Support for rotating x-axis and y-axis labels on XYCharts
- panZoomEnabled XYChart option that can be used to disable pan/zoom functionality
- Animation for line charts
- Area option for line charts
- Stacked area chart

### Fixed

- Vertical bar chart crash when only 1 data element in the series
- generateHueColorPalette generating 1 extra color and causing exception when number of colors is 0
- XYChart crashes if graph size is computed to be < 0
- BulletGraph reanimating unnecessarily

## [0.3.0]

### Added

- Pie slice gap setting

### Changed

- Update Compose version to 1.3.1
- Update Kotlin version to 1.8.10
- Update Android plugin version to 7.4.2
- Update Android compileSdk and targetSdk versions to 33
- Migrate to Material 3
- CategoryAxisModel.computeOffset will throw an IllegalArgumentException instead of returning NaN if an invalid
  category value is provided.

### Fixed

- Avoid compose Exception and subsequent application crash when using a border on a pie slice
- Crash when pie chart values are all zeros
- Crash when ColumnLegend was not provided symbol, label, and value Composables for every entry.

## [0.2.1]

### Fixed

- Add anti-alias option to default pie slice, which when false avoids visual artifacts appearing between adjacent
  slices.

## [0.2.0]

### Added

- maxPieDiameter parameter to the PieChart Composable

### Changed

- HoverableElementArea sets its size based on contained content rather than filling its parent
- Improve Bullet axis auto range implementation
- PieChart to use a StraightLineConnector as the default labelConnector instead of none
- StraightLineConnector and BezierLabelConnector to use a default color of the Material theme's onBackground instead of
  black

### Fixed

- Ensure List<Float>.autoScaleRange() works if receiver's min & max values are equal or zero.
- LinearAxisModel tick value calculation due to precision causing tick values to be duplicated and skipped

## [0.1.2]

- Fix BulletGraph auto axis range calculation so min and max of range cannot be equal
- Fix rendering of feature bar when axis range does not start at 0

## [0.1.1]

- Fixed bug in BulletGraph where a height of 0 would cause an exception due to trying to set a negative height
  constraint
- Fix pie chart size calculation that caused clipping of pie or labels in some circumstances
- Fix pie slice not showing when slice is a full circle
- Fix pie chart infinite measuring loop

## [0.1.0] - 2022-08-23

### Added

- pie chart
- line chart
- vertical bar chart
- bullet graph
