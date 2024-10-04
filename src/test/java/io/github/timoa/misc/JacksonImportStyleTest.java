package io.github.timoa.misc;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.OrderImports;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static java.util.Collections.singletonList;
import static org.openrewrite.java.Assertions.java;

class JacksonImportStyleTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new OrderImports(false));
    }

    @DocumentExample
    @Test
    void simpleExample() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(
            singletonList(
              new JacksonImportStyle())
            )),
          java(
            """
              package com.fasterxml.jackson.core.write;
              
              import com.fasterxml.jackson.core.*;
              
              import java.io.*;
              
              import static org.junit.jupiter.api.Assertions.*;
              
              import org.junit.jupiter.api.Test;
              
              class Test {
              }
              """,
            """
              package com.fasterxml.jackson.core.write;
              
              import java.io.*;
              
              import org.junit.jupiter.api.Test;
              
              import com.fasterxml.jackson.core.*;
              
              import static org.junit.jupiter.api.Assertions.*;
              
              class Test {
              }
              """
          )
        );
    }

    @Test
    void leaveNoGaps() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(
            singletonList(
              new JacksonImportStyle())
          )),
          java(
            """
              package com.fasterxml.jackson.core.write;

              import com.fasterxml.jackson.core.*;
              
              import java.io.*;
              
              import static org.junit.jupiter.api.Assertions.*;
              
              import java.math.BigDecimal;
              import java.math.BigInteger;
              
              import org.junit.jupiter.api.Test;

              class GeneratorBasicDerivedTest {
              }
              """,
            """
              package com.fasterxml.jackson.core.write;
              
              import java.io.*;
              import java.math.BigDecimal;
              import java.math.BigInteger;
              
              import org.junit.jupiter.api.Test;
              
              import com.fasterxml.jackson.core.*;
              
              import static org.junit.jupiter.api.Assertions.*;
              
              class GeneratorBasicDerivedTest {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4165")
    @Test
    // todo reactivate once the issue above is resolved
    void shouldNotIndentExtends() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(
            singletonList(
              new JacksonImportStyle())
          )),
          java("class BaseClass {}"),
          java(
            """              
              import static org.junit.jupiter.api.Assertions.*;

              import java.io.*;
              class Test
                  extends BaseClass {
              }
              """,
            """
              import java.io.*;
              
              import static org.junit.jupiter.api.Assertions.*;
              
              class Test
                  extends BaseClass {
              }
              """
          ),
          java(//different indentation here, just to be sure
            """              
              import static org.junit.jupiter.api.Assertions.*;

              import java.io.*;
              class Test2
                    extends BaseClass {
              }
              """,
            """
              import java.io.*;
              
              import static org.junit.jupiter.api.Assertions.*;
              
              class Test2
                    extends BaseClass {
              }
              """
          )
        );
    }
}
