# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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

### Removed

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
