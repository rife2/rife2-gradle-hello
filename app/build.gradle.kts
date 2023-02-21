import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    application
    id("com.uwyn.rife2")
}

base {
    archivesName.set("hello")
    version = 1.0
}

application {
    mainClass.set("hello.App")
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
    maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots") } // only needed for SNAPSHOT
}

rife2 {
    version.set("1.3.0")
    useAgent.set(true)
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
