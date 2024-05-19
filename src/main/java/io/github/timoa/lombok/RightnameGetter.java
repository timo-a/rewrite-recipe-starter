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
import org.openrewrite.*;
import org.openrewrite.java.*;
import org.openrewrite.java.tree.*;

import java.util.*;
import java.util.stream.Collectors;

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
        return "Rename methods that are effectively getter to the name lombok would give them." +
                "" +
                "limitations: does not respect the @override annotation at this point -> rewrites overwritten methods even if original is external. This can break the build!";
    }

    public static class MethodAcc  {
        List<RenameRecord> renameRecords = new ArrayList<>();
    }

    @Override
    public MethodAcc getInitialValue(ExecutionContext ctx) {
        System.out.println("getInitialValue called on "+ this.hashCode());
        return new MethodAcc();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(MethodAcc acc) {
        return new MethodRecorder(acc);
    }

    @Value
    private class RenameRecord {
        String package_;
        String className_;
        String methodName_;
        String newMethodName_;
    }
    private enum COORDINATES {PACKAGE, CLASSNAME};

    @RequiredArgsConstructor
    private class MethodRecorder extends JavaIsoVisitor<ExecutionContext> {

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
                    acc.renameRecords.add(
                            new RenameRecord(
                                    getCursor().getNearestMessage(COORDINATES.PACKAGE.name()),
                                    getCursor().getNearestMessage(COORDINATES.CLASSNAME.name()),
                                    actualMethodName,
                                    expectedMethodName
                            )
                    );
                }
                if (isMatch)
                    System.out.println("added to records");
            }
            return method;
        }
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(MethodAcc acc) {
        System.out.println("get visitor called");
        if (acc.renameRecords.isEmpty()){
            System.out.println("return empty");
            return new JavaIsoVisitor<>();
        }

        long limit = 148;//working range [1,~148]
        //I've seen 148 both work and not work...
        System.out.println("collecting to map with limit: " + limit);
        Map<String, String> collect = acc.renameRecords.stream().limit(limit)
                .collect(Collectors.toMap(
                        rr -> String.format("%s.%s %s()", rr.package_, rr.className_, rr.methodName_),
                        rr -> rr.newMethodName_)
        );
        System.out.println("collecting to map finished. Result has " + collect.size() + " entries. Passing them to the next recipe...");
        return (new ChangeMethodNames(collect, null, null)).getVisitor();
    }
}
