/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.timoa.lombok;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ConvertToLogAnnotationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ConvertToLogAnnotation())
          .parser(JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .classpath("slf4j-api"));
    }

    @DocumentExample
    @Test
    void replaceSlf4j() {
        rewriteRun(// language=java
          java(
            """
              class A {
                  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(A.class);
              }
              """,
            """
              import lombok.extern.slf4j.Slf4j;

              @Slf4j
              class A {
              }
              """
          )
        );
    }

    @Test
    void replaceSlf4jImportedType() {
        rewriteRun(// language=java
          java(
            """
              import org.slf4j.Logger;
              class A {
                  private static final Logger log = org.slf4j.LoggerFactory.getLogger(A.class);
              }
              """,
            """
              import lombok.extern.slf4j.Slf4j;

              @Slf4j
              class A {
              }
              """
          )
        );
    }

    @Test
    void replaceSlf4jImportedLogger() {
        rewriteRun(// language=java
          java(
            """
              import org.slf4j.LoggerFactory;
              class A {
                  private static final org.slf4j.Logger log = LoggerFactory.getLogger(A.class);
              }
              """,
            """
              import lombok.extern.slf4j.Slf4j;

              @Slf4j
              class A {
              }
              """
          )
        );
    }

    @Test
    void replaceSlf4jStaticallyImportedLogger() {
        rewriteRun(// language=java
          java(
            """
              import static org.slf4j.LoggerFactory.*;
              class A {
                  private static final org.slf4j.Logger log = getLogger(A.class);
              }
              """,
            """
              import lombok.extern.slf4j.Slf4j;

              @Slf4j
              class A {
              }
              """
          )
        );
    }

    @Test
    void shouldNotReplaceWhenNotCalledLog() {
        rewriteRun(// language=java
          java(
            """
              class A {
                  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(A.class);
              }
              """
          )
        );
    }

    @Test
    void replaceSlf4jWithPackage() {
        rewriteRun(// language=java
          java(
            """
              package com.yourorg.yourapp;
              class A {
                  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(A.class);
              }
              """,
            """
              package com.yourorg.yourapp;

              import lombok.extern.slf4j.Slf4j;

              @Slf4j
              class A {
              }
              """
          )
        );
    }

}