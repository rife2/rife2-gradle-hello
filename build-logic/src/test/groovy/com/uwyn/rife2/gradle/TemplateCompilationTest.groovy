package com.uwyn.rife2.gradle

class TemplateCompilationTest extends AbstractFunctionalTest {
    def setup() {
        usesProject("minimal")
    }

    def "doesn't precompile templates when calling `run`"() {
        given:
        buildFile << """
            tasks.named("run") {
                doFirst {
                    throw new RuntimeException("force stop")
                }
            }
        """
        when:
        fails 'run'

        then: "precompile templates task must not be present in task graph"
        errorOutputContains("force stop")
        tasks {
           doesNotContain ":${Rife2Plugin.PRECOMPILE_TEMPLATES_TASK_NAME}"
        }
    }

    def "`run` task classpath includes template sources"() {
        given:
        buildFile << """
            tasks.register("dumpRunClasspath") {
                doLast {
                    tasks.named("run").get().classpath.files.each {
                        println "Classpath entry: \$it"
                    }
                }   
            }
        """

        when:
        run("dumpRunClasspath")

        then: "template sources must be present in the classpath"
        outputContains("Classpath entry: ${file("src/main/templates").absolutePath}")
    }

    def "compiles templates when running #task"() {
        when:
        run task

        then: "precompile templates task must be present in task graph"
        tasks {
            succeeded ":${Rife2Plugin.PRECOMPILE_TEMPLATES_TASK_NAME}"
        }

        where:
        task << ['jar', 'test', 'uberJar']
    }
}
