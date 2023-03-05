plugins {
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
}

dependencies {
    gradleApi()
}

gradlePlugin {
    plugins {
        create("rife2") {
            id = "com.uwyn.rife2"
            implementationClass = "com.uwyn.rife2.gradle.Rife2Plugin"
        }
    }
}
