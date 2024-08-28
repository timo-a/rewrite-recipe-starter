= ${displayName}

*${recipe.coordinates}*

---
${description}

---


== Recipe Source

${recipe.url}[GitHub], ${recipe.issue}[Issue Tracker], ${recipe.sonartype}[Maven Central]

* groupId: ${recipe.groupId}
* artifactId: ${recipe.artifactId}
* version: ${recipe.version}

== Usage

[tabs]
====
Maven POM::
+
1. Add the following to your pom.xml file:
+
.pom.xml
[source,xml]
----
<project>
  <build>
    <plugins>
      <plugin>
        <groupId>org.openrewrite.maven</groupId>
        <artifactId>rewrite-maven-plugin</artifactId>
        <version>${mavenPluginVersion}</version>
        <configuration>
          <activeRecipes>
            <recipe>${recipe.coordinates}</recipe>
          </activeRecipes>
        </configuration>
        <dependencies>
          <dependency>
            <groupId>${recipe.groupId}</groupId>
            <artifactId>${recipe.artifactId}</artifactId>
            <version>${recipe.version}</version>
          </dependency>
        </dependencies>
      </plugin>
    </plugins>
  </build>
</project>
----
2. Run `+mvn rewrite:run+` to run the recipe.

Maven Command Line::
+
You will need to have https://maven.apache.org/download.cgi[Maven] installed on your machine before you can run the following command.
+
.Shell
[source,bash]
--
mvn -U org.openrewrite.maven:rewrite-maven-plugin:run -Drewrite.recipeArtifactCoordinates=${recipe.groupId}:${recipe.artifactId}:RELEASE -Drewrite.activeRecipes=${recipe.coordinates}
--

Gradle::
+
1. Add the following to your `build.gradle` file:
+
.build.gradle
[source,kotlin]
--
plugins {
    id("org.openrewrite.rewrite") version("${gradlePluginVersion}")
}

rewrite {
    activeRecipe("${recipe.coordinates}")
}

repositories {
    mavenCentral()
}

dependencies {
    rewrite("${recipe.groupId}:${recipe.artifactId}:${recipe.version}")
}
--
2. Run `gradle rewriteRun` to run the recipe.

Gradle init script::
+
1. Create a file named `init.gradle` in the root of your project.
+
.init.gradle
[source,groovy]
--
initscript {
    repositories {
        maven { url "https://plugins.gradle.org/m2" }
    }
    dependencies { classpath("org.openrewrite:plugin:${gradlePluginVersion}") }
}
rootProject {
    plugins.apply(org.openrewrite.gradle.RewritePlugin)
    dependencies {
        rewrite("${recipe.groupId}:${recipe.artifactId}:${recipe.version}")
    }
    rewrite {
        activeRecipe("${recipe.coordinates}")
    }
    afterEvaluate {
        if (repositories.isEmpty()) {
            repositories {
                mavenCentral()
            }
        }
    }
}
--
2. Run `gradle --init-script init.gradle rewriteRun` to run the recipe.
====


<#if recipe.subrecipeList??>

== Definition

[tabs]
====
Recipe List::
    <#list recipe.subrecipeList as sub>
        - ${sub}
    </#list>
Yaml Recipe List::
+
[source,yaml]
--
${recipe.yamlString}
--
====
<#else>
</#if>