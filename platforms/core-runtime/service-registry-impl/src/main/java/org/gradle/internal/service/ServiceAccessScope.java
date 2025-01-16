/*
 * Copyright 2025 the original author or authors.
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

package org.gradle.internal.service;

import javax.annotation.Nullable;

/**
 * Scopes determine which services can be accessed on the level of individual service providers.
 *
 * @see ServiceAccess
 */
interface ServiceAccessScope {

    /**
     * Return true if the given token can access services in this scope.
     * <p>
     * The null token value represents access without a token.
     */
    boolean contains(@Nullable ServiceAccessToken token);

}
