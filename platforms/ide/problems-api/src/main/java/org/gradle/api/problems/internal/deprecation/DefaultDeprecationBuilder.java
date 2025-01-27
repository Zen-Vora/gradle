/*
 * Copyright 2024 the original author or authors.
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

package org.gradle.api.problems.internal.deprecation;

import org.gradle.api.problems.Problem;
import org.gradle.api.problems.deprecation.DeprecateGenericSpec;
import org.gradle.api.problems.deprecation.DeprecateMethodSpec;
import org.gradle.api.problems.deprecation.DeprecatePluginSpec;
import org.gradle.api.problems.internal.DefaultProblemBuilder;
import org.gradle.api.problems.internal.InternalProblemBuilder;

import javax.annotation.Nullable;

class DefaultDeprecationBuilder implements DeprecateGenericSpec, DeprecatePluginSpec, DeprecateMethodSpec {
    private final InternalProblemBuilder builder;
    private final DefaultDeprecationData.Builder additionalDataBuilder;

    public DefaultDeprecationBuilder(InternalProblemBuilder builder) {
        this.builder = builder;
        this.additionalDataBuilder = new DefaultDeprecationData.Builder();
    }

    @Override
    public DefaultDeprecationBuilder replacedBy(String replacement) {
        additionalDataBuilder.replacedBy(replacement);
        return this;
    }

    @Override
    public DefaultDeprecationBuilder removedInVersion(String opaqueVersion) {
        additionalDataBuilder.removedIn(new DefaultOpaqueDeprecatedVersion(opaqueVersion));
        return this;
    }

    @Override
    public DeprecateGenericSpec removedInVersion(Integer major, @Nullable Integer minor, @Nullable Integer patch, @Nullable String qualifier) {
        additionalDataBuilder.removedIn(new DefaultSemverDeprecatedVersion(major, minor, patch, qualifier));
        return this;
    }

    @Override
    public DefaultDeprecationBuilder because(String reason) {
        additionalDataBuilder.because(reason);
        return this;
    }

    @Override
    public DefaultDeprecationBuilder withDetails(String details) {
        builder.details(details);
        return this;
    }

    public InternalProblemBuilder getProblemBuilder() {
        return builder;
    }

    public Problem build() {
        ((DefaultProblemBuilder)builder).additionalData(additionalDataBuilder.build()); // TODO (donat) remove once https://github.com/gradle/gradle/pull/32122/files is merged and the pr is rebased
        return builder.build();
    }
}
