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

package org.gradle.internal.component.resolution.failure;

import org.gradle.internal.component.resolution.failure.describer.AmbiguousArtifactTransformFailureDescriber;
import org.gradle.internal.component.resolution.failure.describer.AmbiguousArtifactVariantsFailureDescriber;
import org.gradle.internal.component.resolution.failure.describer.AmbiguousGraphVariantsFailureDescriber;
import org.gradle.internal.component.resolution.failure.describer.IncompatibleArtifactVariantsFailureDescriber;
import org.gradle.internal.component.resolution.failure.describer.IncompatibleGraphVariantsFailureDescriber;
import org.gradle.internal.component.resolution.failure.describer.IncompatibleRequestedConfigurationFailureDescriber;
import org.gradle.internal.component.resolution.failure.describer.InvalidMultipleVariantsFailureDescriber;
import org.gradle.internal.component.resolution.failure.describer.MissingAttributeAmbiguousGraphVariantsFailureDescriber;
import org.gradle.internal.component.resolution.failure.describer.NoMatchingCapabilitiesFailureDescriber;
import org.gradle.internal.component.resolution.failure.describer.RequestedConfigurationNotFoundFailureDescriber;
import org.gradle.internal.component.resolution.failure.describer.ResolutionFailureDescriber;
import org.gradle.internal.component.resolution.failure.describer.UnknownArtifactSelectionFailureDescriber;
import org.gradle.internal.component.resolution.failure.type.AmbiguousArtifactTransformsFailure;
import org.gradle.internal.component.resolution.failure.type.NoCompatibleArtifactFailure;
import org.gradle.internal.component.resolution.failure.type.NoCompatibleVariantsFailure;
import org.gradle.internal.component.resolution.failure.type.IncompatibleMultipleNodesValidationFailure;
import org.gradle.internal.component.resolution.failure.type.ConfigurationNotCompatibleFailure;
import org.gradle.internal.component.resolution.failure.type.NoVariantsWithMatchingCapabilitiesFailure;
import org.gradle.internal.component.resolution.failure.type.ConfigurationDoesNotExistFailure;
import org.gradle.internal.component.resolution.failure.interfaces.ResolutionFailure;
import org.gradle.internal.component.resolution.failure.type.UnknownArtifactSelectionFailure;
import org.gradle.internal.component.resolution.failure.type.AmbiguousVariantsFailure;
import org.gradle.internal.instantiation.InstanceGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * An ordered registry of {@link ResolutionFailureDescriber} instances that can be queried
 * by the {@link ResolutionFailure} type they can describe.
 */
public final class ResolutionFailureDescriberRegistry {
    private final InstanceGenerator instanceGenerator;
    private final LinkedHashMap<Class<? extends ResolutionFailure>, List<ResolutionFailureDescriber<?>>> describers = new LinkedHashMap<>();

    private ResolutionFailureDescriberRegistry(InstanceGenerator instanceGenerator) {
        this.instanceGenerator = instanceGenerator;
    }

    /**
     * Creates a new, empty registry of {@link ResolutionFailureDescriber}s.
     *
     * @param instanceGenerator The instance generator to use to create describers
     * @return a new, empty registry instance
     */
    public static ResolutionFailureDescriberRegistry emptyRegistry(InstanceGenerator instanceGenerator) {
        return new ResolutionFailureDescriberRegistry(instanceGenerator);
    }

    /**
     * Creates a new, registry of {@link ResolutionFailureDescriber}s containing the default internal list of describers
     * that can describe the complete set of {@link ResolutionFailure} types used by Gradle.
     *
     * @param instanceGenerator The instance generator to use to create describers
     * @return a new registry instance with the default describers registered
     */
    public static ResolutionFailureDescriberRegistry standardRegistry(InstanceGenerator instanceGenerator) {
        ResolutionFailureDescriberRegistry registry = new ResolutionFailureDescriberRegistry(instanceGenerator);

        registry.registerDescriber(AmbiguousVariantsFailure.class, MissingAttributeAmbiguousGraphVariantsFailureDescriber.class); // Added ahead of AmbiguousGraphVariantsFailureDescriber so the more specific ambiguity case is checked first
        registry.registerDescriber(AmbiguousVariantsFailure.class, AmbiguousGraphVariantsFailureDescriber.class);
        registry.registerDescriber(NoCompatibleVariantsFailure.class, IncompatibleGraphVariantsFailureDescriber.class);

        registry.registerDescriber(AmbiguousVariantsFailure.class, AmbiguousArtifactVariantsFailureDescriber.class);
        registry.registerDescriber(NoCompatibleArtifactFailure.class, IncompatibleArtifactVariantsFailureDescriber.class);
        registry.registerDescriber(IncompatibleMultipleNodesValidationFailure.class, InvalidMultipleVariantsFailureDescriber.class);
        registry.registerDescriber(AmbiguousArtifactTransformsFailure.class, AmbiguousArtifactTransformFailureDescriber.class);

        registry.registerDescriber(ConfigurationNotCompatibleFailure.class, IncompatibleRequestedConfigurationFailureDescriber.class);
        registry.registerDescriber(ConfigurationDoesNotExistFailure.class, RequestedConfigurationNotFoundFailureDescriber.class);

        registry.registerDescriber(NoVariantsWithMatchingCapabilitiesFailure.class, NoMatchingCapabilitiesFailureDescriber.class);

        registry.registerDescriber(UnknownArtifactSelectionFailure.class, UnknownArtifactSelectionFailureDescriber.class);

        return registry;
    }

    /**
     * Returns the list of {@link ResolutionFailureDescriber}s registered for the given {@link ResolutionFailure} type.
     *
     * @param failureType The type of failure to describe
     * @param <FAILURE> The type of failure to describe
     * @return The list of describers registered for the given failure type
     */
    public <FAILURE extends ResolutionFailure> List<ResolutionFailureDescriber<FAILURE>> getDescribers(Class<FAILURE> failureType) {
        List<ResolutionFailureDescriber<FAILURE>> result = new ArrayList<>();
        describers.getOrDefault(failureType, Collections.emptyList()).forEach(d -> {
            @SuppressWarnings("unchecked") ResolutionFailureDescriber<FAILURE> typedDescriber = (ResolutionFailureDescriber<FAILURE>) d;
            result.add(typedDescriber);
        });
        return result;
    }

    /**
     * Adds a {@link ResolutionFailureDescriber} to the custom describers
     * contained in this registry for the given {@link ResolutionFailure} type.
     *
     * @param failureType The type of failure to describe
     * @param describerType A describer that can potentially describe failures of the given type
     * @param <FAILURE> The type of failure to describe
     */
    public <FAILURE extends ResolutionFailure> void registerDescriber(Class<FAILURE> failureType, Class<? extends ResolutionFailureDescriber<FAILURE>> describerType) {
        ResolutionFailureDescriber<?> describer = instanceGenerator.newInstance(describerType);
        describers.computeIfAbsent(failureType, k -> new ArrayList<>()).add(describer);
    }
}
