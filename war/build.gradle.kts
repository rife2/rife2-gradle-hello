plugins {
    war
}

base {
    archivesName.set("hello")
    version = 1.0
}

repositories {
    mavenCentral()
    maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots") } // only needed for SNAPSHOT
}

dependencies {
    implementation(project(":app"))
}

tasks.war {
    webAppDirectory.set(file("../app/src/main/webapp"))
    webXml = file("src/web.xml")
    rootSpec.exclude("**/jetty*.jar", "**/slf4j*.jar")
}