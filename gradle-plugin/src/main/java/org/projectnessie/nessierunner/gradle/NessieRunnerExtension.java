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

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.TaskProvider;

public class NessieRunnerExtension {
  private final MapProperty<String, String> environment;
  private final MapProperty<String, String> environmentNonInput;
  private final MapProperty<String, String> systemProperties;
  private final MapProperty<String, String> systemPropertiesNonInput;
  private final ListProperty<String> arguments;
  private final ListProperty<String> argumentsNonInput;
  private final ListProperty<String> jvmArguments;
  private final ListProperty<String> jvmArgumentsNonInput;
  private final Property<Integer> javaVersion;
  private final Property<String> httpListenPortProperty;
  private final Property<String> httpListenUrlProperty;
  private final Property<String> managementListenPortProperty;
  private final Property<String> managementListenUrlProperty;
  private final RegularFileProperty executableJar;
  private final RegularFileProperty workingDirectory;
  private final Property<Long> timeToListenUrlMillis;
  private final Property<Long> timeToStopMillis;

  private final Provider<NessieRunnerService> nessieRunnerServiceProvider;

  public NessieRunnerExtension(
      Project project, Provider<NessieRunnerService> nessieRunnerServiceProvider) {
    this.nessieRunnerServiceProvider = nessieRunnerServiceProvider;

    environment = project.getObjects().mapProperty(String.class, String.class);
    environmentNonInput = project.getObjects().mapProperty(String.class, String.class);
    systemProperties = project.getObjects().mapProperty(String.class, String.class);
    systemPropertiesNonInput = project.getObjects().mapProperty(String.class, String.class);
    arguments = project.getObjects().listProperty(String.class);
    argumentsNonInput = project.getObjects().listProperty(String.class);
    jvmArguments = project.getObjects().listProperty(String.class);
    jvmArgumentsNonInput = project.getObjects().listProperty(String.class);
    javaVersion = project.getObjects().property(Integer.class).convention(11);
    httpListenUrlProperty =
        project.getObjects().property(String.class).convention("quarkus.http.test-url");
    httpListenPortProperty =
        project.getObjects().property(String.class).convention("quarkus.http.test-port");
    managementListenUrlProperty =
        project.getObjects().property(String.class).convention("quarkus.management.test-url");
    managementListenPortProperty =
        project.getObjects().property(String.class).convention("quarkus.management.test-port");
    workingDirectory =
        project
            .getObjects()
            .fileProperty()
            .convention(project.getLayout().getBuildDirectory().file("nessie-quarkus"));
    executableJar = project.getObjects().fileProperty();
    timeToListenUrlMillis = project.getObjects().property(Long.class).convention(0L);
    timeToStopMillis = project.getObjects().property(Long.class).convention(0L);
  }

  public MapProperty<String, String> getSystemProperties() {
    return systemProperties;
  }

  public MapProperty<String, String> getSystemPropertiesNonInput() {
    return systemPropertiesNonInput;
  }

  public MapProperty<String, String> getEnvironment() {
    return environment;
  }

  public MapProperty<String, String> getEnvironmentNonInput() {
    return environmentNonInput;
  }

  public ListProperty<String> getArguments() {
    return arguments;
  }

  public ListProperty<String> getArgumentsNonInput() {
    return argumentsNonInput;
  }

  public ListProperty<String> getJvmArguments() {
    return jvmArguments;
  }

  public ListProperty<String> getJvmArgumentsNonInput() {
    return jvmArgumentsNonInput;
  }

  public Property<Integer> getJavaVersion() {
    return javaVersion;
  }

  public Property<String> getHttpListenPortProperty() {
    return httpListenPortProperty;
  }

  public Property<String> getHttpListenUrlProperty() {
    return httpListenUrlProperty;
  }

  public Property<String> getManagementListenPortProperty() {
    return managementListenPortProperty;
  }

  public Property<String> getManagementListenUrlProperty() {
    return managementListenUrlProperty;
  }

  public RegularFileProperty getExecutableJar() {
    return executableJar;
  }

  public RegularFileProperty getWorkingDirectory() {
    return workingDirectory;
  }

  public Property<Long> getTimeToListenUrlMillis() {
    return timeToListenUrlMillis;
  }

  public Property<Long> getTimeToStopMillis() {
    return timeToStopMillis;
  }

  public NessieRunnerExtension includeTasks(TaskCollection<? extends Task> taskCollection) {
    return includeTasks(taskCollection, null);
  }

  public <T extends Task> NessieRunnerExtension includeTasks(
      TaskCollection<T> taskCollection, Action<T> postStartAction) {
    taskCollection.configureEach(
        new NessieRunnerTaskConfigurer<>(postStartAction, nessieRunnerServiceProvider));
    return this;
  }

  public NessieRunnerExtension includeTask(TaskProvider<? extends Task> taskProvider) {
    return includeTask(taskProvider, null);
  }

  public <T extends Task> NessieRunnerExtension includeTask(
      TaskProvider<T> taskProvider, Action<T> postStartAction) {
    taskProvider.configure(
        new NessieRunnerTaskConfigurer<>(postStartAction, nessieRunnerServiceProvider));
    return this;
  }
}
