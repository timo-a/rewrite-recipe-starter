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
package com.yourorg;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.java.TreeVisitingPrinter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.*;

@Value
@EqualsAndHashCode(callSuper = false)
public class DuplicateConstructorArguments extends Recipe {

    @Override
    public String getDisplayName() {
        //language=markdown
        return "Duplicate the arguments of a one arg constructor";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "Duplicate the arguments of a one arg constructor.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                String printed = TreeVisitingPrinter.printTree(cu);
                System.out.println(printed);
                return super.visitCompilationUnit(cu, ctx);
            }

            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext executionContext) {
                //condition that will no longer hold after the first visit
                if (block.getStatements().size() == 1 && block.getStatements().get(0).toString().length() == 14) {

                    //imagine some analysis here that visitNewClass will read
                    List<Statement> preAnalysis = block.getStatements();

                    J.Block superBlock = super.visitBlock(block, executionContext);//this should visit the constructor

                    List<Statement> resultingStatements = new ArrayList<>();
                    resultingStatements.add(superBlock.getStatements().get(0));
                    //imagine a filter operation here

                    return superBlock.withStatements(resultingStatements);
                } else {
                    //needs super, so that inner blocks are visited
                    return super.visitBlock(block, executionContext);
                }
            }

            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext executionContext) {
                Expression e = newClass.getArguments().get(0);
                Expression f = e.withId(UUID.randomUUID());//in case there is an issue with two elements having the same id

                return super.visitNewClass(
                        newClass.withArguments(Arrays.asList(e,f)), //new A(...)
                        executionContext);
            }
        };
    }
}
