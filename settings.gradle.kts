pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

plugins {
    // See https://jmfayard.github.io/refreshVersions
    id("de.fayard.refreshVersions") version "0.40.2"
////                            # available:"0.50.0"
////                            # available:"0.50.1"
////                            # available:"0.50.2"
////                            # available:"0.51.0"
}

rootProject.name = "koalaplot-core"

includeBuild("convention-plugins")
