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

// This is a test for the ConvertToNoArgsConstructor recipe, as an example of how to write a test for an imperative recipe.
class SummarizeGetterTest implements RewriteTest {

    // Note, you can define defaults for the RecipeSpec and these defaults will be used for all tests.
    // In this case, the recipe and the parser are common. See below, on how the defaults can be overridden
    // per test.
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SummarizeGetter())
          .parser(JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true).classpath("lombok"));
    }

    @DocumentExample
    @Test
    void replaceOneFieldGetter() {
        rewriteRun(// language=java
          java(
            """
              import lombok.Getter;
              
              class A {
              
                  @Getter
                  int foo;
              
              }
              """,
            """
              import lombok.Getter;
              
              @Getter
              class A {
              
                  int foo;
              
              }
              """
          )
        );
    }

    @Test
    void replaceOneFieldGetterWhenInFront() {
        rewriteRun(// language=java
          java(
            """
              import lombok.Getter;
              
              class A {
              
                  @Getter int foo = 9;
              
              }
              """,
            """
              import lombok.Getter;
              
              @Getter
              class A {
              
                  int foo = 9;
              
              }
              """
          )
        );
    }

    @Test
    void otherAnnotationAbove() {
        rewriteRun(// language=java
          java(
            """
              import lombok.Getter;
              import lombok.Setter;
              
              class A {
              
                  @Setter
                  @Getter
                  int foo;
              
              }
              """,
            """
              import lombok.Getter;
              import lombok.Setter;
              
              @Getter
              class A {
              
                  @Setter
                  int foo;
              
              }
              """
          )
        );
    }

    @Test
    void otherAnnotationBelow() {
        rewriteRun(// language=java
          java(
            """
              import lombok.Getter;
              import lombok.Setter;
              
              class A {
              
                  @Getter
                  @Setter
                  int foo;
              
              }
              """,
            """
              import lombok.Getter;
              import lombok.Setter;
              
              @Getter
              class A {
              
                  @Setter
                  int foo;
              
              }
              """
          )
        );
    }

    @Test
    void otherAnnotationsAround() {
        rewriteRun(// language=java
          java(
            """
              import lombok.Getter;
              import lombok.Setter;
              import lombok.Singular;
              
              class A {
              
                  @Singular
                  @Getter
                  @Setter
                  int foo;
              
              }
              """,
            """
              import lombok.Getter;
              import lombok.Setter;
              import lombok.Singular;
              
              @Getter
              class A {
              
                  @Singular
                  @Setter
                  int foo;
              
              }
              """
          )
        );
    }

    @Test
    void doNothingWhenNotEveryFieldIsAnnotated() {
        rewriteRun(// language=java
          java(
            """
              import lombok.Getter;
              
              class A {
              
                  @Getter
                  int foo;
              
                  int bar;
              
              }
              """
          )
        );
    }

    @Test
    void doNothingWhenAFieldHasSpecialConfig() {
        rewriteRun(// language=java
          java(
            """
              import lombok.AccessLevel;
              import lombok.Getter;
              
              class A {
              
                  @Getter
                  int foo;
              
                  @Getter(AccessLevel.PACKAGE)
                  int bar;
              
              }
              """
          )
        );
    }

    @Test
    void manyFields() {
        rewriteRun(// language=java
          java(
            """
              import lombok.AccessLevel;
              import lombok.Getter;
              
              class A {
              
                  @Getter
                  int foo;
              
                  @Getter
                  int bar;
              
                  @Getter
                  int foobar;
              
                  @Getter
                  int barfoo;
              
              }
              """,
            """
              import lombok.AccessLevel;
              import lombok.Getter;
              
              @Getter
              class A {
              
                  int foo;
              
                  int bar;
              
                  int foobar;
              
                  int barfoo;
              
              }
              """
          )
        );
    }



}
