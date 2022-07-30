![Kotlin Version](https://img.shields.io/badge/Kotlin-1.7.0-orange)
[![Dokka docs](https://img.shields.io/badge/docs-dokka-278ec7)](http://KoalaPlot.github.io/koalaplot-core)
[![License MIT](https://img.shields.io/badge/license-MIT-278ec7.svg)](https://github.com/KoalaPlot/koalaplot-core/tree/main/LICENSE.txt)

# Koala Plot

Koala Plot is a [Compose Multiplatform](https://www.jetbrains.com/lp/compose-mpp/) based charting
and plotting library allowing you to build great looking interactive charts for
[Android](https://developer.android.com/jetpack/compose), desktop, and web using a single API and
code base. Most elements are Composables, allowing for an infinite degree of customization. Web support is provided by
the Compose-web Canvas support currently in alpha.

This project is in a pre-release experimental/alpha state. We encourage you to give it a try,
make suggestions for improvement, and
even [contribute](https://github.com/KoalaPlot/koalaplot-core/tree/main/CONTRIBUTING.md)! It is expected that
the
API surface and functionality will change as we gain experience using the library in applications.

For a quick look at the possibilities when using Koala Plot, you can try out
the [web version of the samples](https://koalaplot.github.io/koalaplot-samples/index.html). Note that this uses the
alpha Compose web-canvas capability, so there may be bugs in the underlying Compose framework, and it is fixed at
a resolution of 1024 x 768.

# Current Features

Most elements of a plot are Composables and can therefore be easily customized. This
includes things like colors, fonts, borders, shapes, user interaction, etc.

* Pie and donut chart
    * Composable slices, with default implementation that can react to hovering
    * Optional Composable labels and Composable connectors with provided linear and bezier implementations
    * Composable content for donut chart centers
    * Customizable first-draw animation
* Line chart
    * Linear or Log y-axis
    * Numeric or category x-axis
    * Composable axis titles and value labels
    * Optional Composable symbols at each data point with default implementations
    * Zoom and pan one or both axes (Android only, for now)
* Vertical bar chart
    * Linear or Log y-axis
    * Numeric or category x-axis
    * Composable vertical bars with a provided default implementation
    * Support for clustered and stacked bars
    * Support for negative values and bars that don't start at 0
    * Customizable first-draw animation
    * Zoom and pan one or both axes (Android only, for now)
* Chart layout
    * Combines an optional Composable title, plot, and legend with any of 4 positions
* Legends
    * Single column
    * Flow-layout

# Getting Started

To build locally and integrate with your own code, follow these steps (when more mature, we'll publish to
the [Central Maven
repository](https://search.maven.org/)):

1. Clone the repository to your local machine

```shell
git clone https://github.com/koalaplot/koalaplot-core.git
```

2. Build and publish it to your local maven repository

```shell
./gradlew :library:publishToMavenLocal
```

3. Add the MavenLocal repository to your project and include Koala Plot core as a dependency in your project's
   build.gradle.kts

```kotlin
repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    ...
    mavenLocal()
}
```

```kotlin
dependencies {
    ...
    implementation("io.github.koalaplot:core:0.1.0-SNAPSHOT")
}
```

You can also see a complete example of a build.gradle.kts in
the [samples](https://koalaplot.github.io/koalaplot-core-samples).

# Documentation

[Latest build API documentation](https://koalaplot.github.io/koalaplot-core/api/0.1.0-SNAPSHOT).

Also see the [sample repository](https://github.com/koalaplot/koalaplot-core-samples) for code examples.

# Contributing

Contributions are welcome. Further details can be found in the
[Contributing Guidelines](https://github.com/KoalaPlot/core/tree/main/CONTRIBUTING.md)


