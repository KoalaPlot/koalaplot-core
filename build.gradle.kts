import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

buildscript {
    dependencies {
        classpath(libs.dokka.base) // needed for dokka custom format config
    }
}

plugins {
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.dokka)
    alias(libs.plugins.detekt)
    id("convention.publication")
}

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    detektPlugins(libs.detekt.formatting)
}

group = "io.github.koalaplot"
version = "0.7.0-dev1"

kotlin {
    explicitApi()

    androidTarget {
        publishLibraryVariants("release")
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    iosArm64()
    iosSimulatorArm64()
    iosX64()

    js(IR) {
        browser()
    }

    jvm {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            api(compose.ui)
            api(compose.animation)
            implementation(libs.kotlinx.coroutines)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        jvmMain.dependencies {}

        jvmTest.dependencies {
            implementation(kotlin("test"))
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.desktop.uiTestJUnit4)
            implementation(compose.desktop.currentOs)
        }

        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        create("iosMain") {
            dependencies { }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
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
    detekt {
        source.setFrom("src")
        parallel = true
        config.setFrom("$rootDir/detekt.yml")
        buildUponDefaultConfig = true
    }
}
