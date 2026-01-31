import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.dokka)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint.gradle)
    id("convention.publication")
}

repositories {
    google()
    mavenCentral()
}

group = "io.github.koalaplot"
version = "0.11.0"

kotlin {
    explicitApi()

    androidLibrary {
        namespace = "io.github.koalaplot"
        compileSdk = 36
        minSdk = 24

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

    jvm("desktop") {
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
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            api(libs.compose.ui)
            api(libs.compose.animation)
            implementation(libs.kotlinx.coroutines)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        named("desktopMain") {
            dependsOn(commonMain.get())
            dependencies {}
        }

        named("desktopTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.compose.ui.test.junit4)
                implementation(compose.desktop.currentOs)
            }
        }

        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        iosMain {
            dependsOn(commonMain.get())
            dependencies { }
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
    }
}

dokka {
    moduleName.set("Koala Plot Core")

    dokkaPublications.html {
        outputDirectory.set(layout.buildDirectory.dir("docs/api/${project.version}"))
    }

    pluginsConfiguration.html {
        customStyleSheets.from("src/docs/dokka/logo-styles.css")
        customAssets.from("src/docs/assets/images/logo-icon.svg")
    }
}

detekt {
    source.setFrom("src/androidMain", "src/commonMain", "src/desktopMain", "src/iosMain", "src/jsMain", "src/wasmJsMain")
    parallel = true
    config.setFrom("$rootDir/detekt.yml")
    buildUponDefaultConfig = true
}

ktlint {
    version.set("1.8.0")
}

dependencies {
    ktlintRuleset(libs.ktlint.compose)
}
