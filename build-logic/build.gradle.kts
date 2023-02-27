plugins {
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
}

dependencies {
    gradleApi()
}

tasks {
    withType<JavaCompile> {
        options.isDeprecation = true
        options.compilerArgs.add("-Xlint:unchecked")
    }
}

gradlePlugin {
    plugins {
        create("rife2") {
            id = "com.uwyn.rife2"
            implementationClass = "com.uwyn.rife2.gradle.Rife2Plugin"
        }
    }
}
