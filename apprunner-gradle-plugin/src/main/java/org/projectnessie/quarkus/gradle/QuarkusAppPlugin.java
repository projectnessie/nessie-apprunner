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

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class QuarkusAppPlugin implements Plugin<Project> {

  static final String EXTENSION_NAME = "nessieQuarkusApp";

  /** The configuration that contains the Quarkus server application as the only dependency. */
  static final String APP_CONFIG_NAME = "nessieQuarkusServer";

  @Override
  public void apply(Project project) {
    project
        .getConfigurations()
        .register(
            APP_CONFIG_NAME,
            c ->
                c.setTransitive(false)
                    .setDescription(
                        "References the Nessie-Quarkus server dependency, only a single dependency allowed."));

    project.getExtensions().create(EXTENSION_NAME, QuarkusAppExtension.class, project);
  }
}
