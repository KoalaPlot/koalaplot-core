# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.1]

- Fixed bug in BulletGraph where a height of 0 would cause an exception due to trying to set a negative height
  constraint
- Fix pie chart size calculation that caused clipping of pie or labels in some circumstances
- Fix pie slice not showing when slice is a full circle

## [0.1.0] - 2022-08-23

### Added

- pie chart
- line chart
- vertical bar chart
- bullet graph
