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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.TreeVisitingPrinter;
import org.openrewrite.java.tree.J;

import java.util.*;

import static java.util.Comparator.comparing;

@Value
@EqualsAndHashCode(callSuper = false)
public class AnnotataionRemoveRecipe extends Recipe {

    @Override
    public String getDisplayName() {
        //language=markdown
        return "Remove Annotation";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "remove annotation";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new AnnotationRemover();
    }


    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class AnnotationRemover extends JavaIsoVisitor<ExecutionContext> {

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

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {

            //delete methods, note down corresponding fields
            J.ClassDeclaration classDeclAfterVisit = super.visitClassDeclaration(classDecl, ctx);

            return classDeclAfterVisit;
        }

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations variableDecls, ExecutionContext ctx){

            Optional<J.Annotation> oa = variableDecls.getLeadingAnnotations().stream()
                    .filter(a -> "Getter".equals(a.getSimpleName()))
                    .findFirst();

            boolean hasGetterAnnotation = oa.isPresent();
            if (hasGetterAnnotation) {
                List<J.Annotation> leadingAnnotations = variableDecls.getLeadingAnnotations();
                List<J.Annotation> filteredLeadingAnnotations = new ArrayList<>(leadingAnnotations);
                filteredLeadingAnnotations.remove(oa.get());
                J.VariableDeclarations a = variableDecls.withLeadingAnnotations(filteredLeadingAnnotations);
                return a;
            }
            return variableDecls;
        }
    }
}
