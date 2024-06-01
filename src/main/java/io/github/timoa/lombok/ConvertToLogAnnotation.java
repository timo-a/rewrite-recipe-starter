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

import com.google.common.collect.Sets;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
                + "assumptions• the explicit tfield must be called `log`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new LogVisitor();
    }

    enum Logger {SLF4J}

    @Value
    class Result {

        java.util.UUID uuid;

        Logger type;

        Result(J.VariableDeclarations vars, Logger type) {
            uuid = vars.getId();
            this.type = type;
        }
    }

    class LogVisitor extends JavaIsoVisitor<ExecutionContext> {
            public static final String FOUND_LOGGER = "FOUND_LOGGER";

            private final JavaTemplate slf4jTemplate = JavaTemplate
                    .builder("@Slf4j\n")
                    .javaParser(JavaParser.fromJavaVersion()
                            .classpath("lombok"))
                    .imports("lombok.extern.slf4j.Slf4j")
                    .build();

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {

                getCursor().putMessage(FOUND_LOGGER, new HashSet<Result>());

                J.ClassDeclaration visitClassDeclaration = super.visitClassDeclaration(classDecl, ctx);

                Set<Result> loggers = getCursor().pollMessage(FOUND_LOGGER);

                if (loggers.isEmpty()) {
                    return classDecl;
                }

                //assuming there is only one finding
                Result result = loggers.iterator().next();

                if (Logger.SLF4J.equals(result.type)) {
                    maybeAddImport("lombok.extern.slf4j.Slf4j");
                    maybeRemoveImport("org.slf4j.Logger");
                    maybeRemoveImport("org.slf4j.LoggerFactory");

                    J.ClassDeclaration annotatedClass = slf4jTemplate.apply(
                            updateCursor(visitClassDeclaration),
                            visitClassDeclaration.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));

                    return annotatedClass;
                }
                return visitClassDeclaration;
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations method, ExecutionContext ctx) {

                //there must be exactly one Logger per line
                //declaring two or more in one line is possible, but I don't care to support that
                if (method.getVariables().size() != 1)
                    return method;


                HashSet<J.Modifier.Type> requiredModifiers = Sets.newHashSet(
                        J.Modifier.Type.Private,
                        J.Modifier.Type.Static,
                        J.Modifier.Type.Final
                );
                if(!requiredModifiers.equals(
                        method.getModifiers().stream().map(J.Modifier::getType).collect(Collectors.toSet()))) {
                    return method;
                }

                Logger type;
                switch (method.getTypeAsFullyQualified().getFullyQualifiedName()) {
                    case "org.slf4j.Logger":
                        type = Logger.SLF4J;
                        break;
                    default:
                        return method;
                }

                J.VariableDeclarations.NamedVariable var = method.getVariables().get(0);

                //name needs to match the name of the field that lombok creates todo write name normalization recipe
                if (!"log".equals(var.getSimpleName()))
                    return method;
//todo match expression
                /*assert method.getMethodType() != null;
                if (method.getMethodType().getName().equals("<constructor>") //it's a constructor
                        && method.getParameters().get(0) instanceof J.Empty  //no parameters
                        && method.getBody().getStatements().isEmpty()        //does nothing
                ) {
                    getCursor().putMessageOnFirstEnclosing(J.ClassDeclaration.class, FOUND_LOGGER, method);
                    return method;
                }*/

                Set<Result> loggers = getCursor().getNearestMessage(FOUND_LOGGER);
                loggers.add(new Result(method, type));

                return null;
            }
    }

}
