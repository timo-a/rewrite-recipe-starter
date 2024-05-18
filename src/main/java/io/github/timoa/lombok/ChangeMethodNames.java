/*
 * Copyright 2020 the original author or authors.
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
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.DeclaresMethod;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;

import java.util.Map;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeMethodNames extends Recipe {

    @Option(displayName = "Replacement pattern",
            description = "A key value pair where " +
                    "the key is a method pattern that is used to find matching method declarations/invocations, " +
                    "and the value is the method name that will replace the existing name.",
            example = "\"org.mockito.Matchers anyVararg()\" -> \"any\"")
    Map<String, String> replacementPattern;

    @Option(displayName = "Match on overrides",
            description = "When enabled, find methods that are overrides of the method pattern.",
            required = false)
    @Nullable
    Boolean matchOverrides;

    @Option(displayName = "Ignore type definition",
            description = "When set to `true` the definition of the old type will be left untouched. " +
                    "This is useful when you're replacing usage of a class but don't want to rename it.",
            required = false)
    @Nullable
    Boolean ignoreDefinition;

    @Override
    public String getDisplayName() {
        return "Change method name";
    }

    @Override
    public String getDescription() {
        return "Rename a method.";
    }

    private JavaIsoVisitor<ExecutionContext> getCondition(final String methodPattern) {
        System.out.println("ordering a condition");

        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    JavaSourceFile cu = (JavaSourceFile) tree;
                    if (Boolean.TRUE.equals(ignoreDefinition)) {
                        J j = new DeclaresMethod<>(methodPattern, matchOverrides).visitNonNull(cu, ctx);
                        if (cu != j) {
                            return cu;
                        }
                    } else {
                        cu = (JavaSourceFile) new DeclaresMethod<>(methodPattern, matchOverrides).visitNonNull(cu, ctx);
                        if (cu != tree) {
                            return cu;
                        }
                    }
                    System.out.println("condition " + methodPattern + ": about to return uses method");
                    return new UsesMethod<>(methodPattern, matchOverrides).visitNonNull(cu, ctx);
                }
                return super.visit(tree, ctx);
            }
        };
    }
    
    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {

        JavaIsoVisitor<ExecutionContext>[] conditions = replacementPattern.keySet().stream().map(this::getCondition).toArray(JavaIsoVisitor[]::new);
        
        return Preconditions.check(Preconditions.or(conditions), new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                System.out.println("visit method declaration " + method.getSimpleName());
                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);

                J.NewClass newClass = getCursor().firstEnclosing(J.NewClass.class);
                J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                for (Map.Entry<String, String> entry : replacementPattern.entrySet()) {
                    String methodPattern = entry.getKey();
                    String newMethodName = entry.getValue();
                    final MethodMatcher methodMatcher = new MethodMatcher(methodPattern, matchOverrides);

                    boolean methodMatches = newClass != null && methodMatcher.matches(method, newClass) ||
                            classDecl != null && methodMatcher.matches(method, classDecl);
                    if (methodMatches) {
                        JavaType.Method type = m.getMethodType();
                        if (type != null) {
                            type = type.withName(newMethodName);
                        }
                        m = m.withName(m.getName().withSimpleName(newMethodName).withType(type)).withMethodType(type);
                        break;
                    }

                }
                return m;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                for (Map.Entry<String, String> entry : replacementPattern.entrySet()) {
                    String methodPattern = entry.getKey();
                    String newMethodName = entry.getValue();
                    final MethodMatcher methodMatcher = new MethodMatcher(methodPattern, matchOverrides);

                    if (methodMatcher.matches(method) && !method.getSimpleName().equals(newMethodName)) {
                        JavaType.Method type = m.getMethodType();
                        if (type != null) {
                            type = type.withName(newMethodName);
                        }
                        m = m.withName(m.getName().withSimpleName(newMethodName).withType(type)).withMethodType(type);
                        break;
                    }
                }
                return m;
            }

            @Override
            public J.MemberReference visitMemberReference(J.MemberReference memberRef, ExecutionContext ctx) {
                J.MemberReference m = super.visitMemberReference(memberRef, ctx);
                for (Map.Entry<String, String> entry : replacementPattern.entrySet()) {
                    String methodPattern = entry.getKey();
                    String newMethodName = entry.getValue();
                    final MethodMatcher methodMatcher = new MethodMatcher(methodPattern, matchOverrides);

                    if (methodMatcher.matches(m.getMethodType()) && !m.getReference().getSimpleName().equals(newMethodName)) {
                        JavaType.Method type = m.getMethodType();
                        if (type != null) {
                            type = type.withName(newMethodName);
                        }
                        m = m.withReference(m.getReference().withSimpleName(newMethodName)).withMethodType(type);
                        break;
                    }
                }
                return m;
            }

            /**
             * The only time field access should be relevant to changing method names is static imports.
             * This exists to turn
             * import static com.abc.B.static1;
             * into
             * import static com.abc.B.static2;
             */
            @Override
            public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                J.FieldAccess f = super.visitFieldAccess(fieldAccess, ctx);
                for (Map.Entry<String, String> entry : replacementPattern.entrySet()) {
                    String methodPattern = entry.getKey();
                    String newMethodName = entry.getValue();
                    final MethodMatcher methodMatcher = new MethodMatcher(methodPattern, matchOverrides);

                    if (getCursor().getParentTreeCursor().getValue() instanceof J.Import && methodMatcher.isFullyQualifiedClassReference(f)) {
                        Expression target = f.getTarget();
                        if (target instanceof J.FieldAccess) {
                            String className = target.printTrimmed(getCursor());
                            String fullyQualified = className + "." + newMethodName;
                            return TypeTree.build(fullyQualified)
                                    .withPrefix(f.getPrefix());
                        }
                    }
                }
                return f;
            }
        });
    }
}