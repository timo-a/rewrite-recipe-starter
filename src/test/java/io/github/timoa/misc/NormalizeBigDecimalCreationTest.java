package io.github.timoa.misc;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class NormalizeBigDecimalCreationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NormalizeBigDecimalCreation())
          .parser(JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .classpath("guava"));
    }

    @DocumentExample
    @Test
    void exampleValues() {
        rewriteRun(
          spec -> spec
            .parser(JavaParser.fromJavaVersion()
              .logCompilationWarningsAndErrors(false)),
          // language=java
          java(
            """
              import java.math.BigDecimal;
              
              class Test {
                  var constructor_1_00 = new BigDecimal(1.00);
                  var valueOf_1_00     = BigDecimal.valueOf(1.00);
                  var constructor_1d   = new BigDecimal(1d);
              
              }
              """,
            """
              import java.math.BigDecimal;
              
              class Test {
                  var constructor_1_00 = new BigDecimal("1.00");
                  var valueOf_1_00     = new BigDecimal("1.00");
                  var constructor_1d   = new BigDecimal("1");
              
              }
              """
          )
        );
    }

    @Test
    void replaceConstructor() {
        rewriteRun(
          spec -> spec
            .parser(JavaParser.fromJavaVersion()
              .logCompilationWarningsAndErrors(false)),
          // language=java
          java(
            """
              import java.math.BigDecimal;
              
              class Test {
                  BigDecimal bd1 = new BigDecimal(1.00);
                  BigDecimal bd2 = new BigDecimal(1d);
              
                  void a(){
                      BigDecimal bd3 = new BigDecimal(1.00);
                      BigDecimal bd4 = new BigDecimal(1d);
                  }
              }
              """,
            """
              import java.math.BigDecimal;
              
              class Test {
                  BigDecimal bd1 = new BigDecimal("1.00");
                  BigDecimal bd2 = new BigDecimal("1");
              
                  void a(){
                      BigDecimal bd3 = new BigDecimal("1.00");
                      BigDecimal bd4 = new BigDecimal("1");
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceValueOf() {
        rewriteRun(
          spec -> spec
            .parser(JavaParser.fromJavaVersion()
              .logCompilationWarningsAndErrors(false)),
          // language=java
          java(
            """
              import java.math.BigDecimal;
              
              class Test {
                  BigDecimal bd1 = BigDecimal.valueOf(1.00);
                  BigDecimal bd2 = BigDecimal.valueOf(1d);
              
                  void a(){
                      BigDecimal bd3 = BigDecimal.valueOf(1.00);
                      BigDecimal bd4 = BigDecimal.valueOf(1d);
                  }
              }
              """,
            """
              import java.math.BigDecimal;
              
              class Test {
                  BigDecimal bd1 = new BigDecimal("1.00");
                  BigDecimal bd2 = new BigDecimal("1");
              
                  void a(){
                      BigDecimal bd3 = new BigDecimal("1.00");
                      BigDecimal bd4 = new BigDecimal("1");
                  }
              }
              """
          )
        );
    }







}
