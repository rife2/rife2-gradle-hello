import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import com.uwyn.rife2.gradle.TemplateType.*

plugins {
    application
    id("com.uwyn.rife2")
    `maven-publish`
    id("org.graalvm.buildtools.native") version "0.9.20"
}

base {
    archivesName.set("hello")
    version = 1.0
    group = "com.example"
}

application {
    mainClass.set("hello.App")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
    maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots") } // only needed for SNAPSHOT
}

rife2 {
    version.set("1.4.0")
    useAgent.set(true)
    precompiledTemplateTypes.add(HTML)
}

dependencies {
    runtimeOnly("org.eclipse.jetty:jetty-server:11.0.13")
    runtimeOnly("org.eclipse.jetty:jetty-servlet:11.0.13")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.5")

    testImplementation("org.jsoup:jsoup:1.15.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.1")
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
            from(components["java"])
        }
    }
}

graalvmNative.binaries.all {
    imageName.set("hello-$version")
}
