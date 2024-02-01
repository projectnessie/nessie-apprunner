# Nessie Apprunner plugins for Maven and Gradle

[![Build Status](https://github.com/projectnessie/nessie-apprunner/actions/workflows/ci.yml/badge.svg)](https://github.com/projectnessie/nessie-apprunner/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/org.projectnessie.nessie-runner/nessie-runner-maven-plugin)](https://search.maven.org/artifact/org.projectnessie.nessie-runner/nessie-runner-maven-plugin)

See https://github.com/projectnessie/nessie

See https://projectnessie.org

## Usage

The _Nessie Apprunner plugins_ are available for [Maven](#maven) and [Gradle](#Gradle). A
rudimentary integration test is shown below.

### Java integration tests

```java
import org.projectnessie.client.api.NessieApiV2;
import org.projectnessie.client.http.HttpClientBuilder;
import org.projectnessie.model.Branch;

public class ITWorksWithNessie {
  static URI NESSIE_SERVER_URI =
      String.format(
          "%s/api/v2",
          requireNonNull(
              System.getProperty("quarkus.http.test-url"),
              "Required system property quarkus.http.test-url is not set"));

  static NessieApiV2 nessieApi;

  @BeforeAll
  static void setupNessieClient() {
    nessieApi = HttpClientBuilder.builder()
        .fromSystemProperties()
        .withUri(NESSIE_SERVER_URI)
        .build(NessieApiV2.class);
  }
  
  @AfterAll
  static void closeNessieClient() {
    nessieApiV1.close();
  }
  
  @Test
  public void pingNessie() {
    Branch defaultBranch = nessieApiV1.getDefaultBranch();
    // do some more stuff
  }
}
```

### Gradle

#### Kotlin DSL

`build.gradle.kts`
```kotlin
plugins {
  java
  id("org.projectnessie") version "0.29.0"
}

dependencies {
  // your dependencies

  // specify the GAV of the Nessie Quarkus server runnable (uber-jar)
  nessieQuarkusServer("org.projectnessie:nessie-quarkus:0.49.0:runner")
}

nessieQuarkusApp {
  // Ensure that the `test` task has a Nessie Server available.  
  includeTask(tasks.named<Test>("test"))
  // Note: prefer setting up separate `integrationTest` tasks, see https://docs.gradle.org/current/userguide/jvm_test_suite_plugin.html

  // These system properties will be available in your integration tests and will contain the
  // port and full URL of the Nessie Quarkus server's HTTP server.
  // httpListenPortProperty.set("quarkus.http.port") // quarkus.http.port is the default
  // httpListenUrlProperty.set("quarkus.http.test-url") // quarkus.http.test-url is the default
}
```

#### Groovy DSL

`build.gradle`
```groovy
plugins {
  id 'java'
  id 'org.projectnessie' version "0.29.0"
}

dependencies {
  // your dependencies

  // specify the GAV of the Nessie Quarkus server runnable (uber-jar)
  nessieQuarkusServer "org.projectnessie:nessie-quarkus:0.49.0:runner"
}

nessieQuarkusApp {
  // Ensure that the `test` task has a Nessie Server available.  
  includeTask(tasks.named("test"))
  // Note: prefer setting up separate `integrationTest` tasks, see https://docs.gradle.org/current/userguide/jvm_test_suite_plugin.html

  // These system properties will be available in your integration tests and will contain the
  // port and full URL of the Nessie Quarkus server's HTTP server.
  // httpListenPortProperty.set("quarkus.http.port") // quarkus.http.port is the default
  // httpListenUrlProperty.set("quarkus.http.test-url") // quarkus.http.test-url is the default
}
```

### Maven

The `org.projectnessie.nessie-runner:nessie-runner-maven-plugin` Maven plugin can be used together with the
standard `maven-failsafe-plugin`

(POM snippet)

```xml
<project>

  <properties>
    <nessie.version>0.49.0</nessie.version>
  </properties>
  
  <dependencies>
    <dependency>
      <groupId>org.projectnessie</groupId>
      <artifactId>nessie-client</artifactId>
      <version>${nessie.version}</version>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-failsafe-plugin</artifactId>
      </plugin>

      <plugin>
        <groupId>org.projectnessie.nessie-runner</groupId>
        <artifactId>nessie-runner-maven-plugin</artifactId>
        <version>0.29.0</version>
        <configuration>
          <!-- Preferred way, specify the GAV of the Nessie Quarkus server runnable (uber-jar) -->
          <appArtifactId>org.projectnessie:nessie-quarkus:jar:runner:${nessie.version}</appArtifactId>
          <!-- The system properties passed to the Nessie server -->
          <systemProperties>
            <foo>bar</foo>
          </systemProperties>
          <!-- The environment variables passed to the Nessie server -->
          <environment>
            <HELLO>world</HELLO>
          </environment>

          <!-- These system properties will be available in your integration tests and will contain the
               port and full URL of the Nessie Quarkus server's HTTP server. -->
          <!-- quarkus.http.port is the default -->
          <httpListenPortProperty>quarkus.http.port</httpListenPortProperty>
          <!-- quarkus.http.test-url is the default -->
          <httpListenUrlProperty>quarkus.http.test-url</httpListenUrlProperty>
        </configuration>
        <executions>
          <execution>
            <!-- Start the Nessie Server before the integration tests start -->
            <id>start</id>
            <phase>pre-integration-test</phase>
            <goals><goal>start</goal></goals>
          </execution>
          <execution>
            <!-- Stop the Nessie Server after the integration tests finished -->
            <id>stop</id>
            <phase>post-integration-test</phase>
            <goals><goal>stop</goal></goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

## Build requirements

* Java 11

## Build instructions

```basb
./mvnw clean install
(cd gradle-plugin ; ./gradlew build)
```
