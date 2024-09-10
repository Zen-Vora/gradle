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

package org.gradle.internal.declarativedsl.utils

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.declarative.dsl.model.annotations.ElementFactoryName
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType
import java.util.Locale
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KVariance
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.starProjectedType

object DclContainerMemberExtractionUtils {
    fun elementTypeFromNdocContainerType(containerType: Type): Type? =
        when (containerType) {
            is Class<*> -> {
                // can't be NDOC<T>, so go and find the supertype
                if (ndocClass.java.isAssignableFrom(containerType)) {
                    findNamedDomainObjectContainerTypeArgument(containerType)
                } else null
            }
            is ParameterizedType -> run {
                val rawClass = containerType.rawType as? Class<*> ?: return@run null
                if (rawClass == ndocClass.java) {
                    containerType.actualTypeArguments.singleOrNull()
                } else if (ndocClass.java.isAssignableFrom(rawClass)) {
                    findNamedDomainObjectContainerTypeArgument(containerType)
                } else null
            }
            else -> null
        }

    private fun findNamedDomainObjectContainerTypeArgument(containerSubtype: Type): Type? {
        val visited: HashSet<Class<*>> = hashSetOf()

        fun visitRawClass(rawClass: Class<*>, actualTypeArguments: List<Type>): Type? {
            if (rawClass == ndocClass.java) {
                return actualTypeArguments.singleOrNull()
            }

            if (!visited.add(rawClass)) {
                return null
            }

            fun typeParamIndex(typeVariable: TypeVariable<*>): Int =
                rawClass.typeParameters.indexOf(typeVariable)

            fun visitSuper(type: Type?): Type? {
                return when (type) {
                    is Class<*> -> visitRawClass(type, emptyList())
                    is ParameterizedType -> {
                        val actualSupertypeArgs = type.actualTypeArguments.map { arg ->
                            if (arg is TypeVariable<*>) {
                                val indexInOwner = typeParamIndex(arg)
                                if (indexInOwner == -1) {
                                    return@visitSuper null // can't find the type argument for the supertype
                                }
                                actualTypeArguments.getOrNull(indexInOwner) ?: return@visitSuper null
                            } else arg
                        }
                        val rawSupertype = type.rawType as? Class<*> ?: return null
                        visitRawClass(rawSupertype, actualSupertypeArgs)
                    }
                    else -> null
                }
            }

            return visitSuper(rawClass.genericSuperclass) ?: rawClass.genericInterfaces.firstNotNullOfOrNull { visitSuper(it) }
        }

        val result = when (containerSubtype) {
            is Class<*> -> visitRawClass(containerSubtype, emptyList())
            is ParameterizedType -> visitRawClass(containerSubtype.rawType as? Class<*> ?: return null, containerSubtype.actualTypeArguments.asList())
            else -> null
        }

        return when (result) {
            is WildcardType, is TypeVariable<*> -> null
            else -> result
        }
    }

    fun elementTypeFromNdocContainerType(containerType: KType): KType? =
        when (val containerClassifier = containerType.classifier) {
            ndocClass -> containerType.arguments.single().let { typeArgument ->
                if (typeArgument.variance == KVariance.INVARIANT)
                    typeArgument.type
                else null
            }

            is KClass<*> -> run {
                // Is some subclass of NDOC<T>?
                if (!containerClassifier.isSubclassOf(ndocClass))
                    return@run null // non-NDOC type

                fun findInvariantTypeArgumentFor(typeParameter: KTypeParameter): KType? =
                    (containerClassifier.allSupertypes + containerType).firstNotNullOfOrNull { supertype ->
                        val superClass = supertype.classifier as? KClass<*> ?: return@firstNotNullOfOrNull null
                        val index = superClass.typeParameters.indexOf(typeParameter).takeIf { it != -1 } ?: return@firstNotNullOfOrNull null
                        supertype.arguments[index].takeIf { it.variance == KVariance.INVARIANT }?.type
                    }

                fun resolveInvariantTypeArgumentFor(typeParameter: KTypeParameter): KType? {
                    var currentParameter = typeParameter

                    while (true) {
                        val argument = findInvariantTypeArgumentFor(currentParameter)
                        when (val classifier = argument?.classifier) {
                            null, currentParameter -> return null
                            is KClass<*> -> return argument
                            is KTypeParameter -> currentParameter = classifier
                        }
                    }
                }

                // We need to find where the type argument for NDOC<T> comes from
                val elementTypeArg = resolveInvariantTypeArgumentFor(NamedDomainObjectContainer::class.typeParameters.single())

                when (elementTypeArg?.classifier) {
                    is KClass<*> -> elementTypeArg.takeIf {
                        // Is non-parameterized. We can't support types like NDOC<Foo<T>>; that would require us to provide the T type argument from the accessor
                        it.arguments.none { elementArgArg -> elementArgArg.type is KTypeParameter }
                    }

                    else -> null // We can't support types like `NDOC<S>` where `S` is a type parameter itself. That would require an overload per `S` owner.
                }
            }

            else -> null // TODO: is a type-parameter `T : NDOC<Foo>` with a property like `val foo: T` a valid case?
        }

    fun elementFactoryFunctionNameFromElementType(elementType: KClass<*>): String =
        elementFactoryFunctionNameFromElementType(elementType.starProjectedType)

    fun elementFactoryFunctionNameFromElementType(elementType: KType): String =
        when (val annotation = (elementType.classifier as? KClass<*>)?.annotations.orEmpty().filterIsInstance<ElementFactoryName>().singleOrNull()) {
            null -> null
            else -> annotation.value.takeIf { it != "" } ?: error("@${ElementFactoryName::class.java.simpleName} must provide a value")
        } ?: (elementType.classifier as? KClass<*>)?.simpleName?.replaceFirstChar { it.lowercase(Locale.ROOT) }
        ?: (elementType.classifier as? KTypeParameter)?.name?.replaceFirstChar { it.lowercase(Locale.ROOT) }
        ?: error("cannot determine element factory name for unexpected container element type $elementType")

    private val ndocClass = NamedDomainObjectContainer::class
}
