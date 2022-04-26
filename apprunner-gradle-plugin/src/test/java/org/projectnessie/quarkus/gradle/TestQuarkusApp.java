/*
 * Copyright (C) 2020 Dremio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.projectnessie.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test for the {@link QuarkusAppPlugin}, which basically simulates what the {@code build.gradle} in
 * Apache Iceberg does.
 */
class TestQuarkusApp {
  @TempDir Path testProjectDir;

  Path buildFile;

  String nessieVersionForTest;

  List<String> prefix;

  @BeforeEach
  void setup() throws Exception {
    buildFile = testProjectDir.resolve("build.gradle");
    Path localBuildCacheDirectory = testProjectDir.resolve(".local-cache");

    // Copy our test class in the test's project test-source folder
    Path testTargetDir = testProjectDir.resolve("src/test/java/org/projectnessie/quarkus/gradle");
    Files.createDirectories(testTargetDir);
    Files.copy(
        Paths.get(
            "src/test/resources/org/projectnessie/quarkus/gradle/TestSimulatingTestUsingThePlugin.java"),
        testTargetDir.resolve("TestSimulatingTestUsingThePlugin.java"));

    Files.write(
        testProjectDir.resolve("settings.gradle"),
        Arrays.asList(
            "buildCache {",
            "    local {",
            "        directory '" + localBuildCacheDirectory.toUri() + "'",
            "    }",
            "}",
            "",
            "include 'sub'"));

    // Versions injected from build.gradle
    nessieVersionForTest = System.getProperty("nessie-version-for-test", "0.21.2");
    String junitVersion = System.getProperty("junit-version");
    String jacksonVersion = System.getProperty("jackson-version");

    assertThat(junitVersion != null && jacksonVersion != null)
        .withFailMessage(
            "System property required for this test is missing, run this test via Gradle or set the system properties manually")
        .isTrue();

    prefix =
        Arrays.asList(
            "plugins {",
            "    id 'java'",
            "    id 'org.projectnessie'",
            "}",
            "",
            "repositories {",
            "    mavenLocal()",
            "    mavenCentral()",
            "}",
            "",
            "test {",
            "    useJUnitPlatform()",
            "}",
            "",
            "dependencies {",
            "    testImplementation 'org.junit.jupiter:junit-jupiter-api:" + junitVersion + "'",
            "    testImplementation 'org.junit.jupiter:junit-jupiter-engine:" + junitVersion + "'",
            "    testImplementation 'com.fasterxml.jackson.core:jackson-databind:"
                + jacksonVersion
                + "'",
            "    testImplementation 'org.projectnessie:nessie-client:"
                + nessieVersionForTest
                + "'");
  }

  /**
   * Ensure that the plugin fails when there is no dependency specified for the {@code
   * nessieQuarkusServer} configuration.
   */
  @Test
  void noAppConfigDeps() throws Exception {
    Files.write(
        buildFile,
        Stream.concat(
                prefix.stream(),
                Stream.of(
                    "}", "", "nessieQuarkusApp {", "    includeTask(tasks.named(\"test\"))", "}"))
            .collect(Collectors.toList()));

    BuildResult result = createGradleRunner("test").buildAndFail();
    assertThat(result.task(":test"))
        .isNotNull()
        .extracting(BuildTask::getOutcome)
        .isEqualTo(TaskOutcome.FAILED);
    assertThat(Arrays.asList(result.getOutput().split("\n")))
        .contains(
            "> Dependency org.projectnessie:nessie-quarkus:runner missing in configuration nessieQuarkusServer");
  }

  /**
   * Ensure that the plugin fails when there is more than one dependency specified for the {@code
   * nessieQuarkusServer} configuration.
   */
  @Test
  void tooManyAppConfigDeps() throws Exception {
    Files.write(
        buildFile,
        Stream.concat(
                prefix.stream(),
                Stream.of(
                    "    nessieQuarkusServer 'org.projectnessie:nessie-quarkus:"
                        + nessieVersionForTest
                        + ":runner'",
                    "    nessieQuarkusServer 'org.projectnessie:nessie-model:"
                        + nessieVersionForTest
                        + "'",
                    "}",
                    "",
                    "nessieQuarkusApp.includeTask(tasks.named(\"test\"))"))
            .collect(Collectors.toList()));

    BuildResult result = createGradleRunner("test").buildAndFail();
    assertThat(result.task(":test"))
        .isNotNull()
        .extracting(BuildTask::getOutcome)
        .isEqualTo(TaskOutcome.FAILED);
    assertThat(Arrays.asList(result.getOutput().split("\n")))
        .contains(
            "> Configuration nessieQuarkusServer must only contain the org.projectnessie:nessie-quarkus:runner dependency, "
                + "but resolves to these artifacts: "
                + "org.projectnessie:nessie-quarkus:"
                + nessieVersionForTest
                + ", "
                + "org.projectnessie:nessie-model:"
                + nessieVersionForTest);
  }

  /**
   * Ensure that the plugin fails when both the config-dependency and the exec-jar are specified.
   */
  @Test
  void configAndExecJar() throws Exception {
    Files.write(
        buildFile,
        Stream.concat(
                prefix.stream(),
                Stream.of(
                    "    nessieQuarkusServer 'org.projectnessie:nessie-quarkus:"
                        + nessieVersionForTest
                        + ":runner'",
                    "}",
                    "",
                    "nessieQuarkusApp {",
                    "    executableJar.set(jar.archiveFile.get())",
                    "    includeTask(tasks.named(\"test\"))",
                    "}"))
            .collect(Collectors.toList()));

    assertThat(createGradleRunner("jar").build().task(":jar"))
        .extracting(BuildTask::getOutcome)
        .isNotEqualTo(TaskOutcome.FAILED);

    BuildResult result = createGradleRunner("test").buildAndFail();
    assertThat(result.task(":test"))
        .isNotNull()
        .extracting(BuildTask::getOutcome)
        .isEqualTo(TaskOutcome.FAILED);
    assertThat(Arrays.asList(result.getOutput().split("\n")))
        .contains(
            "> Configuration nessieQuarkusServer contains a dependency and option 'executableJar' are mutually exclusive");
  }

  /** Ensure that the plugin fails when it doesn't find a matching Java. */
  @Test
  void unknownJdk() throws Exception {
    Files.write(
        buildFile,
        Stream.concat(
                prefix.stream(),
                Stream.of(
                    "    nessieQuarkusServer 'org.projectnessie:nessie-quarkus:"
                        + nessieVersionForTest
                        + ":runner'",
                    "}",
                    "",
                    "nessieQuarkusApp {",
                    "    javaVersion.set(42)",
                    "    includeTask(tasks.named(\"test\"))",
                    "}"))
            .collect(Collectors.toList()));

    BuildResult result = createGradleRunner("test").buildAndFail();
    assertThat(result.task(":test"))
        .isNotNull()
        .extracting(BuildTask::getOutcome)
        .isEqualTo(TaskOutcome.FAILED);
    assertThat(Arrays.asList(result.getOutput().split("\n")))
        .contains("> " + ProcessState.noJavaMessage(42));
  }

  /**
   * Starting the Nessie-Server via the Nessie-Quarkus-Gradle-Plugin must work fine, even if a
   * different nessie-client version is being used (despite whether having conflicting versions
   * makes any sense).
   */
  @Test
  void conflictingDependenciesNessie() throws Exception {
    Files.write(
        buildFile,
        Stream.concat(
                prefix.stream(),
                Stream.of(
                    "    implementation 'org.projectnessie:nessie-client:0.4.0'",
                    "    nessieQuarkusServer 'org.projectnessie:nessie-quarkus:"
                        + nessieVersionForTest
                        + ":runner'",
                    "}",
                    "",
                    "nessieQuarkusApp {",
                    "    includeTask(tasks.named(\"test\"))",
                    "}"))
            .collect(Collectors.toList()));

    BuildResult result = createGradleRunner("test").build();
    assertThat(result.task(":test"))
        .isNotNull()
        .extracting(BuildTask::getOutcome)
        .isEqualTo(TaskOutcome.SUCCESS);

    assertThat(Arrays.asList(result.getOutput().split("\n")))
        .anyMatch(l -> l.contains("Listening on: http://0.0.0.0:"))
        .contains("Quarkus application stopped.");

    // 2nd run must be up-to-date

    result = createGradleRunner("test").build();
    assertThat(result.task(":test"))
        .isNotNull()
        .extracting(BuildTask::getOutcome)
        .isEqualTo(TaskOutcome.UP_TO_DATE);

    // 3rd run after a 'clean' must use the cached result

    result = createGradleRunner("clean").build();
    assertThat(result.task(":clean"))
        .isNotNull()
        .extracting(BuildTask::getOutcome)
        .isEqualTo(TaskOutcome.SUCCESS);

    result = createGradleRunner("test").build();
    assertThat(result.task(":test"))
        .isNotNull()
        .extracting(BuildTask::getOutcome)
        .isEqualTo(TaskOutcome.FROM_CACHE);
  }

  @Test
  void genericTask() throws Exception {
    Files.write(
        buildFile,
        Stream.concat(
                prefix.stream(),
                Stream.of(
                    "    implementation 'org.projectnessie:nessie-client:0.4.0'",
                    "    nessieQuarkusServer 'org.projectnessie:nessie-quarkus:"
                        + nessieVersionForTest
                        + ":runner'",
                    "}",
                    "",
                    "tasks.register('foobar') {",
                    "  doFirst {",
                    "    System.out.println(\"FOO BAR ${ext[\"quarkus.http.test-port\"]} BAZ\")",
                    "  }",
                    "}",
                    "",
                    "nessieQuarkusApp {",
                    "    includeTask(tasks.named(\"foobar\"))",
                    "}"))
            .collect(Collectors.toList()));

    // "test" task must fail (Nessie-Quarkus not started)
    BuildResult result = createGradleRunner("test").buildAndFail();
    assertThat(result)
        .satisfies(r -> assertThat(r.getOutput()).doesNotContain("powered by Quarkus"))
        .extracting(r -> r.task(":test"))
        .isNotNull()
        .extracting(BuildTask::getOutcome)
        .isEqualTo(TaskOutcome.FAILED);

    // "foobar" task must succeed and extra-property yield the port number
    result = createGradleRunner("foobar").build();
    assertThat(result)
        .satisfies(
            r ->
                assertThat(r.getOutput())
                    // Nessie-Quarkus must have been started
                    .contains("powered by Quarkus")
                    .satisfies(
                        s ->
                            // verify that there's a port number > 0 (printed by the 'foobar' task)
                            assertThat(
                                    Pattern.compile(".*FOO BAR (\\d+) BAZ.*", Pattern.DOTALL)
                                        .matcher(s))
                                .satisfies(m -> assertThat(m.matches()).isTrue())
                                .satisfies(
                                    m ->
                                        assertThat(Integer.parseInt(m.group(1))).isGreaterThan(0))))
        .extracting(r -> r.task(":foobar"))
        .isNotNull()
        .extracting(BuildTask::getOutcome)
        .isEqualTo(TaskOutcome.SUCCESS);
  }

  private GradleRunner createGradleRunner(String task) {
    return GradleRunner.create()
        .withPluginClasspath()
        .withProjectDir(testProjectDir.toFile())
        .withArguments("--build-cache", "--info", "--stacktrace", task)
        .withDebug(true)
        .forwardOutput();
  }
}
