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
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

// This is a test for the ConvertToNoArgsConstructor recipe, as an example of how to write a test for an imperative recipe.
class RightnameGetterTest implements RewriteTest {

    // Note, you can define defaults for the RecipeSpec and these defaults will be used for all tests.
    // In this case, the recipe and the parser are common. See below, on how the defaults can be overridden
    // per test.
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RightnameGetter())
          .parser(JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true));
    }

    @DocumentExample
    @Test
    void applyDirectly() {//TODO remove again
        rewriteRun(
          spec -> spec
            .recipe(new ChangeMethodName("com.yourorg.whatever.A giveFoo()", "getFoo", null, null))
            .parser(JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true)),
          // language=java
          java(
            """
              package com.yourorg.whatever;
              class A {
                  int foo = 9;
                  int giveFoo() { return foo; }
              }
              """,
            """
              package com.yourorg.whatever;
              class A {
                  int foo = 9;
                  int getFoo() { return foo; }
              }
              """
          )
        );
    }

    @Test
    void renameInSingleClass() {
        rewriteRun(// language=java
          java(
            """
              package com.yourorg.whatever;
              class A {
                  int foo = 9;
                  int giveFoo() { return foo; }
              }
              """,
            """
              package com.yourorg.whatever;
              class A {
                  int foo = 9;
                  int getFoo() { return foo; }
              }
              """
          )
        );
    }

    @Test
    void renamePrimitiveBooleanInSingleClass() {
        rewriteRun(// language=java
          java(
            """
              package com.yourorg.whatever;
              class A {
                  boolean foo;
                  boolean giveFoo() { return foo; }
              }
              """,
            """
              package com.yourorg.whatever;
              class A {
                  boolean foo;
                  boolean isFoo() { return foo; }
              }
              """
          )
        );
    }

    @Test
    void renamePrimitiveBooleanWithPrefixInSingleClass() {
        rewriteRun(// language=java
          java(
            """
              package com.yourorg.whatever;
              class A {
                  boolean isFoo;
                  boolean giveFoo() { return isFoo; }
              }
              """,
            """
              package com.yourorg.whatever;
              class A {
                  boolean isFoo;
                  boolean isFoo() { return isFoo; }
              }
              """
          )
        );
    }

    @Test
    void renameClassBooleanInSingleClass() {
        rewriteRun(// language=java
          java(
            """
              package com.yourorg.whatever;
              class A {
                  Boolean Foo;
                  Boolean giveFoo() { return Foo; }
              }
              """,
            """
              package com.yourorg.whatever;
              class A {
                  Boolean Foo;
                  Boolean getFoo() { return Foo; }
              }
              """
          )
        );
    }

    @Test
    void renameClassBooleanWithPrefixInSingleClass() {
        rewriteRun(// language=java
          java(
            """
              package com.yourorg.whatever;
              class A {
                  Boolean isFoo;
                  Boolean giveFoo() { return isFoo; }
              }
              """,
            """
              package com.yourorg.whatever;
              class A {
                  Boolean isFoo;
                  Boolean getIsFoo() { return isFoo; }
              }
              """
          )
        );
    }

    @Test
    void renameAcrossClasses() {
        rewriteRun(// language=java
          java(
            """
              package com.yourorg.whatever;
              class A {
                  int foo = 9;
                  int giveFoo() { return foo; }
              }
              """,
            """
              package com.yourorg.whatever;
              class A {
                  int foo = 9;
                  int getFoo() { return foo; }
              }
              """
          ),
          java(
            """
              package com.yourorg.whatever;
              class B {
                  void useIt() {
                      var a = new A();
                      a.giveFoo();
                  }
              }
              """,
            """
              package com.yourorg.whatever;
              class B {
                  void useIt() {
                      var a = new A();
                      a.getFoo();
                  }
              }
              """
          )
        );
    }

}
