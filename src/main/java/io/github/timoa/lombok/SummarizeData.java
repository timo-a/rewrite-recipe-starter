/*
 * Copyright 2024 the original author or authors.
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

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;

@Value
@EqualsAndHashCode(callSuper = false)
public class SummarizeData extends Recipe {
    @Override
    public String getDisplayName() {
        return "Summarize class annotations into @Data";
    }

    @Override
    public String getDescription() {
        return "Summarize class annotations into @Data.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new SummarizeData.Summarizer();
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class Summarizer extends JavaIsoVisitor<ExecutionContext> {

        Set<String> needed = Stream
                .of(
                        "ToString",
                        "EqualsAndHashCode",
                        "Getter",
                        "Setter",
                        "RequiredArgsConstructor")
                .collect(Collectors.toSet());

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {

            J.ClassDeclaration visited = super.visitClassDeclaration(classDecl, ctx);
            int annotationsRemoved = classDecl.getLeadingAnnotations().size() - visited.getLeadingAnnotations().size();

            if (visited != classDecl &&
                    //since duplicate annotations are not allowed, we expect exactly as many annotations removed as needed
                    annotationsRemoved == needed.size() ) {

                maybeRemoveImport("lombok.ToString");
                maybeRemoveImport("lombok.EqualsAndHashCode");
                maybeRemoveImport("lombok.Getter");
                maybeRemoveImport("lombok.Setter");
                maybeRemoveImport("lombok.RequiredArgsConstructor");
                maybeAddImport("lombok.Data");


                JavaTemplate template = JavaTemplate.builder("@Data\n")
                        .imports("lombok.Data")
                        .javaParser(JavaParser.fromJavaVersion().classpath("lombok"))
                        .build();

                return template.apply(
                        updateCursor(visited),
                        visited.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));

            }

            return classDecl;
        }

        @Override
        public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
            return needed.contains(annotation.getSimpleName())
                    && annotation.getArguments() == null //no arguments of any kind. Too strict?
                    //should only trigger on class annotation
                    && getCursor().getParent().getValue() instanceof J.ClassDeclaration
                    ? null
                    : annotation;
        }

    }
}
