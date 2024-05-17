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
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.*;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

@Value
@EqualsAndHashCode(callSuper = false)
public class RightnameGetter extends ScanningRecipe<RightnameGetter.MethodAcc> {

    @Override
    public String getDisplayName() {
        //language=markdown
        return "Correct the name of getter methods according to how lombok would name";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "Rename methods that are effectively getter to the name lombok would give them.";
    }

    static class MethodAcc  {
        static List<RenameRecord> renameRecords = new ArrayList<>();
    }

    @Override
    public MethodAcc getInitialValue(ExecutionContext ctx) {
        System.out.println("getInitialValue");
        return new MethodAcc();
    }

    @Override
    public List<Recipe> getRecipeList() {
        System.out.println("there are " + MethodAcc.renameRecords.size() + " records");
        MethodAcc.renameRecords.forEach(r -> System.out.println(String.format("%s.%s %s() -> %s", r.package_, r.className_, r.methodName_, r.newMethodName_)));
        return MethodAcc.renameRecords.stream()
                .map(rr -> new ChangeMethodName(
                        String.format("%s.%s %s()", rr.package_, rr.className_, rr.methodName_),
                        rr.newMethodName_,
                        null,
                        null))
                .collect(Collectors.toList());
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(MethodAcc acc) {
        return new MethodRecorder(acc);
    }

    @Value
    private static class RenameRecord {
        String package_;
        String className_;
        String methodName_;
        String newMethodName_;
    }

    @RequiredArgsConstructor
    private static class MethodRecorder extends JavaIsoVisitor<ExecutionContext> {
        private enum COORDINATES {PACKAGE, CLASSNAME};

        private final MethodAcc acc;

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {

            J.Package package_ = cu.getPackageDeclaration();

            getCursor().putMessage(COORDINATES.PACKAGE.name(), package_ == null ? "" : package_.getPackageName());

            // You must always delegate to the super method to ensure the visitor continues to visit deeper
            return super.visitCompilationUnit(cu, ctx);
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {

            getCursor().putMessage(COORDINATES.CLASSNAME.name(), classDecl.getSimpleName());

            super.visitClassDeclaration(classDecl, ctx);

            return classDecl;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            assert method.getMethodType() != null;

            boolean isMatch = method.getSimpleName().contains("getMaxRawContentLength");
            if (isMatch)
                System.out.println("visiting " + method.getSimpleName());

            if (LombokUtils.isEffectivelyGetter(method)) {
                if (isMatch)
                    System.out.println("iseffectivegetter");

                J.Return return_ = (J.Return) method.getBody().getStatements().get(0);
                JavaType.Variable fieldType = ((J.Identifier) return_.getExpression()).getFieldType();

                String expectedMethodName = LombokUtils.deriveGetterMethodName(fieldType);
                String actualMethodName = method.getSimpleName();

                if (!expectedMethodName.equals(actualMethodName)){
                    if (isMatch)
                        System.out.println("add to records");

                    acc.renameRecords.add(
                            new RenameRecord(
                                    getCursor().getNearestMessage(COORDINATES.PACKAGE.name()),
                                    getCursor().getNearestMessage(COORDINATES.CLASSNAME.name()),
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
