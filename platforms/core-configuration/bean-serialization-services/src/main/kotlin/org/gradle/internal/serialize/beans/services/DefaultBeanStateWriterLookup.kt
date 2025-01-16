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

package org.gradle.internal.serialize.beans.services

import org.gradle.internal.serialize.graph.BeanStateWriter
import org.gradle.internal.serialize.graph.BeanStateWriterLookup
import org.gradle.internal.service.scopes.Scope
import org.gradle.internal.service.scopes.ServiceScope
import java.util.concurrent.ConcurrentHashMap


@ServiceScope(Scope.BuildTree::class)
class DefaultBeanStateWriterLookup : BeanStateWriterLookup {
    private
    val beanPropertyWriters = ConcurrentHashMap<Class<*>, BeanStateWriter>()

    override fun beanStateWriterFor(beanType: Class<*>): BeanStateWriter =
        beanPropertyWriters.computeIfAbsent(beanType, ::BeanPropertyWriter)
}
