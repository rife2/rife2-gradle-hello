package com.uwyn.rife2.gradle

import java.nio.file.FileSystems
import java.nio.file.Files

class PackagingTest extends AbstractFunctionalTest {
    def setup() {
        usesProject("minimal")
    }

    def "#archive contains compiled resources"() {
        def jarFile = file(archive).toPath()
        when:
        run task

        then: "compiles templates are found in the archive"
        tasks {
            succeeded ":${Rife2Plugin.PRECOMPILE_TEMPLATES_TASK_NAME}"
        }
        Files.exists(jarFile)
        try (def fs = FileSystems.newFileSystem(jarFile, [:])) {
            fs.getRootDirectories().each {
                Files.walk(it).forEach { path ->
                    println path
                }
            }
            assert Files.exists(fs.getPath("/rife/template/html/hello.class"))
            assert Files.exists(fs.getPath("/rife/template/html/world.class"))
            assert !Files.exists(fs.getPath("/templates/hello.html"))
            assert !Files.exists(fs.getPath("/templates/world.html"))
        }

        where:
        task      | archive
        'jar'     | 'build/libs/hello-1.0.jar'
        'uberJar' | 'build/libs/hello-uber-1.0.jar'
    }
}
