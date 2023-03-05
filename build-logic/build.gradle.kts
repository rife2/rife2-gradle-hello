plugins {
    `java-gradle-plugin`
    groovy
}

repositories {
    mavenCentral()
}

dependencies {
    gradleApi()
    testImplementation(libs.spock.core)
    testImplementation(gradleTestKit())
}

gradlePlugin {
    plugins {
        create("rife2") {
            id = "com.uwyn.rife2"
            implementationClass = "com.uwyn.rife2.gradle.Rife2Plugin"
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        events = setOf(org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED, org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED, org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED)
    }
}
