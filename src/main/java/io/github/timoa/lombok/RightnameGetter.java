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
import org.openrewrite.java.tree.JavaType;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

@Value
@EqualsAndHashCode(callSuper = false)
public class RightnameGetter extends Recipe {

    @Override
    public String getDisplayName() {
        //language=markdown
        return "Use `new ArrayList<>()` instead of Guava";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "Prefer the Java standard library over third-party usage of Guava in simple cases like this.";
    }

    static List<RenameRecord> renameRecords = new ArrayList<>();

    @Override
    public List<Recipe> getRecipeList() {
        return renameRecords.stream()
                .map(rr -> new ChangeMethodName(
                        String.format("%s.%s %s()", rr.package_, rr.className_, rr.methodName_),
                        rr.newMethodName_,
                        null,
                        null))
                .collect(Collectors.toList());
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MethodRecorder();
    }

    @Value
    private static class RenameRecord {
        String package_;
        String className_;
        String methodName_;
        String newMethodName_;
    }

    private static class MethodRecorder extends JavaIsoVisitor<ExecutionContext> {
        private enum COORDNINATES {PACKAGE, CLASSNAME};

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {

            J.Package package_ = cu.getPackageDeclaration();

            getCursor().putMessage(COORDNINATES.PACKAGE.name(), package_ == null ? "" : package_.getPackageName());


            String printed = TreeVisitingPrinter.printTree(cu);

            // You must always delegate to the super method to ensure the visitor continues to visit deeper
            return super.visitCompilationUnit(cu, ctx);
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {

            getCursor().putMessage(COORDNINATES.CLASSNAME.name(), classDecl.getSimpleName());

            super.visitClassDeclaration(classDecl, ctx);

            return classDecl;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            assert method.getMethodType() != null;

            if (LombokUtils.isEffectivelyGetter(method)) {

                J.Return return_ = (J.Return) method.getBody().getStatements().get(0);
                JavaType.Variable fieldType = ((J.Identifier) return_.getExpression()).getFieldType();

                String expectedMethodName = LombokUtils.deriveGetterMethodName(fieldType);
                String actualMethodName = method.getSimpleName();

                if (!expectedMethodName.equals(actualMethodName)){
                    renameRecords.add(
                            new RenameRecord(
                                    getCursor().getNearestMessage(COORDNINATES.PACKAGE.name()),
                                    getCursor().getNearestMessage(COORDNINATES.CLASSNAME.name()),
                                    actualMethodName,
                                    expectedMethodName
                            )
                    );
                }
            }
            return method;
        }
    }
}
