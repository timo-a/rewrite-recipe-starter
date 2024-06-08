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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
@Value
@EqualsAndHashCode(callSuper = false)
public class ConvertToLogAnnotation extends Recipe {


    @Override
    public String getDisplayName() {
        //language=markdown
        return "Use `@Slf4` instead of defining the field yourself";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "Prefer the lombok annotation `@Slf4` over explicitly written out Logger fields.\n"
                + "assumptions: the explicit field must be called `log`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new LogVisitor();
    }

    class LogVisitor extends JavaIsoVisitor<ExecutionContext> {
        public static final String CLASS_NAME = "CLASS_NAME";

        private JavaTemplate javaTemplate = JavaTemplate
                .builder("@Slf4j\n")
                .javaParser(JavaParser.fromJavaVersion()
                        .classpath("lombok"))
                .imports("lombok.extern.slf4j.Slf4j")
                .build();


        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {

            getCursor().putMessage(CLASS_NAME, classDecl.getSimpleName());

            J.ClassDeclaration visitClassDeclaration = super.visitClassDeclaration(classDecl, ctx);

            if (visitClassDeclaration != classDecl) {
                //something changed -> field was removed
                maybeAddImport("lombok.extern.slf4j.Slf4j");
                maybeRemoveImport("org.slf4j.Logger");
                maybeRemoveImport("org.slf4j.LoggerFactory");
                J.ClassDeclaration annotatedClass = javaTemplate.apply(
                        updateCursor(visitClassDeclaration),
                        visitClassDeclaration.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
                return annotatedClass;
            } else {
                return classDecl;
            }
        }

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {

            //there must be exactly one Logger per line
            //declaring two or more in one line is possible, but I don't care to support that
            if (multiVariable.getVariables().size() != 1)
                return multiVariable;

            J.VariableDeclarations.NamedVariable namedVariable = multiVariable.getVariables().get(0);

            //declaration needs to be private static final
            JavaType.Variable variableType = namedVariable.getVariableType();
            if (!variableType.hasFlags(Flag.Private, Flag.Static, Flag.Final)) {
                return multiVariable;
            }

            String path = multiVariable.getTypeAsFullyQualified().getFullyQualifiedName();
            if (!"org.slf4j.Logger".equals(path)) {
                return multiVariable;
            }

            //name needs to match the name of the field that lombok creates
            if (!"log".equals(namedVariable.getSimpleName()))
                return multiVariable;

            J.MethodInvocation methodCall = (J.MethodInvocation) namedVariable.getInitializer();

            JavaType.Method methodType = methodCall.getMethodType();
            String leftSide = methodType.getDeclaringType().getFullyQualifiedName() + "." +  methodType.getName();

            //method call must match
            if (!"org.slf4j.LoggerFactory.getLogger".equals(leftSide)) {
                return multiVariable;
            }

            //String className = variableType.getOwner().toString();

            //argument must match
            String className = getCursor().pollNearestMessage(CLASS_NAME);
            if (methodCall.getArguments().size() != 1
                    || !methodCall.getArguments().get(0).toString().equals(className + ".class")) {
                return multiVariable;
            }

            //delete
            return null;
        }
    }
}
