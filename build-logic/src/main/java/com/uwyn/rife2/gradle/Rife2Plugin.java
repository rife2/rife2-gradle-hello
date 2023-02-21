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
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.plugins.BasePluginExtension;
import org.gradle.api.plugins.JavaApplication;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.testing.Test;
import org.gradle.process.CommandLineArgumentProvider;

import java.util.Collections;
import java.util.stream.Collectors;

public class Rife2Plugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        var plugins = project.getPlugins();
        plugins.apply("java");
        var javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
        var rife2Extension = createRife2Extension(project, javaPluginExtension);
        var configurations = project.getConfigurations();
        var dependencyHandler = project.getDependencies();
        var rife2Configuration = createRife2Configuration(configurations, dependencyHandler, rife2Extension);
        var rife2CompilerClasspath = createRife2CompilerClasspathConfiguration(configurations, rife2Configuration);
        var rife2AgentClasspath = createRife2AgentConfiguration(configurations, dependencyHandler, rife2Extension);
        configurations.getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME).extendsFrom(rife2Configuration);
        var precompileTemplates = registerPrecompileTemplateTask(project, rife2CompilerClasspath);
        addTemplatesToMainOutput(precompileTemplates, javaPluginExtension);
        configureAgent(project, plugins, rife2Extension, rife2AgentClasspath);
        project.getTasks().register("uberJar", Jar.class, jar -> {
            var base = project.getExtensions().getByType(BasePluginExtension.class);
            jar.getArchiveBaseName().convention(project.provider(() -> base.getArchivesName().get() + "-uber"));
            jar.setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE);
            jar.into("webapp", spec -> spec.from("src/main/webapp"));
            var runtimeClasspath = project.getConfigurations().getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME);
            jar.from(javaPluginExtension.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getOutput());
            jar.from(runtimeClasspath.getElements().map(e -> e.stream().map(project::zipTree).collect(Collectors.toList())));
            plugins.withId("application", unused -> jar.manifest(manifest ->
                manifest.getAttributes().put("Main-Class", rife2Extension.getUberMainClass().get()))
            );
        });
    }

    private static void configureAgent(Project project, PluginContainer plugins, Rife2Extension rife2Extension, Configuration rife2AgentClasspath) {
        CommandLineArgumentProvider agentProvider = () -> {
            if (Boolean.TRUE.equals(rife2Extension.getUseAgent().get())) {
                return Collections.singleton("-javaagent:" + rife2AgentClasspath.getAsPath());
            }
            return Collections.emptyList();
        };
        project.getTasks().named("test", Test.class, test -> test.getJvmArgumentProviders().add(agentProvider));
        plugins.withId("application", unused -> project.getTasks().named("run", JavaExec.class, run -> run.getArgumentProviders().add(agentProvider)));
    }

    private static Rife2Extension createRife2Extension(Project project, JavaPluginExtension javaPluginExtension) {
        var rife2 = project.getExtensions().create("rife2", Rife2Extension.class);
        rife2.getUseAgent().convention(false);
        rife2.getUberMainClass().convention(project.getExtensions().getByType(JavaApplication.class).getMainClass()
            .map(mainClass -> mainClass + "Uber"));
        return rife2;
    }

    private static Configuration createRife2CompilerClasspathConfiguration(ConfigurationContainer configurations, Configuration rife2Configuration) {
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

    private static void addTemplatesToMainOutput(TaskProvider<PrecompileTemplates> precompileTemplates,
                                                 JavaPluginExtension javaPluginExtension) {
        javaPluginExtension.getSourceSets()
            .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
            .getOutput()
            .dir(precompileTemplates);
    }

    private static TaskProvider<PrecompileTemplates> registerPrecompileTemplateTask(Project project,
                                                                                    Configuration rife2CompilerClasspath) {
        return project.getTasks().register("precompileTemplates", PrecompileTemplates.class, task -> {
            task.getVerbose().convention(true);
            task.getClasspath().from(rife2CompilerClasspath);
            task.getType().convention("html");
            task.getTemplatesDirectory().set(project.getLayout().getProjectDirectory().dir("src/main/templates"));
            task.getOutputDirectory().set(project.getLayout().getBuildDirectory().dir("generated/classes/rife2"));
        });
    }
}
