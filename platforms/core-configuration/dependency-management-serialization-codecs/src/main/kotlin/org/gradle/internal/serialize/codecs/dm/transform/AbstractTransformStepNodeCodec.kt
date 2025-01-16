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

package org.gradle.internal.serialize.codecs.dm.transform

import org.gradle.api.internal.artifacts.transform.TransformStepNode
import org.gradle.internal.serialize.graph.Codec
import org.gradle.internal.serialize.graph.ReadContext
import org.gradle.internal.serialize.graph.WriteContext
import org.gradle.internal.serialize.graph.decodePreservingSharedIdentity
import org.gradle.internal.serialize.graph.encodePreservingSharedIdentityOf


abstract class AbstractTransformStepNodeCodec<T : TransformStepNode> : Codec<T> {

    override suspend fun WriteContext.encode(value: T) {
        encodePreservingSharedIdentityOf(value) { doEncode(value) }
    }

    override suspend fun ReadContext.decode(): T =
        decodePreservingSharedIdentity { doDecode() }

    protected
    abstract suspend fun WriteContext.doEncode(value: T)

    protected
    abstract suspend fun ReadContext.doDecode(): T
}
