package io.github.timoa;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.OrderImports;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.Arrays;

import static java.util.Collections.*;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.Assertions.java;

class JacksonImportStyleTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new OrderImports(false));
    }

    @DocumentExample
    @Test
    //this test does not use my defined style but creates one on the fly
    void inlineStyle() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(
            singletonList(
              new NamedStyles(
                randomId(),
                "custom",
                "custom style",
                null,
                emptySet(),
                singletonList(
                  ImportLayoutStyle.builder()
                    .importAllOthers()
                    .blankLine()
                    .importPackage("com.fasterxml.jackson.core.*")
                    .importPackage("tools.jackson.*")
                    .blankLine()
                    .importStaticAllOthers()
                    .build()
                )
              )
            ))),
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
    //This tests seeks to use the style I defined. not sure if it is done correctly
    void myStyle() {
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
        //This tests seeks to use the style I defined. not sure if it is done correctly
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
}
