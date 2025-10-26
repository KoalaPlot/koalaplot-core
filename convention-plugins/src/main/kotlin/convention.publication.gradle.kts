import java.util.Properties

plugins {
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin")
}

// Stub secrets to let the project sync and build without the publication values set up
ext["signing.password"] = null
ext["signing.key"] = null
ext["ossrhUsername"] = null
ext["ossrhPassword"] = null
ext["sonatypeStagingProfileId"] = null

// Grabbing secrets from local.properties file or from environment variables
val secretPropsFile = project.rootProject.file("local.properties")
if (secretPropsFile.exists()) {
    secretPropsFile.reader().use {
        Properties().apply {
            load(it)
        }
    }.onEach { (name, value) ->
        ext[name.toString()] = value
    }
} else {
    ext["signing.password"] = System.getenv("SIGNING_PASSWORD")
    ext["signing.key"] = System.getenv("SIGNING_KEY")
    ext["ossrhUsername"] = System.getenv("OSSRH_USERNAME")
    ext["ossrhPassword"] = System.getenv("OSSRH_PASSWORD")
    ext["sonatypeStagingProfileId"] = System.getenv("SONATYPE_STAGING_PROFILE_ID")
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

fun getExtraString(name: String) = ext[name]?.toString()

tasks.withType<AbstractPublishToMaven>().configureEach {
    // Fix errors related to declared dependencies between publishing tasks and signing tasks
    dependsOn.add("signAndroidReleasePublication")
    dependsOn.add("signJsPublication")
    dependsOn.add("signDesktopPublication")
    dependsOn.add("signKotlinMultiplatformPublication")
    dependsOn.add("signWasmJsPublication")
    dependsOn.add("signIosArm64Publication")
    dependsOn.add("signIosSimulatorArm64Publication")
    dependsOn.add("signIosX64Publication")
}

publishing {
    publications.withType<MavenPublication> {
        // Stub javadoc.jar artifact
        artifact(javadocJar.get())

        // Provide artifacts information required by Maven Central
        pom {
            name.set("koalaplot-core")
            description.set(
                "Koala Plot is a Compose Multiplatform based charting and " +
                    "plotting library written in Kotlin"
            )
            url.set("https://github.com/KoalaPlot/koalaplot-core")

            licenses {
                license {
                    name.set("MIT")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }
            developers {
                developer {
                    id.set("gsteckman")
                    name.set("Greg Steckman")
                    email.set("27028452+gsteckman@users.noreply.github.com")
                }
            }
            scm {
                connection.set("scm:git:github.com/KoalaPlot/koalaplot-core.git")
                developerConnection.set("scm:git:ssf://github.com/KoalaPlot/koalaplot-core.git")
                url.set("https://github.com/KoalaPlot/koalaplot-core")
            }
        }
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
            stagingProfileId.set(getExtraString("sonatypeStagingProfileId"))
            username.set(getExtraString("ossrhUsername"))
            password.set(getExtraString("ossrhPassword"))
        }
    }
}

signing {
    setRequired({ gradle.taskGraph.hasTask("publishToSonatype") })
    useInMemoryPgpKeys(getExtraString("signing.key"), getExtraString("signing.password"))
    sign(publishing.publications)
}
