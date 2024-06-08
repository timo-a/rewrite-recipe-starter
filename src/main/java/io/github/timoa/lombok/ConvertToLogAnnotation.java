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

import java.util.HashSet;
import java.util.Map;
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
                + "assumptions: the explicit field must be called `log`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new LogVisitor();
    }

    enum Logger {SLF4J, LOG4J2, LOG, JBOSS, COMMONS}

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
                case LOG:
                    maybeAddImport("lombok.extern.java.Log");
                    maybeRemoveImport("java.util.logging.Logger");
                case JBOSS:
                    maybeAddImport("lombok.extern.jbosslog.JBossLog");
                    maybeRemoveImport("org.jboss.logging.Logger");
                case COMMONS:
                    maybeAddImport("lombok.extern.apachecommons.CommonsLog");
                    maybeRemoveImport("org.apache.commons.logging.Log");
                    maybeRemoveImport("org.apache.commons.logging.LogFactory");
                default:
                    J.ClassDeclaration annotatedClass = getLombokTemplate(result.type).apply(
                            updateCursor(visitClassDeclaration),
                            visitClassDeclaration.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
                    return annotatedClass;
                }
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {

                //there must be exactly one Logger per line
                //declaring two or more in one line is possible, but I don't care to support that
                if (multiVariable.getVariables().size() != 1)
                    return multiVariable;


                JavaType.FullyQualified type = multiVariable.getTypeAsFullyQualified();
                if (!type.hasFlags(Flag.Private, Flag.Static, Flag.Final)) {
                    return multiVariable;
                }

                Logger loggerType;
                Map<String, Logger> typeMap = ImmutableMap.<String, Logger> builder()
                        .put("org.slf4j.Logger", Logger.SLF4J)
                        .put("org.apache.logging.log4j.Logger", Logger.LOG4J2)
                        .put("java.util.logging.Logger", Logger.LOG)
                        .put("org.jboss.logging.Logger", Logger.JBOSS)
                        .put("org.apache.commons.logging.Log", Logger.COMMONS)
                        .build();

                String path = type.getFullyQualifiedName();
                if (typeMap.containsKey(path))
                    loggerType = typeMap.get(path);
                else
                    return multiVariable;

                J.VariableDeclarations.NamedVariable var = multiVariable.getVariables().get(0);

                //name needs to match the name of the field that lombok creates todo write name normalization recipe
                if (!"log".equals(var.getSimpleName()))
                    return multiVariable;

                J.MethodInvocation methodCall = (J.MethodInvocation) var.getInitializer();

                String leftSide = methodCall.getMethodType().getDeclaringType().getFullyQualifiedName() + "." +  methodCall.getMethodType().getName();

                //method call must match
                if (
                        loggerType == Logger.SLF4J && !"org.slf4j.LoggerFactory.getLogger".equals(leftSide)
                                ||
                        loggerType == Logger.LOG4J2 && !"org.apache.logging.log4j.LogManager.getLogger".equals(leftSide)
                                ||
                        loggerType == Logger.LOG && !"java.util.logging.Logger.getLogger".equals(leftSide)
                                ||
                        loggerType == Logger.JBOSS && !"org.jboss.logging.Logger.getLogger".equals(leftSide)
                                ||
                        loggerType == Logger.COMMONS && !"org.apache.commons.logging.LogFactory.getLog".equals(leftSide)
                ) {
                    return multiVariable;
                }

                //argument must match
                String className = getCursor().pollNearestMessage(CLASS_NAME);
                if (methodCall.getArguments().size() != 1 ||
                        !methodCall.getArguments().get(0).toString().equals(
                                loggerType == Logger.LOG
                                        ? className + ".class.getName()"
                                        : className + ".class"
                        )) {
                    return multiVariable;
                }

                Set<Result> loggers = getCursor().getNearestMessage(FOUND_LOGGER);
                loggers.add(new Result(multiVariable, loggerType));

                return null;
            }
    }

    private JavaTemplate getLombokTemplate(Logger type) {
        switch (type) {
            case SLF4J:
                return getLombokTemplate("Slf4j", "lombok.extern.slf4j.Slf4j");
            case LOG4J2:
                return getLombokTemplate("Log4j2", "lombok.extern.log4j.Log4j2");
            case LOG:
                return getLombokTemplate("Log", "lombok.extern.java.Log");
            case JBOSS:
                return getLombokTemplate("JBossLog", "lombok.extern.jbosslog.JBossLog");
            case COMMONS:
                return getLombokTemplate("CommonsLog", "lombok.extern.apachecommons.CommonsLog");
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
