# Miscellaneous Rewrite Recipes

[![ci](https://github.com/timo-a/rewrite-recipe-starter/actions/workflows/ci.yml/badge.svg)](https://github.com/timo-a/rewrite-recipe-starter/actions/workflows/ci.yml)
[![Apache 2.0](https://img.shields.io/github/license/openrewrite/rewrite-migrate-java.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.timo-a/rewrite-recipe-starter.svg)](https://mvnrepository.com/artifact/io.github.timo-a/rewrite-recipe-starter)

This repository collects my custom recipes, primarily ones that add lombok annotations to projects that started without it.  
This repository is still young and may e.g. be rebased, the [releases on maven central](https://mvnrepository.com/artifact/io.github.timo-a/rewrite-recipe-starter) however are immutable.

## Getting started

In order to run any of these recipes on your project you need to use either maven or gradle as a build tool.
The [OpenRewrite Docs](https://docs.openrewrite.org/running-recipes) are a great start.

* [META-INF/rewrite/stringutils.yml](./src/main/resources/META-INF/rewrite/stringutils.yml) - A declarative YAML recipe that replaces usages of `org.springframework.util.StringUtils` with `org.apache.commons.lang3.StringUtils`.
  * [UseApacheStringUtilsTest](./src/test/java/com/yourorg/UseApacheStringUtilsTest.java) - A test class for the `io.github.timo-a.UseApacheStringUtils` recipe.
* [NoGuavaListsNewArrayList.java](./src/main/java/com/yourorg/NoGuavaListsNewArrayList.java) - An imperative Java recipe that replaces usages of `com.google.common.collect.Lists` with `new ArrayList<>()`.
  * [NoGuavaListsNewArrayListTest.java](./src/test/java/com/yourorg/NoGuavaListsNewArrayListTest.java) - A test class for the `NoGuavaListsNewArrayList` recipe.
* [SimplifyTernary](./src/main/java/com/yourorg/SimplifyTernary.java) - An Refaster style recipe that simplifies ternary expressions.
  * [SimplifyTernaryTest](./src/test/java/com/yourorg/SimplifyTernaryTest.java) - A test class for the `SimplifyTernary` recipe.
* [AssertEqualsToAssertThat](./src/main/java/com/yourorg/AssertEqualsToAssertThat.java) - An imperative Java recipe that replaces JUnit's `assertEquals` with AssertJ's `assertThat`, to show how to handle classpath dependencies.
  * [AssertEqualsToAssertThatTest](./src/test/java/com/yourorg/AssertEqualsToAssertThatTest.java) - A test class for the `AssertEqualsToAssertThat` recipe.
* [AppendToReleaseNotes](./src/main/java/com/yourorg/AppendToReleaseNotes.java) - A ScanningRecipe that appends a message to the release notes of a project.
  * [AppendToReleaseNotesTest](./src/test/java/com/yourorg/AppendToReleaseNotesTest.java) - A test class for the `AppendToReleaseNotes` recipe.
* [ClassHierarchy](./src/main/java/com/yourorg/ClassHierarchy.java) - A recipe that demonstrates how to produce a data table on the class hierarchy of a project.
  * [ClassHierarchyTest](./src/test/java/com/yourorg/ClassHierarchyTest.java) - A test class for the `ClassHierarchy` recipe.
* [UpdateConcoursePipeline](./src/main/java/com/yourorg/UpdateConcoursePipeline.java) - A recipe that demonstrates how to update a Concourse pipeline, as an example of operating on Yaml files.
  * [UpdateConcoursePipelineTest](./src/test/java/com/yourorg/UpdateConcoursePipelineTest.java) - A test class for the `UpdateConcoursePipeline` recipe.

## Local Publishing for Testing

Before you publish your recipe module to an artifact repository, you may want to try it out locally.
To do this on the command line, using `gradle`, run:

```bash
./gradlew publishToMavenLocal
# or ./gradlew pTML
# or mvn install
```

To publish using maven, run:

```bash
./mvnw install
```

This will publish to your local maven repository, typically under `~/.m2/repository`.

Replace the groupId, artifactId, recipe name, and version in the below snippets with the ones that correspond to your recipe.

In the pom.xml of a different project you wish to test your recipe out in, make your recipe module a plugin dependency of rewrite-maven-plugin:

### Quickstart
 Copy what you need from here:
```xml
<project>
    <build>
        <plugins>
            <plugin>
                <groupId>org.openrewrite.maven</groupId>
                <artifactId>rewrite-maven-plugin</artifactId>
                <version>RELEASE</version>
                <configuration>
                    <activeRecipes>
                        <recipe>io.github.timoa.lombok.ConvertNoArgsConstructor</recipe>
                    </activeRecipes>
                </configuration>
                <dependencies>
                    <dependency>
                        <groupId>io.github.timo-a</groupId>
                        <artifactId>rewrite-misc-recipes</artifactId>
                        <version>0.0.6</version><!-- see above for the latest version -->
                    </dependency>
                </dependencies>
            </plugin>
        </plugins>
    </build>
</project>
```


---
Todo: clean up the following parts of the original Readme.
  
## Publishing to Artifact Repositories

This project is configured to publish to Moderne's open artifact repository (via the `publishing` task at the bottom of
the `build.gradle.kts` file). If you want to publish elsewhere, you'll want to update that task.
[app.moderne.io](https://app.moderne.io) can draw recipes from the provided repository, as well as from [Maven Central](https://search.maven.org).

Note:
Running the publish task _will not_ update [app.moderne.io](https://app.moderne.io), as only Moderne employees can
add new recipes. If you want to add your recipe to [app.moderne.io](https://app.moderne.io), please ask the
team in [Slack](https://join.slack.com/t/rewriteoss/shared_invite/zt-nj42n3ea-b~62rIHzb3Vo0E1APKCXEA) or in [Discord](https://discord.gg/xk3ZKrhWAb).

These other docs might also be useful for you depending on where you want to publish the recipe:

* Sonatype's instructions for [publishing to Maven Central](https://maven.apache.org/repository/guide-central-repository-upload.html)
* Gradle's instructions on the [Gradle Publishing Plugin](https://docs.gradle.org/current/userguide/publishing\_maven.html).

### From Github Actions

The `.github` directory contains a Github action that will push a snapshot on every successful build.

Run the release action to publish a release version of a recipe.

### From the command line

To build a snapshot, run `./gradlew snapshot publish` to build a snapshot and publish it to Moderne's open artifact repository for inclusion at [app.moderne.io](https://app.moderne.io).

To build a release, run `./gradlew final publish` to tag a release and publish it to Moderne's open artifact repository for inclusion at [app.moderne.io](https://app.moderne.io).

## Applying OpenRewrite recipe development best practices

We maintain a collection of [best practices for writing OpenRewrite recipes](https://docs.openrewrite.org/recipes/recipes/openrewritebestpractices).
You can apply these recommendations to your recipes by running the following command:

```bash
./gradlew rewriteRun -Drewrite.activeRecipe=org.openrewrite.recipes.OpenRewriteBestPractices
```
or
```bash
./mvnw -U org.openrewrite.maven:rewrite-maven-plugin:run -Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-recommendations:RELEASE -Drewrite.activeRecipes=org.openrewrite.recipes.OpenRewriteBestPractices
```