/*
 * Copyright 2009 the original author or authors.
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

package org.gradle.external.javadoc;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.internal.provider.ProviderApiDeprecationLogger;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Console;
import org.gradle.api.tasks.IgnoreEmptyDirectories;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.internal.instrumentation.api.annotations.ReplacesEagerProperty;
import org.gradle.internal.instrumentation.api.annotations.ToBeReplacedByLazyProperty;
import org.gradle.process.ExecSpec;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Provides the core Javadoc options.
 */
public interface MinimalJavadocOptions {
    @Input
    @Optional
    @ReplacesEagerProperty
    Property<String> getOverview();

    MinimalJavadocOptions overview(String overview);

    @Input
    @Optional
    @ReplacesEagerProperty
    Property<JavadocMemberLevel> getMemberLevel();

    MinimalJavadocOptions showFromPublic();

    MinimalJavadocOptions showFromProtected();

    MinimalJavadocOptions showFromPackage();

    MinimalJavadocOptions showFromPrivate();

    MinimalJavadocOptions showAll();

    @Input
    @Optional
    @ReplacesEagerProperty
    Property<String> getDoclet();

    MinimalJavadocOptions doclet(String docletClass);

    @Classpath
    @ReplacesEagerProperty
    ConfigurableFileCollection getDocletpath();

    MinimalJavadocOptions docletpath(File... docletpath);

    @Input
    @Optional
    @ReplacesEagerProperty
    Property<String> getSource();

    MinimalJavadocOptions source(String source);

    @Internal
    @ReplacesEagerProperty
    ConfigurableFileCollection getClasspath();

    /**
     * The --module-path.
     *
     * @since 6.4
     */
    @Internal
    @ReplacesEagerProperty
    ConfigurableFileCollection getModulePath();

    /**
     * The --module-path.
     *
     * @since 6.4
     */
    MinimalJavadocOptions modulePath(List<File> classpath);

    MinimalJavadocOptions classpath(List<File> classpath);

    MinimalJavadocOptions classpath(File... classpath);

    @Classpath
    @ReplacesEagerProperty
    ConfigurableFileCollection getBootClasspath();

    MinimalJavadocOptions bootClasspath(File... bootClasspath);

    @InputFiles
    @Optional
    @IgnoreEmptyDirectories
    @PathSensitive(PathSensitivity.RELATIVE)
    @ReplacesEagerProperty
    ConfigurableFileCollection getExtDirs();

    MinimalJavadocOptions extDirs(File... extDirs);

    @Console
    @ReplacesEagerProperty
    Property<JavadocOutputLevel> getOutputLevel();

    MinimalJavadocOptions verbose();

    @Internal
    @ToBeReplacedByLazyProperty
    boolean isVerbose();

    MinimalJavadocOptions quiet();

    @Input
    @ReplacesEagerProperty
    Property<Boolean> getBreakIterator();

    /**
     * This method exists only for Kotlin source backward compatibility.
     */
    @Internal
    @Deprecated
    default Property<Boolean> getIsBreakIterator() {
        ProviderApiDeprecationLogger.logDeprecation(MinimalJavadocOptions.class, "getIsBreakIterator()", "getBreakIterator()");
        return getBreakIterator();
    }

    MinimalJavadocOptions breakIterator(boolean breakIterator);

    MinimalJavadocOptions breakIterator();

    @Input
    @Optional
    @ReplacesEagerProperty
    Property<String> getLocale();

    MinimalJavadocOptions locale(String locale);

    @Input
    @Optional
    @ReplacesEagerProperty
    Property<String> getEncoding();

    MinimalJavadocOptions encoding(String encoding);

    @ToBeReplacedByLazyProperty
    @Nullable @Optional @Input
    List<String> getJFlags();

    void setJFlags(@Nullable List<String> jFlags);

    MinimalJavadocOptions jFlags(String... jFlags);

    @ToBeReplacedByLazyProperty
    @Nullable @Optional @PathSensitive(PathSensitivity.NONE) @InputFiles
    List<File> getOptionFiles();

    void setOptionFiles(@Nullable List<File> optionFiles);

    MinimalJavadocOptions optionFiles(File... argumentFiles);

    @Internal
    @ToBeReplacedByLazyProperty
    File getDestinationDirectory();

    void setDestinationDirectory(@Nullable File directory);

    MinimalJavadocOptions destinationDirectory(File directory);

    @ToBeReplacedByLazyProperty
    @Nullable @Optional @Input
    String getWindowTitle();

    void setWindowTitle(@Nullable String windowTitle);

    StandardJavadocDocletOptions windowTitle(String windowTitle);

    @ToBeReplacedByLazyProperty
    @Nullable @Optional @Input
    String getHeader();

    void setHeader(@Nullable String header);

    StandardJavadocDocletOptions header(String header);

    void write(File outputFile) throws IOException;

    @Nullable
    @Internal
    @ToBeReplacedByLazyProperty
    List<String> getSourceNames();

    void setSourceNames(@Nullable List<String> sourceNames);

    MinimalJavadocOptions sourceNames(String... sourceNames);

    void contributeCommandLineOptions(ExecSpec execHandleBuilder);
}
