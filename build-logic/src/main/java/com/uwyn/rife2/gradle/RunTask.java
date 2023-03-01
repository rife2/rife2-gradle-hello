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
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.process.ExecOperations;

import javax.inject.Inject;
import java.util.List;

@CacheableTask
public abstract class RunTask extends DefaultTask {
    @Input
    public abstract Property<String> getAgentClassPath();

    @Classpath
    public abstract ConfigurableFileCollection getClasspath();

    @Inject
    protected abstract ExecOperations getExecOperations();

    @Input
    public abstract Property<String> getMainClass();

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getTemplatesDirectory();

    @TaskAction
    public void run() {
        getExecOperations().javaexec(run -> {
            run.setClasspath(getProject().getObjects().fileCollection().from(getTemplatesDirectory()).plus(getClasspath()));
            run.getMainClass().set(getMainClass().get());
            run.args(List.of("-javaagent:" + getAgentClassPath().get()));
        });
    }
}