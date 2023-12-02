# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- Update to Compose version 1.5.10
- Update Kotlin version to 1.9.20
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
