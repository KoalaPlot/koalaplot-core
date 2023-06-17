buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-base:_") // needed for dokka custom format config
    }
}

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.dokka")
    id("io.gitlab.arturbosch.detekt")
    id("convention.publication")
}

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:_")
}

group = "io.github.koalaplot"
version = "0.4.0-dev1"

kotlin {
    explicitApi()
    jvm()
    js(IR) {
        browser()
    }
    android {
        publishLibraryVariants("release")
    }
    ios()
    iosX64()
    iosArm64()
    // iosSimulatorArm64()

    sourceSets {
        named("commonMain") {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.animation)
                implementation(KotlinX.coroutines.core)
            }
        }

        named("commonTest") {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        named("jvmMain") {
            dependencies {
                implementation(compose.material3)
            }
        }

        named("jvmTest") {
            dependencies {
                implementation(kotlin("test"))
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.uiTestJUnit4)
                implementation(compose.desktop.currentOs)
            }
        }

        named("iosMain") {
            dependencies {
                implementation(compose.material3)
            }
        }
    }
}

android {
    namespace = "io.github.koalaplot"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
        targetSdk = 33
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

/**
 * Custom format adds a custom logo
 */
tasks.register<org.jetbrains.dokka.gradle.DokkaTask>("dokkaCustomFormat") {
    moduleName.set("Koala Plot Core")
    outputDirectory.set(buildDir.resolve("docs/api/${project.version}"))

    pluginConfiguration<org.jetbrains.dokka.base.DokkaBase, org.jetbrains.dokka.base.DokkaBaseConfiguration> {
        customStyleSheets = listOf(file("src/docs/dokka/logo-styles.css"))
        customAssets = listOf(
            file("src/docs/assets/images/logo-icon.svg")
        )
    }
}

tasks["build"].dependsOn.add("dokkaCustomFormat")

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=kotlin.RequiresOptIn"
        )
        jvmTarget = "11"
    }

    detekt {
        source = files("src")
        parallel = true
        config = files("$rootDir/detekt.yml")
        buildUponDefaultConfig = true
    }
}

afterEvaluate {
    // https://discuss.kotlinlang.org/t/disabling-androidandroidtestrelease-source-set-in-gradle-kotlin-dsl-script/21448
    project.extensions.findByType<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension>()?.let { ext ->
        ext.sourceSets.removeAll { sourceSet ->
            setOf(
                // "androidAndroidTestRelease",
                "androidTestFixtures",
                "androidTestFixturesDebug",
                "androidTestFixturesRelease",
            ).contains(sourceSet.name)
        }
    }
}
