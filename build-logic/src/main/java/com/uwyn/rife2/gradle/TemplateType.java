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

import java.io.Serial;
import java.io.Serializable;

/**
 * Allows template types to be specified for pre-compilation.
 */
public class TemplateType implements Serializable {
    @Serial private static final long serialVersionUID = -2736320275307140837L;

    /**
     * The {@code html} template type.
     */
    public static TemplateType HTML = new TemplateType("html");
    /**
     * The {@code json} template type.
     */
    public static TemplateType JSON = new TemplateType("json");
    /**
     * The {@code svg} template type.
     */
    public static TemplateType SVG = new TemplateType("svg");
    /**
     * The {@code xml} template type.
     */
    public static TemplateType XML = new TemplateType("xml");
    /**
     * The {@code txt} template type.
     */
    public static TemplateType TXT = new TemplateType("txt");
    /**
     * The {@code sql} template type.
     */
    public static TemplateType SQL = new TemplateType("sql");

    private final String identifier_;

    /**
     * Creates a new template type instance.
     *
     * @param identifier the identifier of this template type
     */
    public TemplateType(String identifier) {
        identifier_ = identifier;
    }

    /**
     * Retrieves the identifier for this template type
     * @return the template type identifier as a string
     */
    public String identifier() {
        return identifier_;
    }
}