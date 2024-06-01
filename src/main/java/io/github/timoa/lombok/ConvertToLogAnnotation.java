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

import java.util.HashSet;
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

    enum Logger {SLF4J, LOG4J2}

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
        public static final String CLASS_NAME = "CLASS_NAME";

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {

            getCursor().putMessage(FOUND_LOGGER, new HashSet<Result>());
            getCursor().putMessage(CLASS_NAME, classDecl.getSimpleName());

            J.ClassDeclaration visitClassDeclaration = super.visitClassDeclaration(classDecl, ctx);

            Set<Result> loggers = getCursor().pollMessage(FOUND_LOGGER);

            if (loggers.isEmpty()) {
                return classDecl;
            }

                //assuming there is only one finding
            Result result = loggers.iterator().next();

            switch (result.type) {
                case SLF4J:
                    maybeAddImport("lombok.extern.slf4j.Slf4j");
                    maybeRemoveImport("org.slf4j.Logger");
                    maybeRemoveImport("org.slf4j.LoggerFactory");
                case LOG4J2:
                    maybeAddImport("lombok.extern.log4j.Log4j2");
                    maybeRemoveImport("org.apache.logging.log4j.Logger");
                    maybeRemoveImport("org.apache.logging.log4j.LogManager");
                default:
                    J.ClassDeclaration annotatedClass = getLombokTemplate(result.type).apply(
                            updateCursor(visitClassDeclaration),
                            visitClassDeclaration.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
                    return annotatedClass;
                }
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
                    case "org.apache.logging.log4j.Logger":
                        type = Logger.LOG4J2;
                        break;
                    default:
                        return method;
                }

                J.VariableDeclarations.NamedVariable var = method.getVariables().get(0);

                //name needs to match the name of the field that lombok creates todo write name normalization recipe
                if (!"log".equals(var.getSimpleName()))
                    return method;

                J.MethodInvocation methodCall = (J.MethodInvocation) var.getInitializer();

                String leftSide = methodCall.getMethodType().getDeclaringType().getFullyQualifiedName() + "." +  methodCall.getMethodType().getName();

                //method call must match
                if (
                        type == Logger.SLF4J && !"org.slf4j.LoggerFactory.getLogger".equals(leftSide)
                                ||
                        type == Logger.LOG4J2 && !"org.apache.logging.log4j.LogManager.getLogger".equals(leftSide)
                ) {
                    return method;
                }

                //argument must match
                String className = getCursor().pollNearestMessage(CLASS_NAME);
                if (methodCall.getArguments().size() != 1 || !methodCall.getArguments().get(0).toString().equals(className + ".class")) {
                    return method;
                }

                Set<Result> loggers = getCursor().getNearestMessage(FOUND_LOGGER);
                loggers.add(new Result(method, type));

                return null;
            }
    }

    private JavaTemplate getLombokTemplate(Logger type) {
        switch (type) {
            case SLF4J:
                return getLombokTemplate("Slf4j", "lombok.extern.slf4j.Slf4j");
            case LOG4J2:
                return getLombokTemplate("Log4j2", "lombok.extern.log4j.Log4j2");
            default:
                throw new IllegalArgumentException("Unsupported log type: " + type);
        }
    }

    JavaTemplate getLombokTemplate(String name, String import_) {
        return JavaTemplate
                .builder("@"+name+"\n")
                .javaParser(JavaParser.fromJavaVersion()
                        .classpath("lombok"))
                .imports(import_)
                .build();
    }

}
