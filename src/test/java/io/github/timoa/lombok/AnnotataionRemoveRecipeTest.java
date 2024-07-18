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

class AnnotataionRemoveRecipeTest implements RewriteTest {

    // Note, you can define defaults for the RecipeSpec and these defaults will be used for all tests.
    // In this case, the recipe and the parser are common. See below, on how the defaults can be overridden
    // per test.
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AnnotataionRemoveRecipe())
          .parser(JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true).classpath("lombok"));
    }

    //I put spaces in the 'expected' block only to match the 'actual' but in running the test these spaces are trimmed away, so the expected block is different from what I specify!?
    @DocumentExample
    @Test
    void removeAnnotationAbove() {
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
              
              class A {
              
                  
                  int foo;
              
              }
              """
          )
        );
    }

    //works only if we expect 5 spaces, I guess the space between
    @Test
    void removeAnnotationBefore() {
        rewriteRun(// language=java
          java(
            """
              import lombok.Getter;
              
              class A {
              
                  @Getter int foo;
              
              }
              """,
            """
              import lombok.Getter;
              
              class A {
              
                   int foo;
              
              }
              """
          )
        );
    }
}
