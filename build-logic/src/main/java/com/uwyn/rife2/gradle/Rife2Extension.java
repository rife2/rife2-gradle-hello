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

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

/**
 * The Gradle RIFE2 extension
 */
public abstract class Rife2Extension {
    /**
     * The RIFE2 version that should be used by the project.
     *
     * @return the RIFE2 version as a string
     */
    public abstract Property<String> getVersion();

    /**
     * Indicates whether the project should be launched with the RIFE2 agent or not.
     *
     * @return {@code true} when the project should be launched with the RIFE2 agent;
     * {@code false} otherwise
     */
    public abstract Property<Boolean> getUseAgent();

    /**
     * Specifies the main Java class to use when building the uber jar.
     *
     * @return the fully qualified name of the main class to use when launching the uber jar.
     */
    public abstract Property<String> getUberMainClass();

    /**
     * Specifies the template types that should be precompiled.
     * By default, none are precompiled.
     *
     * @return a list of template types to precompile
     */
    public abstract ListProperty<TemplateType> getPrecompiledTemplateTypes();

    /**
     * Specifies the directories where the template files can be found.
     * By default, this includes {@code "src/main/resources/templates"}.
     *
     * @return the collection of directories to look for template files
     */
    public abstract ConfigurableFileCollection getTemplateDirectories();
}
