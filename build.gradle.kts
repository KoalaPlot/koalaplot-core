import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

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
version = "0.5.1"

kotlin {
    explicitApi()

    androidTarget {
        publishLibraryVariants("release")
    }

    iosArm64()
    iosSimulatorArm64()
    iosX64()

    js(IR) {
        browser()
    }

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                api(compose.ui)
                api(compose.animation)
                implementation(KotlinX.coroutines.core)
            }
        }

        named("commonTest") {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        named("jvmMain") {
            dependsOn(commonMain)

            dependencies {

            }
        }

        named("jvmTest") {
            dependencies {
                implementation(kotlin("test"))
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.desktop.uiTestJUnit4)
                implementation(compose.desktop.currentOs)
            }
        }

        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        create("iosMain") {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)

            dependencies { }
        }
    }
}

android {
    namespace = "io.github.koalaplot"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
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
    outputDirectory.set(layout.buildDirectory.dir("docs/api/${project.version}").get())

    pluginConfiguration<org.jetbrains.dokka.base.DokkaBase, org.jetbrains.dokka.base.DokkaBaseConfiguration> {
        customStyleSheets = listOf(file("src/docs/dokka/logo-styles.css"))
        customAssets = listOf(file("src/docs/assets/images/logo-icon.svg"))
    }
}

tasks["build"].dependsOn.add("dokkaCustomFormat")

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + listOf("-opt-in=kotlin.RequiresOptIn")
        jvmTarget = "11"
    }

    detekt {
        source.setFrom("src")
        parallel = true
        config.setFrom("$rootDir/detekt.yml")
        buildUponDefaultConfig = true
    }
}

afterEvaluate { // https://discuss.kotlinlang.org/t/disabling-androidandroidtestrelease-source-set-in-gradle-kotlin-dsl-script/21448
    project.extensions.findByType<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension>()
        ?.let { ext ->
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
