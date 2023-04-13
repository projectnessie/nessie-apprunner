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
package org.projectnessie.nessierunner.gradle;

import static org.projectnessie.nessierunner.gradle.NessieRunnerPlugin.APP_CONFIG_NAME;

import java.util.stream.Collectors;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskInputs;

/** Configures the task for which the Nessie-Quarkus process shall be started. */
public class NessieRunnerTaskConfigurer<T extends Task> implements Action<T> {

  private final Action<T> postStartAction;
  private final Provider<NessieRunnerService> nessieRunnerServiceProvider;

  public NessieRunnerTaskConfigurer(
      Action<T> postStartAction, Provider<NessieRunnerService> nessieRunnerServiceProvider) {
    this.postStartAction = postStartAction;
    this.nessieRunnerServiceProvider = nessieRunnerServiceProvider;
  }

  @SuppressWarnings(
      "Convert2Lambda") // Gradle complains when using lambdas (build-cache won't wonk)
  @Override
  public void execute(T task) {
    Project project = task.getProject();

    Configuration appConfig = project.getConfigurations().getByName(APP_CONFIG_NAME);
    NessieRunnerExtension extension =
        project.getExtensions().getByType(NessieRunnerExtension.class);

    // Add the StartTask's properties as "inputs" to the Test task, so the Test task is
    // executed, when those properties change.
    TaskInputs inputs = task.getInputs();
    inputs.properties(extension.getEnvironment().get());
    inputs.properties(extension.getSystemProperties().get());
    inputs.property("nessie.quarkus.arguments", extension.getArguments().get().toString());
    inputs.property("nessie.quarkus.jvmArguments", extension.getJvmArguments().get().toString());
    RegularFile execJar = extension.getExecutableJar().getOrNull();
    if (execJar != null) {
      inputs.file(execJar).withPathSensitivity(PathSensitivity.RELATIVE);
    }
    inputs.property("nessie.quarkus.javaVersion", extension.getJavaVersion().get());

    inputs.files(appConfig);

    DependencySet dependencies = appConfig.getDependencies();
    // Although we assert that only a single artifact is used (later), collect all dependencies
    // for a nicer error message.
    String dependenciesString =
        dependencies.stream()
            .map(d -> String.format("%s:%s:%s", d.getGroup(), d.getName(), d.getVersion()))
            .collect(Collectors.joining(", "));
    FileCollection files =
        !dependencies.isEmpty()
            ? appConfig.fileCollection(dependencies.toArray(new Dependency[0]))
            : null;

    // Start the Nessie-Quarkus-App only when the Test task actually runs

    task.usesService(nessieRunnerServiceProvider);
    task.doFirst(
        new Action<Task>() {
          @SuppressWarnings("unchecked")
          @Override
          public void execute(Task t) {
            ProcessState processState = new ProcessState();
            nessieRunnerServiceProvider.get().register(processState, t);

            processState.quarkusStart(t, extension, files, dependenciesString);

            if (postStartAction != null) {
              postStartAction.execute((T) t);
            }
          }
        });
    task.doLast(
        new Action<Task>() {
          @Override
          public void execute(Task t) {
            nessieRunnerServiceProvider.get().finished(t);
          }
        });
  }
}
