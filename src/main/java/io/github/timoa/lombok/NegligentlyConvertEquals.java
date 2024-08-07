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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.*;
import org.openrewrite.java.tree.J;

import static java.util.Comparator.comparing;

@Value
@EqualsAndHashCode(callSuper = false)
public class NegligentlyConvertEquals extends Recipe {

    @Override
    public String getDisplayName() {
        //language=markdown
        return "Replace any custom `equals` or `hashCode` methods with the `EqualsAndHashCode` annotation";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "This recipe substitutes a class level `@EqualsAndHashCode` annotation for a custom `equals` or `hashCode` methods. " +
                "If both are defined, then both will be replaced. If only one is defined then it will be replaced. " +
                "This recipe does not check if the custom `equals` or `hashCode` methods behave like ones generated by the lombok annotation. " +
                "Doing so is considered infeasible at this time. " +
                "As a compromise this recipe finds and replaces the custom methods and relies on the user to review the changes closely. " +
                "As a consequence this recipe is VERY DANGEROUS to include into a composite recipe! " +
                "Users are advised to run it only in isolation.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new Converter();
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class Converter extends JavaIsoVisitor<ExecutionContext> {

        // This method override is only here to show how to print the AST for debugging purposes.
        // You can remove this method if you don't need it.
        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            // This is a useful debugging tool if you're ever unsure what the visitor is visiting
            String printed = TreeVisitingPrinter.printTree(cu);
            System.out.printf(printed);
            // You must always delegate to the super method to ensure the visitor continues to visit deeper
            return super.visitCompilationUnit(cu, ctx);
        }

        MethodMatcher equalsMatcher = new MethodMatcher("* equals(Object)");
        MethodMatcher hashCodeMatcher = new MethodMatcher("* hashCode()");

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {

            J.ClassDeclaration classDeclAfterVisit = super.visitClassDeclaration(classDecl, ctx);

            //only thing that can have changed is removal of either equals or hash code
            //and something needs to have changed before we add an annotation at class level
            if (classDeclAfterVisit != classDecl) {
                maybeAddImport("lombok.EqualsAndHashCode");

                //Add annotation
                JavaTemplate template = JavaTemplate.builder("@EqualsAndHashCode\n")
                        .imports("lombok.EqualsAndHashCode")
                        .javaParser(JavaParser.fromJavaVersion().classpath("lombok"))
                        .build();

                return template.apply(
                        updateCursor(classDeclAfterVisit),
                        classDeclAfterVisit.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
            }
            return classDecl;
        }


        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
            J.ClassDeclaration classDecl = getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class);

            // The enclosing class of a J.MethodDeclaration must be known for a MethodMatcher to match it
            if (equalsMatcher.matches(method, classDecl) || hashCodeMatcher.matches(method, classDecl)) {
                return null;
            } else {
                return method;
            }
        }
    }
}
