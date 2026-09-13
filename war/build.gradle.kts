plugins {
    war
}

version = 1.0

base {
    archivesName.set("hello")
}

repositories {
    mavenCentral()
    maven { url = uri("https://central.sonatype.com/repository/maven-snapshots") } // only needed for SNAPSHOT
}

dependencies {
    implementation(project(":app"))
}

tasks.war {
    webAppDirectory.set(file("../app/src/main/webapp"))
    webXml = file("src/web.xml")
}