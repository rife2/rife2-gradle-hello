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

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecOperations;

import javax.inject.Inject;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Gradle task to pre-compile RIFE2 templates
 */
@CacheableTask
public abstract class PrecompileTemplates extends DefaultTask {
    /**
     * The directories where template files can be found.
     *
     * @return the directories with template files
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getTemplatesDirectories();

    /**
     * The template types to pre-compile.
     *
     * @return a list of template types
     */
    @Input
    public abstract ListProperty<TemplateType> getTypes();

    /**
     * The encoding to use when reading the template files.
     * Defaults to {@code UTF-8}.
     *
     * @return the encoding of the template files
     */
    @Input
    @Optional
    public abstract Property<String> getEncoding();

    /**
     * Indicates whether the pre-compilation should be verbose or not.
     *
     * @return {@code true} when the pre-compilation should be verbose; or
     * {@code false} otherwise
     */
    @Input
    @Optional
    public abstract Property<Boolean> getVerbose();

    /**
     * Provides the directory into which pre-compiled template class files should be stored.
     *
     * @return the output directory for the template pre-compilation
     */
    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @Classpath
    public abstract ConfigurableFileCollection getClasspath();

    @Inject
    protected abstract ExecOperations getExecOperations();

    /**
     * Perform the template pre-compilation
     */
    @TaskAction
    public void precompileTemplates() {
        for (var type : getTypes().get()) {
            getTemplatesDirectories().getFiles().forEach(dir -> {
                if (Files.exists(dir.toPath())) {
                    getExecOperations().javaexec(javaexec -> {
                        javaexec.setClasspath(getClasspath());
                        javaexec.getMainClass().set("rife.template.TemplateDeployer");
                        List<String> args = new ArrayList<>();
                        if (getVerbose().isPresent() && Boolean.TRUE.equals(getVerbose().get())) {
                            args.add("-verbose");
                        }
                        args.add("-t");
                        args.add(type.identifier());
                        args.add("-d");
                        args.add(getOutputDirectory().get().getAsFile().getPath());
                        args.add("-encoding");
                        args.add(getEncoding().orElse("UTF-8").get());
                        args.add(dir.getPath());
                        javaexec.args(args);
                    });
                }
            });

        }
    }
}
