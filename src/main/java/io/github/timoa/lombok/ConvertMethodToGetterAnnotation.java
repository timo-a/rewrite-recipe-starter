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
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;

import static java.util.Comparator.comparing;

@Value
@EqualsAndHashCode(callSuper = false)
public class ConvertMethodToGetterAnnotation extends Recipe {

    @Override
    public String getDisplayName() {
        //language=markdown
        return "Convert method to annotation";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "convert any method to an annotation.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MethodRemover();
    }

    /**
     * This visitor removes all methods.
     * If there were any methods, this visitor triggers another visitor to annotate all fields.
     * (The fields cannot be annotated by this visitor because I cannot visit field and method declarations separately.)
     */
    private static class MethodRemover extends JavaIsoVisitor<ExecutionContext> {
        private static final String DECORATE_FIELDS = "DECORATE_FIELDS";

        //this flag indicates if all fields should be annotated
        private boolean decorateFields;

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {

            getCursor().putMessage(DECORATE_FIELDS, false);

            //delete methods, record if there were any
            J.ClassDeclaration classDeclAfterVisit = super.visitClassDeclaration(classDecl, ctx);

            //see if any methods where deleted
            decorateFields = getCursor().pollMessage(DECORATE_FIELDS);

            //no lombok (neither annotation nor import) in result without this one
            maybeAddImport("lombok.Getter");//todo find out why this has to be both here and in the other visitor
            return new FieldAnnotator().visitClassDeclaration(classDeclAfterVisit, ctx);
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            //pass on that there is a method
            getCursor().putMessageOnFirstEnclosing(J.ClassDeclaration.class, DECORATE_FIELDS, true);

            //delete
            return null;
        }

    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    static class FieldAnnotator extends JavaIsoVisitor<ExecutionContext>{

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {

            JavaTemplate annotation = JavaTemplate.builder("@Getter\n")
                    .javaParser(JavaParser.fromJavaVersion()
                            .classpath("lombok"))
                    .imports("lombok.Getter")
                    .build();

            J.VariableDeclarations annotated = annotation.apply(
                    getCursor(),
                    multiVariable.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
            //no import in result without this one
            maybeAddImport("lombok.Getter");//todo find out why this has to be both here and in the other visitor
            return annotated;
        }
    }
}
