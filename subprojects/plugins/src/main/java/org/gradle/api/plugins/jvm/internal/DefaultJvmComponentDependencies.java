/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.plugins.jvm.internal;

import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer;
import org.gradle.api.artifacts.dsl.DependencyAdder;
import org.gradle.api.plugins.jvm.JvmComponentDependencies;
import org.gradle.api.component.VariantMatchingFailureInterpreter;
import org.gradle.internal.component.DefaultVariantMatchingFailureInterpreter;

import javax.inject.Inject;

public abstract class DefaultJvmComponentDependencies implements JvmComponentDependencies {
    private final DependencyAdder implementation;
    private final DependencyAdder compileOnly;
    private final DependencyAdder runtimeOnly;
    private final DependencyAdder annotationProcessor;
    private final ExtensiblePolymorphicDomainObjectContainer<VariantMatchingFailureInterpreter> variantMatchingFailureInterpreters;

    @Inject
    public DefaultJvmComponentDependencies(DependencyAdder implementation, DependencyAdder compileOnly, DependencyAdder runtimeOnly, DependencyAdder annotationProcessor) {
        this.implementation = implementation;
        this.compileOnly = compileOnly;
        this.runtimeOnly = runtimeOnly;
        this.annotationProcessor = annotationProcessor;

        this.variantMatchingFailureInterpreters = getObjectFactory().polymorphicDomainObjectContainer(VariantMatchingFailureInterpreter.class);
        variantMatchingFailureInterpreters.registerBinding(VariantMatchingFailureInterpreter.class, DefaultVariantMatchingFailureInterpreter.class);
    }

    @Override
    public DependencyAdder getImplementation() {
        return this.implementation;
    }

    @Override
    public DependencyAdder getCompileOnly() {
        return this.compileOnly;
    }

    @Override
    public DependencyAdder getRuntimeOnly() {
        return this.runtimeOnly;
    }

    @Override
    public DependencyAdder getAnnotationProcessor() {
        return this.annotationProcessor;
    }

    @Override
    public ExtensiblePolymorphicDomainObjectContainer<VariantMatchingFailureInterpreter> getMatchingFailureInterpreters() {
        return variantMatchingFailureInterpreters;
    }
}
