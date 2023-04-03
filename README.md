[![Maven Central](https://img.shields.io/maven-central/v/io.github.koalaplot/koalaplot-core?color=278ec7)](https://repo1.maven.org/maven2/io/github/koalaplot/koalaplot-core/)
[![Kotlin](https://img.shields.io/badge/kotlin-1.8.10-278ec7.svg?logo=kotlin)](http://kotlinlang.org)
[![Dokka docs](https://img.shields.io/badge/docs-dokka-278ec7)](https://koalaplot.github.io/koalaplot-core/api/0.1.0/)
[![License MIT](https://img.shields.io/badge/license-MIT-278ec7.svg)](https://github.com/KoalaPlot/koalaplot-core/tree/main/LICENSE.txt)

# Koala Plot

Koala Plot is a [Compose Multiplatform](https://www.jetbrains.com/lp/compose-mpp/) based charting and plotting library
allowing you to build great looking interactive charts for
[Android](https://developer.android.com/jetpack/compose), desktop, and web using a single API and code base.

Try out the [web version of the samples](https://koalaplot.github.io/koalaplot-samples/index.html) for a quick look at
the possibilities when using Koala Plot. Note that this uses the alpha Compose web-canvas capability, so there may be
bugs in the underlying Compose framework, and it is fixed at a resolution of 1024 x 768.

This project is in a pre-release experimental/alpha state. We encourage you to give it a try, make suggestions for
improvement, and even [contribute](https://github.com/KoalaPlot/koalaplot-core/blob/main/CONTIBUTING.md)! It is expected
that the API surface and functionality will change as we gain experience using the library in applications.

# Current Features

Most elements of a plot are Composables and can therefore be easily customized. This includes things like colors, fonts,
borders, shapes, user interaction, etc. Web support is provided by the Compose-web Canvas support currently in alpha.

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
* Bullet Graph
    * Individual or multiple vertically aligned bullet graphs
* Chart layout
    * Combines an optional Composable title, plot, and legend with any of 4 positions
* Legends
    * Single column
    * Flow-layout

# Getting Started

1. Add the mavenCentral and compose repositories to your project's build.gradle.kts

```kotlin
repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}
```

2. Include Koala Plot core as a dependency in your project's build.gradle.kts

```kotlin
dependencies {
    implementation("io.github.koalaplot:koalaplot-core:0.1.0")
}
```

You can also see a complete example of a build.gradle.kts in
the [samples](https://koalaplot.github.io/koalaplot-samples).

3. Start coding

```kotlin
    BulletGraph {
        label {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = KoalaPlotTheme.sizes.gap)
            ) {
                Text(
                    "Revenue 2005 YTD",
                    textAlign = TextAlign.End
                )
                Text(
                    "(US $ in thousands)",
                    textAlign = TextAlign.End
                )
            }
        }
        axis { labels { "${it.toInt()}" } }
        comparativeMeasure(260f)
        featuredMeasureBar(275f)
        ranges(0f, 200f, 250f, 300f)
    }
```

# Documentation

- [Latest build](https://koalaplot.github.io/koalaplot-core/api/0.3.0)
- [Release 0.3.0](https://koalaplot.github.io/koalaplot-core/api/0.3.0)

Also see the [sample repository](https://github.com/KoalaPlot/koalaplot-samples) for code examples.

# Contributing

Contributions are welcome. Further details can be found in the
[Contributing Guidelines](https://github.com/KoalaPlot/koalaplot-core/blob/main/CONTRIBUTING.md)
