/*
 * Copyright 2003-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.uwyn.rife2.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.component.AdhocComponentWithVariants;
import org.gradle.api.component.ConfigurationVariantDetails;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.plugins.BasePluginExtension;
import org.gradle.api.plugins.JavaApplication;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.testing.Test;
import org.gradle.process.CommandLineArgumentProvider;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Rife2Plugin implements Plugin<Project> {
    public static final List<String> DEFAULT_TEMPLATES_DIRS = List.of("src/main/resources/templates", "src/main/templates");
    public static final String DEFAULT_GENERATED_RIFE2_CLASSES_DIR = "generated/classes/rife2";
    public static final String RIFE2_GROUP = "rife2";
    public static final String WEBAPP_SRCDIR = "src/main/webapp";
    public static final String PRECOMPILE_TEMPLATES_TASK_NAME = "precompileTemplates";

    @Override
    public void apply(Project project) {
        var plugins = project.getPlugins();
        plugins.apply("java");
        var javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
        var rife2Extension = createRife2Extension(project);
        var configurations = project.getConfigurations();
        var dependencyHandler = project.getDependencies();
        var tasks = project.getTasks();

        var rife2Configuration = createRife2Configuration(configurations, dependencyHandler, rife2Extension);
        var rife2CompilerClasspath = createRife2CompilerClasspathConfiguration(configurations, rife2Configuration);
        var rife2AgentClasspath = createRife2AgentConfiguration(configurations, dependencyHandler, rife2Extension);
        configurations.getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME).extendsFrom(rife2Configuration);

        var precompileTemplates = registerPrecompileTemplateTask(project, rife2CompilerClasspath, rife2Extension);
        createRife2DevelopmentOnlyConfiguration(project, configurations, dependencyHandler);
        exposePrecompiledTemplatesToTestTask(project, configurations, dependencyHandler, precompileTemplates);
        configureAgent(project, plugins, rife2Extension, rife2AgentClasspath);
        TaskProvider<Jar> uberJarTask = registerUberJarTask(project, plugins, javaPluginExtension, rife2Extension, tasks, precompileTemplates);
        bundlePrecompiledTemplatesIntoJarFile(tasks, precompileTemplates);

        configureMavenPublishing(project, plugins, configurations, uberJarTask);
    }

    @SuppressWarnings("unchecked")
    private static void configureMavenPublishing(Project project,
                                                 PluginContainer plugins,
                                                 ConfigurationContainer configurations,
                                                 TaskProvider<Jar> uberJarTask) {
        plugins.withId("maven-publish", unused -> {
            var rife2UberJarElements = configurations.create("rife2UberJarElements", conf -> {
                conf.setDescription("Exposes the uber jar archive of the RIFE2 web application.");
                conf.setCanBeResolved(false);
                conf.setCanBeConsumed(true);
                conf.getOutgoing().artifact(uberJarTask, artifact -> artifact.setClassifier("uber"));

                var runtimeAttributes = configurations.getByName(JavaPlugin.RUNTIME_ELEMENTS_CONFIGURATION_NAME).getAttributes();
                conf.attributes(attrs -> {
                    for (Attribute<?> attribute : runtimeAttributes.keySet()) {
                        Object value = runtimeAttributes.getAttribute(attribute);
                        //noinspection unchecked
                        if (Bundling.class.equals(attribute.getType())) {
                            attrs.attribute(Bundling.BUNDLING_ATTRIBUTE, project.getObjects().named(Bundling.class, Bundling.SHADOWED));
                        } else {
                            attrs.attribute((Attribute<Object>) attribute, value);
                        }
                    }
                });
            });

            var component = (AdhocComponentWithVariants) project.getComponents().getByName("java");
            component.addVariantsFromConfiguration(rife2UberJarElements, ConfigurationVariantDetails::mapToOptional);
        });
    }

    private static void exposePrecompiledTemplatesToTestTask(Project project,
                                                             ConfigurationContainer configurations,
                                                             DependencyHandler dependencyHandler,
                                                             TaskProvider<PrecompileTemplates> precompileTemplatesTask) {
        configurations.getByName(JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME)
            .getDependencies()
            .add(dependencyHandler.create(project.files(precompileTemplatesTask)));
    }

    private static void bundlePrecompiledTemplatesIntoJarFile(TaskContainer tasks,
                                                              TaskProvider<PrecompileTemplates> precompileTemplatesTask) {
        tasks.named("jar", Jar.class, jar -> {
            jar.from(precompileTemplatesTask);
            // This isn't great because it needs to be hardcoded, in order to avoid the templates
            // declared in `src/main/resources/templates` to be included in the jar file.
            // which means that if for whatever reason the user also uses the same directory for
            // something else, it will be excluded from the jar file.
            jar.exclude("templates");
        });
    }

    private void createRife2DevelopmentOnlyConfiguration(Project project,
                                                         ConfigurationContainer configurations,
                                                         DependencyHandler dependencies) {
        var rife2DevelopmentOnly = configurations.create("rife2DevelopmentOnly", conf -> {
            conf.setDescription("Dependencies which should only be visible when running the application in development mode (and not in tests).");
            conf.setCanBeConsumed(false);
            conf.setCanBeResolved(false);
        });
        DEFAULT_TEMPLATES_DIRS.stream().forEachOrdered(dir -> rife2DevelopmentOnly.getDependencies().add(dependencies.create(project.files(dir))));
        configurations.getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME).extendsFrom(rife2DevelopmentOnly);
    }

    private static TaskProvider<Jar> registerUberJarTask(Project project,
                                                         PluginContainer plugins,
                                                         JavaPluginExtension javaPluginExtension,
                                                         Rife2Extension rife2Extension,
                                                         TaskContainer tasks,
                                                         TaskProvider<PrecompileTemplates> precompileTemplatesTask) {
        return tasks.register("uberJar", Jar.class, jar -> {
            jar.setGroup(RIFE2_GROUP);
            jar.setDescription("Assembles the web application and all dependencies into a single jar archive.");
            var base = project.getExtensions().getByType(BasePluginExtension.class);
            jar.getArchiveBaseName().convention(project.provider(() -> base.getArchivesName().get() + "-uber"));
            jar.setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE);
            jar.from(javaPluginExtension.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getOutput());
            jar.from(precompileTemplatesTask);
            jar.into("webapp", spec -> spec.from(WEBAPP_SRCDIR));
            var runtimeClasspath = project.getConfigurations().getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME);
            jar.from(runtimeClasspath.getElements().map(e -> e.stream()
                .filter(f -> f.getAsFile().getName().toLowerCase(Locale.ENGLISH).endsWith(".jar"))
                .map(project::zipTree)
                .toList()));
            // This isn't great because it needs to be hardcoded, in order to avoid the templates
            // declared in `src/main/resources/templates` to be included in the jar file.
            // which means that if for whatever reason the user also uses the same directory for
            // something else, it will be excluded from the jar file.
            jar.exclude("templates");
            plugins.withId("application", unused -> jar.manifest(manifest ->
                manifest.getAttributes().put("Main-Class", rife2Extension.getUberMainClass().get()))
            );
        });
    }

    private static void configureAgent(Project project,
                                       PluginContainer plugins,
                                       Rife2Extension rife2Extension,
                                       Configuration rife2AgentClasspath) {
        CommandLineArgumentProvider agentProvider = () -> {
            if (Boolean.TRUE.equals(rife2Extension.getUseAgent().get())) {
                return Collections.singleton("-javaagent:" + rife2AgentClasspath.getAsPath());
            }
            return Collections.emptyList();
        };
        project.getTasks().named("test", Test.class, test -> test.getJvmArgumentProviders().add(agentProvider));
        plugins.withId("application", unused -> project.getTasks().named("run", JavaExec.class, run -> run.getArgumentProviders().add(agentProvider)));
    }

    private static Rife2Extension createRife2Extension(Project project) {
        var rife2 = project.getExtensions().create("rife2", Rife2Extension.class);
        rife2.getUseAgent().convention(false);
        rife2.getUberMainClass().convention(project.getExtensions().getByType(JavaApplication.class).getMainClass()
            .map(mainClass -> mainClass + "Uber"));
        return rife2;
    }

    private static Configuration createRife2CompilerClasspathConfiguration(ConfigurationContainer configurations,
                                                                           Configuration rife2Configuration) {
        return configurations.create("rife2CompilerClasspath", conf -> {
            conf.setDescription("The RIFE2 compiler classpath");
            conf.setCanBeConsumed(false);
            conf.setCanBeResolved(true);
            conf.extendsFrom(rife2Configuration);
        });
    }

    private static Configuration createRife2AgentConfiguration(ConfigurationContainer configurations,
                                                               DependencyHandler dependencyHandler,
                                                               Rife2Extension rife2Extension) {
        return configurations.create("rife2Agent", conf -> {
            conf.setDescription("The RIFE2 agent classpath");
            conf.setCanBeConsumed(false);
            conf.setCanBeResolved(true);
            conf.setTransitive(false);
            conf.getDependencies().addLater(rife2Extension.getVersion()
                .map(version -> dependencyHandler.create("com.uwyn.rife2:rife2:" + version + ":agent")));
        });
    }

    private static Configuration createRife2Configuration(ConfigurationContainer configurations,
                                                          DependencyHandler dependencyHandler,
                                                          Rife2Extension rife2Extension) {
        var config = configurations.create("rife2", conf -> {
            conf.setDescription("The RIFE2 framework dependencies");
            conf.setCanBeConsumed(false);
            conf.setCanBeResolved(false);
        });
        config.getDependencies().addLater(rife2Extension.getVersion()
            .map(version -> dependencyHandler.create("com.uwyn.rife2:rife2:" + version)));
        return config;
    }

    private static TaskProvider<PrecompileTemplates> registerPrecompileTemplateTask(Project project,
                                                                                    Configuration rife2CompilerClasspath,
                                                                                    Rife2Extension rife2Extension) {
        return project.getTasks().register(PRECOMPILE_TEMPLATES_TASK_NAME, PrecompileTemplates.class, task -> {
            task.setGroup(RIFE2_GROUP);
            task.setDescription("Pre-compiles the templates.");
            task.getVerbose().convention(true);
            task.getClasspath().from(rife2CompilerClasspath);
            task.getTypes().convention(rife2Extension.getPrecompiledTemplateTypes());
            DEFAULT_TEMPLATES_DIRS.stream().forEachOrdered(dir -> task.getTemplatesDirectories().from(project.getLayout().getProjectDirectory().dir(dir)));
            task.getOutputDirectory().set(project.getLayout().getBuildDirectory().dir(DEFAULT_GENERATED_RIFE2_CLASSES_DIR));
        });
    }
}
