import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import com.uwyn.rife2.gradle.TemplateType.*

plugins {
    application
    id("com.uwyn.rife2") version "1.3.0"
    `maven-publish`
    id("org.graalvm.buildtools.native") version "1.1.12"
}

version = 1.0
group = "com.example"

rife2 {
    version.set("1.10.1")
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
    maven { url = uri("https://central.sonatype.com/repository/maven-snapshots") } // only needed for SNAPSHOT
}

dependencies {
    testImplementation("org.jsoup:jsoup:1.23.2")
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
    imageName.set("hello-$version")
}
