import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import com.uwyn.rife2.gradle.TemplateType.*

plugins {
    application
    id("com.uwyn.rife2") version "1.2.0"
    `maven-publish`
    id("org.graalvm.buildtools.native") version "0.9.28"
}

version = 1.0
group = "com.example"

rife2 {
    version.set("1.9.0")
    uberMainClass.set("hello.AppSiteUber")
    useAgent.set(true)
    precompiledTemplateTypes.add(HTML)
}

base {
    archivesName.set("hello")
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
    maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots") } // only needed for SNAPSHOT
}

dependencies {
    testImplementation("org.jsoup:jsoup:1.18.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
}

application {
    mainClass.set("hello.AppSite")
}

tasks {
    test {
        useJUnitPlatform()
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        }
    }
}

publishing {
    repositories {
        maven {
            name = "Build"
            url = uri(rootProject.layout.buildDirectory.dir("repo"))
        }
    }
    publications {
        create<MavenPublication>("maven") {
            artifactId = rootProject.name
            from(components["java"])
        }
    }
}

graalvmNative.binaries.all {
    buildArgs.add("--enable-preview") // support for Jetty virtual threads with JDK 19
    imageName.set("hello-$version")
}
