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

class ConvertNoArgsConstructorTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ConvertNoArgsConstructor())
          .parser(JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true));
    }

    @DocumentExample
    @Test
    void replaceEmptyPublicConstructor() {
        rewriteRun(// language=java
          java(
            """
              class A {
                  public A() {}
              }
              """,
            """
              import lombok.NoArgsConstructor;
              
              @NoArgsConstructor()
              class A {
              }
              """
          )
        );
    }

    @Test
    void keepNonEmptyPublicConstructor() {
        rewriteRun(
          java(
            """
              class A {
              
                  int foo;
              
                  public A() {
                      foo = 7;
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceEmptyProtectedConstructor() {
        rewriteRun(
          java(
            """
              class A {
                  protected A() {}
              }
              """,
            """
              import lombok.NoArgsConstructor;
              
              @NoArgsConstructor(access = AccessLevel.PROTECTED)
              class A {
              }
              """
          )
        );
    }

    @Test
    void replaceEmptyPrivateConstructor() {
        rewriteRun(
          java(
            """
              class A {
                  private A() {}
              }
              """,
            """
              import lombok.NoArgsConstructor;
              
              @NoArgsConstructor(access = AccessLevel.PRIVATE)
              class A {
              }
              """
          )
        );
    }

    @Test
    void replaceEmptyPackageConstructor() {
        rewriteRun(
          java(
            """
              class A {
                  A() {}
              }
              """,
            """
              import lombok.NoArgsConstructor;
              
              @NoArgsConstructor(access = AccessLevel.PACKAGE)
              class A {
              }
              """
          )
        );
    }

}
