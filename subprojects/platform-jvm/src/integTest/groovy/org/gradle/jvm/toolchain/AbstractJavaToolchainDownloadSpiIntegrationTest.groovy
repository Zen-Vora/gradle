/*
 * Copyright 2022 the original author or authors.
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

package org.gradle.jvm.toolchain

import org.gradle.integtests.fixtures.AbstractIntegrationSpec

class AbstractJavaToolchainDownloadSpiIntegrationTest extends AbstractIntegrationSpec {

    protected static String applyToolchainResolverPlugin(String resolverClass, String code, String pluginName = null) {
        if (pluginName == null) {
            pluginName = resolverClass + "Plugin"
        }
        """
            public abstract class ${pluginName} implements Plugin<Settings> {
                @Inject
                protected abstract JavaToolchainResolverRegistry getToolchainResolverRegistry();

                void apply(Settings settings) {
                    settings.getPlugins().apply("jvm-toolchain-management");

                    JavaToolchainResolverRegistry registry = getToolchainResolverRegistry();
                    registry.register(${resolverClass}.class);
                }
            }

            ${code}

            apply plugin: ${pluginName}
        """
    }

}
