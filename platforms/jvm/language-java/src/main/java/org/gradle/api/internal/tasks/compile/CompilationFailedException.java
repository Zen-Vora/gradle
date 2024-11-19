/*
 * Copyright 2023 the original author or authors.
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
package org.gradle.api.internal.tasks.compile;

import org.gradle.api.problems.Problem;
import org.gradle.api.problems.internal.ProblemAwareFailure;
import org.gradle.internal.exceptions.CompilationFailedIndicator;
import org.gradle.problems.internal.rendering.ProblemRenderer;

import javax.annotation.Nullable;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CompilationFailedException extends RuntimeException implements CompilationFailedIndicator, ProblemAwareFailure {

    public static final String RESOLUTION_MESSAGE = "Check your code and dependencies to fix the compilation error(s)";
    public static final String COMPILATION_FAILED_DETAILS_ABOVE = "Compilation failed; see the compiler error output for details.";
    public static final String COMPILATION_FAILED_DETAILS_BELOW = "Compilation failed; see the compiler output below.";

    private final ApiCompilerResult compilerPartialResult;
    private final List<Problem> reportedProblems;

    public CompilationFailedException() {
        this((ApiCompilerResult) null);
    }

    public CompilationFailedException(int exitCode) {
        super(String.format("Compilation failed with exit code %d; see the compiler error output for details.", exitCode));
        this.compilerPartialResult = null;
        this.reportedProblems = Collections.emptyList();
    }

    public CompilationFailedException(Throwable cause) {
        super(cause);
        this.compilerPartialResult = null;
        this.reportedProblems = Collections.emptyList();
    }

    public CompilationFailedException(@Nullable ApiCompilerResult result) {
        super(COMPILATION_FAILED_DETAILS_ABOVE);
        this.compilerPartialResult = result;
        this.reportedProblems = Collections.emptyList();
    }

    public CompilationFailedException(ApiCompilerResult result, List<Problem> reportedProblems, String diagnosticCounts) {
        super(exceptionMessage(COMPILATION_FAILED_DETAILS_BELOW + System.lineSeparator(), reportedProblems, diagnosticCounts));
        this.compilerPartialResult = result;
        this.reportedProblems = reportedProblems;
    }

    private static String exceptionMessage(String prefix, List<Problem> problems, String diagnosticCounts) {
        StringWriter result = new StringWriter();
        result.append(prefix);
        new ProblemRenderer(result).render(problems);
        result.append(diagnosticCounts);
        return result.toString();
    }

    public Optional<ApiCompilerResult> getCompilerPartialResult() {
        return Optional.ofNullable(compilerPartialResult);
    }

    @Override
    public Collection<Problem> getProblems() {
        return reportedProblems;
    }
}
