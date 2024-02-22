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
import org.openrewrite.java.tree.Statement;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

@Value
@EqualsAndHashCode(callSuper = false)
public class ConvertToNoArgsConstructor extends Recipe {

    @Override
    public String getDisplayName() {
        //language=markdown
        return "Use `@NoArgsConstructor` where applicable";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "Prefer the lombok annotation `@NoArgsConstructor` over explicitly written out constructors.\n"
                + "This recipe does not create annotations for implicit constructors.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            public static final String FOUND_EMPTY_CONSTRUCTOR = "FOUND_EMPTY_CONSTRUCTOR";

            private final JavaTemplate noArgsAnnotationPublic = JavaTemplate
                    .builder("@NoArgsConstructor()\n")
                    .javaParser(JavaParser.fromJavaVersion()
                            .classpath("lombok"))
                    .imports("lombok.NoArgsConstructor")
                    .build();

            //separate template because more imports are needed
            private final JavaTemplate noArgsAnnotationParameterized = JavaTemplate
                    .builder("@NoArgsConstructor(access = AccessLevel.#{})\n")
                    .javaParser(JavaParser.fromJavaVersion()
                            .classpath("lombok"))
                    .imports("lombok.AccessLevel")
                    .imports("lombok.NoArgsConstructor")
                    .build();

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                super.visitClassDeclaration(classDecl, ctx);

                J.MethodDeclaration message = getCursor().pollMessage(FOUND_EMPTY_CONSTRUCTOR);

                //if no constructor is found return immediately
                if (message == null) {
                    return classDecl;
                }

                maybeAddImport("lombok.NoArgsConstructor");
                List<J.Modifier.Type> modifiers = message.getModifiers().stream()
                        .map(J.Modifier::getType)
                        .collect(Collectors.toList());

                J.ClassDeclaration annotatedClass;
                if (modifiers.contains(J.Modifier.Type.Public)) {
                    //for public constructors the simple annotation can be used

                    annotatedClass = noArgsAnnotationPublic.apply(
                            getCursor(),
                            classDecl.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));

                } else {
                    //for not public constructors we need to specify a parameter.
                    //TODO it is probably cleaner to specify the access level parameter in any case here and develop a
                    // separate recipe that deletes default parameters in lombok annotations

                    String accessLevel;

                    if (modifiers.contains(J.Modifier.Type.Protected)) {
                        accessLevel = "PROTECTED";
                    } else if (modifiers.contains(J.Modifier.Type.Private)) {
                        accessLevel = "PRIVATE";
                    } else {//no modifier -> package
                        accessLevel = "PACKAGE";
                    }

                    annotatedClass = noArgsAnnotationParameterized.apply(
                            getCursor(),
                            classDecl.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)),
                            accessLevel);
                }


                //remove the constructor which is a method declaration which is a statement
                List<Statement> statements = classDecl.getBody().getStatements()
                        .stream()
                        .filter(s -> s != message)//keep every statement that isn't the constructor declaration
                        .collect(Collectors.toList());
                return annotatedClass.withBody(annotatedClass.getBody().withStatements(statements));//todo is there a better way to remove the constructor?
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                assert method.getMethodType() != null;
                if (method.getMethodType().getName().equals("<constructor>") //it's a constructor
                        && method.getParameters().get(0) instanceof J.Empty  //no parameters
                        && method.getBody().getStatements().isEmpty()        //does nothing
                ) {
                    getCursor().putMessageOnFirstEnclosing(J.ClassDeclaration.class, FOUND_EMPTY_CONSTRUCTOR, method);
                    return method;
                }
                return super.visitMethodDeclaration(method, ctx);
            }

        };
    }

}
