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
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecOperations;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@CacheableTask
public abstract class PrecompileTemplates extends DefaultTask {

    @Classpath
    public abstract ConfigurableFileCollection getClasspath();

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getTemplatesDirectory();

    @Input
    public abstract ListProperty<TemplateType> getTypes();

    @Input
    @Optional
    public abstract Property<String> getEncoding();

    @Input
    @Optional
    public abstract Property<Boolean> getVerbose();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @Inject
    protected abstract ExecOperations getExecOperations();

    @TaskAction
    public void precompileTemplates() {
        for (var type : getTypes().get()) {
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
                args.add(getTemplatesDirectory().get().getAsFile().getPath());
                javaexec.args(args);
            });
        }
    }
}
