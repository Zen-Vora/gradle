package org.gradle.kotlin.dsl.integration

import org.codehaus.groovy.runtime.StringGroovyMethods
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.integtests.fixtures.RepoScriptBlockUtil
import org.gradle.kotlin.dsl.fixtures.classEntriesFor
import org.gradle.kotlin.dsl.fixtures.normalisedPath
import org.gradle.test.fixtures.dsl.GradleDsl
import org.gradle.test.fixtures.file.LeaksFileHandles
import org.gradle.util.GradleVersion
import org.gradle.util.internal.TextUtil.normaliseFileSeparators
import org.gradle.util.internal.ToBeImplemented
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import spock.lang.Issue
import java.io.File


@LeaksFileHandles("Kotlin Compiler Daemon working directory")
class PrecompiledScriptPluginIntegrationTest : AbstractPluginIntegrationTest() {

    @Test
    fun `generated code follows kotlin-dsl coding conventions`() {

        assumeNonEmbeddedGradleExecuter() // ktlint plugin issue in embedded mode

        withBuildScript(
            """
            plugins {
                `kotlin-dsl`
                id("org.gradle.kotlin-dsl.ktlint-convention") version "0.8.0"
            }

            $repositoriesBlock
            """
        )

        withPrecompiledKotlinScript(
            "plugin-without-package.gradle.kts",
            """
            plugins {
                org.gradle.base
            }

            """.trimIndent()
        )
        withPrecompiledKotlinScript(
            "test/gradle/plugins/plugin-with-package.gradle.kts",
            """
            package test.gradle.plugins

            plugins {
                org.gradle.base
            }

            """.trimIndent()
        )

        build("generateScriptPluginAdapters")

        build("ktlintCheck", "-x", "ktlintKotlinScriptCheck")
    }

    @Test
    fun `precompiled script plugins tasks are cached and relocatable`() {

        val firstLocation = "first-location"
        val secondLocation = "second-location"
        val cacheDir = newDir("cache-dir")

        withDefaultSettingsIn(firstLocation).appendText(
            """
            rootProject.name = "test"
            buildCache {
                local {
                    directory = file("${cacheDir.normalisedPath}")
                }
            }
            """
        )
        withBuildScriptIn(
            firstLocation,
            """
            plugins { `kotlin-dsl` }
            ${RepoScriptBlockUtil.mavenCentralRepository(GradleDsl.KOTLIN)}
            """
        )

        withFile("$firstLocation/src/main/kotlin/plugin-without-package.gradle.kts")
        withFile(
            "$firstLocation/src/main/kotlin/plugins/plugin-with-package.gradle.kts",
            """
            package plugins
            """
        )


        val firstDir = existing(firstLocation)
        val secondDir = newDir(secondLocation)
        firstDir.copyRecursively(secondDir)

        val cachedTasks = listOf(
            ":extractPrecompiledScriptPluginPlugins",
            ":generateExternalPluginSpecBuilders",
            ":compilePluginsBlocks",
            ":generateScriptPluginAdapters"
        )
        val downstreamKotlinCompileTask = ":compileKotlin"

        // TODO: the Kotlin compile tasks check for cacheability using Task.getProject
        executer.beforeExecute {
            it.withBuildJvmOpts("-Dorg.gradle.configuration-cache.internal.task-execution-access-pre-stable=true")
        }

        build(firstDir, "classes", "--build-cache").apply {
            cachedTasks.forEach { assertTaskExecuted(it) }
            assertTaskExecuted(downstreamKotlinCompileTask)
        }

        build(firstDir, "classes", "--build-cache").apply {
            cachedTasks.forEach { assertOutputContains("$it UP-TO-DATE") }
            assertOutputContains("$downstreamKotlinCompileTask UP-TO-DATE")
        }

        build(secondDir, "classes", "--build-cache").apply {
            cachedTasks.forEach { assertOutputContains("$it FROM-CACHE") }
            assertOutputContains("$downstreamKotlinCompileTask FROM-CACHE")
        }
    }

    @Test
    fun `precompiled script plugins adapters generation clean stale outputs`() {

        withBuildScript(
            """
            plugins { `kotlin-dsl` }
            """
        )

        val fooScript = withFile("src/main/kotlin/foo.gradle.kts", "")

        build("generateScriptPluginAdapters")
        assertTrue(existing("build/generated-sources/kotlin-dsl-plugins/kotlin/FooPlugin.kt").isFile)

        fooScript.renameTo(fooScript.parentFile.resolve("bar.gradle.kts"))

        build("generateScriptPluginAdapters")
        assertFalse(existing("build/generated-sources/kotlin-dsl-plugins/kotlin/FooPlugin.kt").exists())
        assertTrue(existing("build/generated-sources/kotlin-dsl-plugins/kotlin/BarPlugin.kt").isFile)
    }

    @Test
    fun `can apply precompiled script plugin from groovy script`() {

        withKotlinBuildSrc()
        withFile(
            "buildSrc/src/main/kotlin/my-plugin.gradle.kts",
            """
            tasks.register("myTask") {}
            """
        )

        withDefaultSettings()
        withFile(
            "build.gradle",
            """
            plugins {
                id 'my-plugin'
            }
            """
        )

        build("myTask")
    }

    @Test
    fun `accessors are available after script body change`() {

        withKotlinBuildSrc()
        val myPluginScript = withFile(
            "buildSrc/src/main/kotlin/my-plugin.gradle.kts",
            """
            plugins { base }

            base {
                archivesName.set("my")
            }

            println("base")
            """
        )

        withDefaultSettings()
        withBuildScript(
            """
            plugins {
                `my-plugin`
            }
            """
        )

        build("help").apply {
            assertThat(output, containsString("base"))
        }

        myPluginScript.appendText(
            """

            println("modified")
            """.trimIndent()
        )

        build("help").apply {
            assertThat(output, containsString("base"))
            assertThat(output, containsString("modified"))
        }
    }

    @Test
    fun `accessors are available after re-running tasks`() {

        withKotlinBuildSrc()
        withFile(
            "buildSrc/src/main/kotlin/my-plugin.gradle.kts",
            """
            plugins { base }

            base {
                archivesName.set("my")
            }
            """
        )

        withDefaultSettings()
        withBuildScript(
            """
            plugins {
                `my-plugin`
            }
            """
        )

        build("clean")

        build("clean", "--rerun-tasks")
    }

    @Test
    fun `accessors are available after registering plugin`() {
        withSettings(
            """
            $defaultSettingsScript

            include("consumer", "producer")
            """
        )

        withBuildScript(
            """
            plugins {
                `java-library`
            }

            allprojects {
                $repositoriesBlock
            }

            dependencies {
                api(project(":consumer"))
            }
            """
        )

        withFolders {

            "consumer" {
                withFile(
                    "build.gradle.kts",
                    """
                    plugins {
                        id("org.gradle.kotlin.kotlin-dsl")
                    }

                    // Forces dependencies to be visible as jars
                    // so we get plugin spec accessors
                    ${forceJarsOnCompileClasspath()}

                    dependencies {
                        implementation(project(":producer"))
                    }
                    """
                )

                withFile(
                    "src/main/kotlin/consumer-plugin.gradle.kts",
                    """
                    plugins { `producer-plugin` }
                    """
                )
            }

            "producer" {
                withFile(
                    "build.gradle",
                    """
                    plugins {
                        id("java-library")
                        id("java-gradle-plugin")
                    }
                    """
                )
                withFile(
                    "src/main/java/producer/ProducerPlugin.java",
                    """
                    package producer;
                    public class ProducerPlugin {
                        // Using internal class to verify https://github.com/gradle/gradle/issues/17619
                        public static class Implementation implements ${nameOf<Plugin<*>>()}<${nameOf<Project>()}> {
                            @Override public void apply(${nameOf<Project>()} target) {}
                        }
                    }
                    """
                )
            }
        }

        buildAndFail("assemble").run {
            // Accessor is not available on the first run as the plugin hasn't been registered.
            assertTaskExecuted(
                ":consumer:generateExternalPluginSpecBuilders"
            )
        }

        existing("producer/build.gradle").run {
            appendText(
                """
                gradlePlugin {
                    plugins {
                        producer {
                            id = 'producer-plugin'
                            implementationClass = 'producer.ProducerPlugin${'$'}Implementation'
                        }
                    }
                }
                """
            )
        }

        // Accessor becomes available after registering the plugin.
        build("assemble").run {
            assertTaskExecuted(
                ":consumer:generateExternalPluginSpecBuilders"
            )
        }
    }

    private
    inline fun <reified T> nameOf() = T::class.qualifiedName

    @Test
    fun `accessors are available after renaming precompiled script plugin from project dependency`() {

        withSettings(
            """
            $defaultSettingsScript

            include("consumer", "producer")
            """
        )

        withBuildScript(
            """
            plugins {
                `java-library`
                `kotlin-dsl` apply false
            }

            allprojects {
                $repositoriesBlock
            }

            dependencies {
                api(project(":consumer"))
            }
            """
        )

        withFolders {

            "consumer" {
                withFile(
                    "build.gradle.kts",
                    """
                    plugins {
                        id("org.gradle.kotlin.kotlin-dsl")
                    }

                    // Forces dependencies to be visible as jars
                    // to reproduce the failure that happens in forkingIntegTest.
                    // Incidentally, this also allows us to write `stable-producer-plugin`
                    // in the plugins block below instead of id("stable-producer-plugin").
                    ${forceJarsOnCompileClasspath()}

                    dependencies {
                        implementation(project(":producer"))
                    }
                    """
                )

                withFile(
                    "src/main/kotlin/consumer-plugin.gradle.kts",
                    """
                    plugins { `stable-producer-plugin` }
                    """
                )
            }

            "producer" {
                withFile(
                    "build.gradle.kts",
                    """
                    plugins { id("org.gradle.kotlin.kotlin-dsl") }
                    """
                )
                withFile("src/main/kotlin/changing-producer-plugin.gradle.kts")
                withFile(
                    "src/main/kotlin/stable-producer-plugin.gradle.kts",
                    """
                    println("*42*")
                    """
                )
            }
        }

        build("assemble").run {
            assertTaskExecuted(
                ":consumer:generateExternalPluginSpecBuilders"
            )
        }

        existing("producer/src/main/kotlin/changing-producer-plugin.gradle.kts").run {
            renameTo(resolveSibling("changed-producer-plugin.gradle.kts"))
        }

        build("assemble").run {
            assertTaskExecuted(
                ":consumer:generateExternalPluginSpecBuilders"
            )
        }
    }

    private
    fun forceJarsOnCompileClasspath() = """
        configurations {
            compileClasspath {
                attributes {
                    attribute(
                        LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                        objects.named(LibraryElements.JAR)
                    )
                }
            }
        }
    """

    @Test
    fun `applied precompiled script plugin is reloaded upon change`() {
        // given:
        withFolders {
            "build-logic" {
                withFile(
                    "settings.gradle.kts",
                    """
                        $defaultSettingsScript
                        include("producer", "consumer")
                    """
                )
                withFile(
                    "build.gradle.kts",
                    """
                        plugins {
                            `kotlin-dsl` apply false
                        }

                        subprojects {
                            apply(plugin = "org.gradle.kotlin.kotlin-dsl")
                            $repositoriesBlock
                        }

                        project(":consumer") {
                            dependencies {
                                "implementation"(project(":producer"))
                            }
                        }
                    """
                )

                withFile(
                    "producer/src/main/kotlin/producer-plugin.gradle.kts",
                    """
                        println("*version 1*")
                    """
                )
                withFile(
                    "consumer/src/main/kotlin/consumer-plugin.gradle.kts",
                    """
                        plugins { id("producer-plugin") }
                    """
                )
            }
        }
        withSettings(
            """
                includeBuild("build-logic")
            """
        )
        withBuildScript(
            """
                plugins { id("consumer-plugin") }
            """
        )

        // when:
        build("help").run {
            // then:
            assertThat(
                output.count("*version 1*"),
                equalTo(1)
            )
        }

        // when:
        file("build-logic/producer/src/main/kotlin/producer-plugin.gradle.kts").text = """
            println("*version 2*")
        """
        build("help").run {
            // then:
            assertThat(
                output.count("*version 2*"),
                equalTo(1)
            )
        }
    }

    private
    fun CharSequence.count(text: CharSequence): Int =
        StringGroovyMethods.count(this, text)

    @Test
    @Issue("https://github.com/gradle/gradle/issues/15416")
    fun `can use an empty plugins block in precompiled settings plugin`() {
        withFolders {
            "build-logic" {
                withFile("settings.gradle.kts", defaultSettingsScript)
                withFile(
                    "build.gradle.kts",
                    """
                        plugins {
                            `kotlin-dsl`
                        }
                        $repositoriesBlock
                    """
                )
                withFile(
                    "src/main/kotlin/my-plugin.settings.gradle.kts",
                    """
                        plugins {
                        }
                        println("my-plugin settings plugin applied")
                    """
                )
            }
        }
        withSettings(
            """
                pluginManagement {
                    includeBuild("build-logic")
                }
                plugins {
                    id("my-plugin")
                }
            """
        )

        build("help").run {
            assertThat(output, containsString("my-plugin settings plugin applied"))
        }
    }

    @Test
    @Issue("https://github.com/gradle/gradle/issues/15416")
    fun `can apply a plugin from the same project in precompiled settings plugin`() {
        withFolders {
            "build-logic" {
                withFile("settings.gradle.kts", defaultSettingsScript)
                withFile(
                    "build.gradle.kts",
                    """
                        plugins {
                            `kotlin-dsl`
                        }
                        $repositoriesBlock
                    """
                )
                withFile(
                    "src/main/kotlin/base-plugin.settings.gradle.kts",
                    """
                        println("base-plugin settings plugin applied")
                    """
                )
                withFile(
                    "src/main/kotlin/my-plugin.settings.gradle.kts",
                    """
                        plugins {
                            id("base-plugin")
                        }
                        println("my-plugin settings plugin applied")
                    """
                )
            }
        }
        withSettings(
            """
                pluginManagement {
                    includeBuild("build-logic")
                }
                plugins {
                    id("my-plugin")
                }
            """
        )

        build("help").run {
            assertThat(output, containsString("base-plugin settings plugin applied"))
            assertThat(output, containsString("my-plugin settings plugin applied"))
        }
    }

    @Test
    @Issue("https://github.com/gradle/gradle/issues/15416")
    fun `can apply a plugin from a repository in precompiled settings plugin`() {
        withFolders {
            "external-plugin" {
                withFile("settings.gradle.kts", defaultSettingsScript)
                withFile(
                    "build.gradle.kts",
                    """
                        plugins {
                            `kotlin-dsl`
                            id("maven-publish")
                        }
                        $repositoriesBlock
                        publishing {
                            repositories {
                                maven {
                                    url = uri("maven-repo")
                                }
                            }
                        }
                        group = "test"
                        version = "42"
                    """
                )
                withFile(
                    "src/main/kotlin/base-plugin.settings.gradle.kts",
                    """
                        println("base-plugin settings plugin applied")
                    """
                )
            }
            "build-logic" {
                withFile("settings.gradle.kts", defaultSettingsScript)
                withFile(
                    "build.gradle.kts",
                    """
                        plugins {
                            `kotlin-dsl`
                        }
                        repositories {
                            gradlePluginPortal()
                            maven {
                                url = uri("../external-plugin/maven-repo")
                            }
                        }
                        dependencies {
                             implementation("test:external-plugin:42")
                        }
                    """
                )
                withFile(
                    "src/main/kotlin/my-plugin.settings.gradle.kts",
                    """
                        plugins {
                            id("base-plugin")
                        }
                        println("my-plugin settings plugin applied")
                    """
                )
            }
        }
        withSettings(
            """
                pluginManagement {
                    repositories {
                        maven {
                            url = uri("external-plugin/maven-repo")
                        }
                    }
                    includeBuild("build-logic")
                }
                plugins {
                    id("my-plugin")
                }
            """
        )

        build(file("external-plugin"), "publish")

        build("help").run {
            assertThat(output, containsString("base-plugin settings plugin applied"))
            assertThat(output, containsString("my-plugin settings plugin applied"))
        }
    }

    @Test
    fun `should not allow precompiled plugin to conflict with core plugin`() {
        withKotlinBuildSrc()
        withFile(
            "buildSrc/src/main/kotlin/java.gradle.kts",
            """
            tasks.register("myTask") {}
            """
        )
        withDefaultSettings()
        withFile(
            "build.gradle",
            """
            plugins {
                java
            }
            """
        )

        val error = buildAndFail("help")

        error.assertHasCause(
            "The precompiled plugin (${"src/main/kotlin/java.gradle.kts".replace("/", File.separator)}) conflicts with the core plugin 'java'. Rename your plugin.\n\n"
                + "See https://docs.gradle.org/${GradleVersion.current().version}/userguide/custom_plugins.html#sec:precompiled_plugins for more details."
        )
    }

    @Test
    fun `should not allow precompiled plugin to have org-dot-gradle prefix`() {
        withKotlinBuildSrc()
        withFile(
            "buildSrc/src/main/kotlin/org.gradle.my-plugin.gradle.kts",
            """
            tasks.register("myTask") {}
            """
        )
        withDefaultSettings()

        val error = buildAndFail("help")

        error.assertHasCause(
            "The precompiled plugin (${"src/main/kotlin/org.gradle.my-plugin.gradle.kts".replace("/", File.separator)}) cannot start with 'org.gradle' or be in the 'org.gradle' package.\n\n"
                + "See https://docs.gradle.org/${GradleVersion.current().version}/userguide/custom_plugins.html#sec:precompiled_plugins for more details."
        )
    }

    @Test
    fun `should not allow precompiled plugin to be in org-dot-gradle package`() {
        withKotlinBuildSrc()
        withFile(
            "buildSrc/src/main/kotlin/org/gradle/my-plugin.gradle.kts",
            """
            package org.gradle

            tasks.register("myTask") {}
            """
        )
        withDefaultSettings()

        val error = buildAndFail("help")

        error.assertHasCause(
            "The precompiled plugin (${"src/main/kotlin/org/gradle/my-plugin.gradle.kts".replace("/", File.separator)}) cannot start with 'org.gradle' or be in the 'org.gradle' package.\n\n"
                + "See https://docs.gradle.org/${GradleVersion.current().version}/userguide/custom_plugins.html#sec:precompiled_plugins for more details."
        )
    }

    @Test
    fun `should compile correctly with Kotlin explicit api mode`() {
        assumeNonEmbeddedGradleExecuter()
        withBuildScript(
            """
            plugins {
                `kotlin-dsl`
            }

            $repositoriesBlock

            kotlin {
                explicitApi()
            }
            """
        )
        withPrecompiledKotlinScript(
            "my-plugin.gradle.kts",
            """
            tasks.register("myTask") {}
            """
        )

        compileKotlin()
    }

    @Test
    @Issue("https://github.com/gradle/gradle/issues/22091")
    fun `does not add extra task actions to kotlin compilation task`() {
        assumeNonEmbeddedGradleExecuter()
        withKotlinDslPlugin().appendText("""
            gradle.taskGraph.whenReady {
                val compileKotlinActions = allTasks.single { it.path == ":compileKotlin" }.actions.size
                require(compileKotlinActions == 1) {
                    ":compileKotlin has ${'$'}compileKotlinActions actions, expected 1"
                }
            }
        """)
        withPrecompiledKotlinScript("my-plugin.gradle.kts", "")

        compileKotlin()
    }

    @Test
    @Issue("https://github.com/gradle/gradle/issues/23576")
    @ToBeImplemented
    fun `can compile precompiled scripts with compileOnly dependency`() {

        fun withPluginJar(fileName: String, versionString: String): File =
            withZip(
                fileName,
                classEntriesFor(MyPlugin::class.java, MyTask::class.java) + sequenceOf(
                    "META-INF/gradle-plugins/my-plugin.properties" to "implementation-class=org.gradle.kotlin.dsl.integration.MyPlugin".toByteArray(),
                    "my-plugin-version.txt" to versionString.toByteArray(),
                )
            )

        val pluginJarV1 = withPluginJar("my-plugin-1.0.jar", "1.0")
        val pluginJarV2 = withPluginJar("my-plugin-2.0.jar", "2.0")

        withBuildScriptIn("buildSrc", """
            plugins {
                `kotlin-dsl`
            }

            $repositoriesBlock

            dependencies {
                compileOnly(files("${normaliseFileSeparators(pluginJarV1.absolutePath)}"))
            }
        """)
        val precompiledScript = withFile("buildSrc/src/main/kotlin/my-precompiled-script.gradle.kts", """
            plugins {
                id("my-plugin")
            }
        """)

        withBuildScript("""
            buildscript {
                dependencies {
                    classpath(files("${normaliseFileSeparators(pluginJarV2.absolutePath)}"))
                }
            }
            plugins {
                id("my-precompiled-script")
            }
        """)

        buildAndFail("action").apply {
            assertHasFailure("Plugin [id: 'my-plugin'] was not found in any of the following sources") {
                assertHasErrorOutput("Precompiled script plugin '${precompiledScript.absolutePath}' line: 1")
            }
        }

        // Once implemented:
        // build("action").apply {
        //     assertOutputContains("Applied plugin 2.0")
        // }
    }

    @Test
    @Issue("https://github.com/gradle/gradle/issues/23564")
    fun `respects offline start parameter on synthetic builds for accessors generation`() {

        withSettings("""include("producer", "consumer")""")

        withKotlinDslPluginIn("producer")
        withFile("producer/src/main/kotlin/offline.gradle.kts", """
            if (!gradle.startParameter.isOffline) throw IllegalStateException("Build is not offline!")
        """)

        withKotlinDslPluginIn("consumer").appendText("""
           dependencies { implementation(project(":producer")) }
        """)
        withFile("consumer/src/main/kotlin/my-plugin.gradle.kts", """
            plugins { id("offline") }
        """)

        buildAndFail(":consumer:generatePrecompiledScriptPluginAccessors").apply {
            assertHasFailure("An exception occurred applying plugin request [id: 'offline']") {
                assertHasCause("Build is not offline!")
            }
        }

        build(":consumer:generatePrecompiledScriptPluginAccessors", "--offline")
    }

    @Test
    @Issue("https://github.com/gradle/gradle/issues/17831")
    fun `precompiled script plugins in resources are ignored`() {
        withKotlinDslPlugin()
        withPrecompiledKotlinScript("correct.gradle.kts", "")
        file("src/main/resources/invalid.gradle.kts", "DOES NOT COMPILE")
        compileKotlin()
        val generated = file("build/generated-sources/kotlin-dsl-plugins/kotlin").walkTopDown().filter { it.isFile }.map { it.name }
        assertThat(generated.toList(), equalTo(listOf("CorrectPlugin.kt")))
    }

    @Test
    @Issue("https://github.com/gradle/gradle/issues/17831")
    fun `fails with a reasonable error message if init precompiled script has no plugin id`() {
        withKotlinDslPlugin()
        val init = withPrecompiledKotlinScript("init.gradle.kts", "")
        buildAndFail(":compileKotlin").apply {
            assertHasCause("Precompiled script '${normaliseFileSeparators(init.absolutePath)}' file name is invalid, please rename it to '<plugin-id>.init.gradle.kts'.")
        }
    }

    @Test
    @Issue("https://github.com/gradle/gradle/issues/17831")
    fun `fails with a reasonable error message if settings precompiled script has no plugin id`() {
        withKotlinDslPlugin()
        val settings = withPrecompiledKotlinScript("settings.gradle.kts", "")
        buildAndFail(":compileKotlin").apply {
            assertHasCause("Precompiled script '${normaliseFileSeparators(settings.absolutePath)}' file name is invalid, please rename it to '<plugin-id>.settings.gradle.kts'.")
        }
    }

    @Test
    @Issue("https://github.com/gradle/gradle/issues/12955")
    fun `logs output from schema collection on errors only`() {

        fun outputFrom(origin: String, logger: Boolean = true) = buildString {
            appendLine("""println("STDOUT from $origin")""")
            appendLine("""System.err.println("STDERR from $origin")""")
            if (logger) {
                appendLine("""logger.info("INFO log from $origin")""")
                appendLine("""logger.lifecycle("LIFECYCLE log from $origin")""")
                appendLine("""logger.warn("WARN log from $origin")""")
                appendLine("""logger.error("ERROR log from $origin")""")
            }
        }

        withDefaultSettingsIn("external-plugins")
        withKotlinDslPluginIn("external-plugins").appendText("""group = "test"""")
        withFile("external-plugins/src/main/kotlin/applied-output.gradle.kts", outputFrom("applied-output plugin"))
        withFile("external-plugins/src/main/kotlin/applied-output-fails.gradle.kts", """
            ${outputFrom("applied-output-fails plugin")}
            TODO("applied-output-fails plugin application failure")
        """)

        withDefaultSettings().appendText("""includeBuild("external-plugins")""")
        withKotlinDslPlugin().appendText("""dependencies { implementation("test:external-plugins") }""")
        withPrecompiledKotlinScript("some.gradle.kts", """
            plugins {
                ${outputFrom("plugins block", logger = false)}
                id("applied-output")
            }
        """)
        build(":compileKotlin").apply {
            assertNotOutput("STDOUT")
            assertNotOutput("STDERR")
            assertNotOutput("INFO")
            assertNotOutput("LIFECYCLE")
            assertNotOutput("WARN")
            assertHasErrorOutput("ERROR")
        }

        withPrecompiledKotlinScript("some.gradle.kts", """
            plugins {
                ${outputFrom("plugins block", logger = false)}
                id("applied-output")
                TODO("BOOM") // Fail in the plugins block
            }
        """)
        buildAndFail(":compileKotlin").apply {
            assertHasFailure("Execution failed for task ':generatePrecompiledScriptPluginAccessors'.") {
                assertHasCause("Failed to collect plugin requests of 'src/main/kotlin/some.gradle.kts'")
                assertHasCause("An operation is not implemented: BOOM")
            }
            assertHasErrorOutput("STDOUT from plugins block")
            assertHasErrorOutput("STDERR from plugins block")
            assertNotOutput("STDOUT from applied plugin")
            assertNotOutput("STDERR from applied plugin")
        }

        withPrecompiledKotlinScript("some.gradle.kts", """
            plugins {
                ${outputFrom("plugins block", logger = false)}
                id("applied-output-fails")
            }
        """)
        buildAndFail(":compileKotlin").apply {
            assertHasFailure("Execution failed for task ':generatePrecompiledScriptPluginAccessors'.") {
                assertHasCause("Failed to generate type-safe Gradle model accessors for the following precompiled script plugins")
                assertHasCause("An operation is not implemented: applied-output-fails plugin application failure")
            }
            assertHasErrorOutput("src/main/kotlin/some.gradle.kts")
            assertHasErrorOutput("STDOUT from applied-output-fails plugin")
            assertHasErrorOutput("STDERR from applied-output-fails plugin")
            assertNotOutput("STDOUT from plugins block")
            assertNotOutput("STDERR from plugins block")
            assertNotOutput("INFO")
            assertNotOutput("LIFECYCLE")
            assertNotOutput("WARN")
            assertHasErrorOutput("ERROR")
        }
    }
}


abstract class MyPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("action", MyTask::class.java)
    }
}


abstract class MyTask : DefaultTask() {
    @TaskAction
    fun action() {
        this::class.java.classLoader
            .getResource("my-plugin-version.txt")!!
            .readText()
            .let { version ->
                println("Applied plugin $version")
            }
    }
}
