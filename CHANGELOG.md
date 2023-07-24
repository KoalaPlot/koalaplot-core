# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- Update Compose version to 1.4.0
- Update Kotlin version to 1.8.20
- Update Kotlin Coroutines to version 1.7.1
- Update Detekt to version 1.23.0

### Added
- forceCenteredPie parameter to PieChart that always places the pie in the center of the component, adjusting its size
as needed to accommodate the labels.
- Support for ios (not working)
- Step chart
- Support for rotating x-axis and y-axis labels on XYCharts

### Fixed
- Vertical bar chart crash when only 1 data element in the series
- generateHueColorPalette generating 1 extra color and causing exception when number of colors is 0

## [0.3.0]

### Added
- Pie slice gap setting

### Changed
- Update Compose version to 1.3.1
- Update Kotlin version to 1.8.10
- Update Android plugin version to 7.4.2
- Update Android compileSdk and targetSdk versions to 33
- Migrate to Material 3

### Fixed
- Avoid compose Exception and subsequent application crash when using a border on a pie slice
- Crash when pie chart values are all zeros

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
