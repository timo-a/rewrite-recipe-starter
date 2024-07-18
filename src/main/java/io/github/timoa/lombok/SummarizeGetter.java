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

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.TreeVisitingPrinter;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.*;

import static java.util.Comparator.comparing;
import static org.openrewrite.java.tree.JavaType.Variable;

@Value
@EqualsAndHashCode(callSuper = false)
public class SummarizeGetter extends Recipe {

    @Override
    public String getDisplayName() {
        //language=markdown
        return "Convert getter methods to annotations";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return new StringJoiner("\n")
                .add("Convert trivial getter methods to `@Getter` annotations on their respective fields.")
                .add("")
                .add("Limitations:")
                .add("")
                .add(" - Does not add a dependency to Lombok, users need to do that manually")
                .add(" - Ignores fields that are declared on the same line as others, e.g. `private int foo, bar;" +
                        "Users who have such fields are advised to separate them beforehand with " +
                        "[org.openrewrite.staticanalysis.MultipleVariableDeclaration]" +
                        "(https://docs.openrewrite.org/recipes/staticanalysis/multiplevariabledeclarations).")
                .add(" - Does not offer any of the configuration keys listed in https://projectlombok.org/features/GetterSetter.")
                .toString();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new Summarizer();
    }


    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class Summarizer extends JavaIsoVisitor<ExecutionContext> {
        private static final String ALL_FIELDS_DECORATED_ACC = "ALL_FIELDS_DECORATED_ACC";

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

            //initialize variable to store if all encountered fields have getters
            getCursor().putMessage(ALL_FIELDS_DECORATED_ACC, true);
            getCursor().putMessage(ALL_FIELDS_DECORATED_ACC + "2", 4);

            //delete methods, note down corresponding fields
            J.ClassDeclaration classDeclAfterVisit = super.visitClassDeclaration(classDecl, ctx);

            System.out.println(11111);
            int allFieldsAnnotated2 = getCursor().pollNearestMessage(ALL_FIELDS_DECORATED_ACC + "2");
            boolean allFieldsAnnotated = getCursor().pollNearestMessage(ALL_FIELDS_DECORATED_ACC);
            System.out.println("all fields annotated: " + allFieldsAnnotated);
            System.out.println("something changed: " + (classDeclAfterVisit != classDecl));

            //only thing that can have changed is removal of getter methods
            if (allFieldsAnnotated //
                    && classDeclAfterVisit != classDecl //need to have encountered at least one getter annotation
            ) {
                TreeVisitingPrinter.printTree(classDecl);
                TreeVisitingPrinter.printTree(classDeclAfterVisit);
                System.out.println("adding annotation");
                //Add annotation
                JavaTemplate template = JavaTemplate.builder("@Getter\n")
                            .imports("lombok.Getter")
                            .javaParser(JavaParser.fromJavaVersion()
                                    .classpath("lombok"))
                        .build();

                J.ClassDeclaration annotatedClass = template.apply(
                        updateCursor(classDeclAfterVisit),
                        classDeclAfterVisit.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));

                return annotatedClass;
            }
            return classDecl;
        }

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations variableDecls, ExecutionContext ctx){
            System.out.println();
            System.out.println("visiting VD " + variableDecls.getVariables().get(0).getSimpleName());
            boolean allFieldsAnnotatedSoFar = getCursor().getNearestMessage(ALL_FIELDS_DECORATED_ACC);
            if (!allFieldsAnnotatedSoFar) {
                System.out.println("not all fields annotated so far");
                return variableDecls;
            }

            Optional<J.Annotation> oa = variableDecls.getLeadingAnnotations().stream()
                    .filter(a -> "Getter".equals(a.getSimpleName()))
                    .findFirst();

            boolean hasGetterAnnotation = oa.isPresent();
            if (hasGetterAnnotation) {
                System.out.println("has getter annotation");
                List<J.Annotation> leadingAnnotations = variableDecls.getLeadingAnnotations();
                System.out.println("leading annotations has " + leadingAnnotations.size() + " annotations");
                List<J.Annotation> filteredLeadingAnnotations = new ArrayList<>(leadingAnnotations);
                filteredLeadingAnnotations.remove(oa.get());
                System.out.println("leading annotations has " + leadingAnnotations.size() + " annotations");
                System.out.println("copy has " + filteredLeadingAnnotations.size() + " annotations");
                J.VariableDeclarations a = variableDecls.withLeadingAnnotations(filteredLeadingAnnotations);
                System.out.println("a has " + a.getLeadingAnnotations().size() + " annotations");
                return a;
            } else {
                getCursor().putMessage(ALL_FIELDS_DECORATED_ACC, false);
            }
            return variableDecls;
        }
    }
}
