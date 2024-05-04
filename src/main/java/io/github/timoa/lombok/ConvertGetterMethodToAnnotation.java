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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static org.openrewrite.java.tree.JavaType.*;

@Value
@EqualsAndHashCode(callSuper = false)
public class ConvertGetterMethodToAnnotation extends Recipe {

    @Override
    public String getDisplayName() {
        //language=markdown
        return "Convert getter methods to annotations";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "Convert trivial getter methods `@Getter` annotations on their respective fields.\n"
                + "\n"
                + "limitations:  \n"
                + " - Does not add a dependency to Lombok, users need to do that manually\n"
                + " - Ignores fields that are declared on the same line as others, e.g. `private int foo, bar;`."
                + "Users who have such fields are advised to separate them beforehand with "
                + "[org.openrewrite.staticanalysis.MultipleVariableDeclarations]"
                + "(https://docs.openrewrite.org/recipes/staticanalysis/multiplevariabledeclarations).\n"
                + " - Does not offer any of the configuration keys listed in https://projectlombok.org/features/GetterSetter.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MethodRemover();
    }


    private static class MethodRemover extends JavaIsoVisitor<ExecutionContext> {
        private static final String FIELDS_TO_DECORATE_KEY = "FIELDS_TO_DECORATE";

        //this set collects the fields for which existing methods should be removed and replaced by annotations
        private Set<Variable> fieldsToDecorate;

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {

            //initialize set of fields to annotate
            getCursor().putMessage(FIELDS_TO_DECORATE_KEY, new HashSet<Variable>());

            super.visitClassDeclaration(classDecl, ctx);

            fieldsToDecorate = getCursor().pollNearestMessage(FIELDS_TO_DECORATE_KEY);

            //if no applicable getter methods where found, return early
            if (fieldsToDecorate == null) {
                return classDecl;
            }

            maybeAddImport("lombok.Getter");//todo find out why this has to be both here and in the other visitor
            //filter out those statements that are the manually implemented getter methods that should be removed
            Predicate<Statement> filterOutMethods = s -> {
                if(s instanceof J.MethodDeclaration) {
                    J.MethodDeclaration method = (J.MethodDeclaration) s;
                    //if none of the fields we recorded matches, the method can stay
                    return fieldsToDecorate.stream()
                            .map(LombokUtils::deriveGetterMethodName)
                            .noneMatch(method.getSimpleName()::equals);
                }
                return true; //statements that are not method declarations can stay
            };

            List<Statement> statements = classDecl.getBody().getStatements()
                    .stream()
                    .filter(filterOutMethods)
                    .collect(Collectors.toList());
            return classDecl.withBody(classDecl.getBody().withStatements(statements));
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            assert method.getMethodType() != null;

            if (LombokUtils.isEffectivelyGetter(method)) {
                J.Return return_ = (J.Return) method.getBody().getStatements().get(0);
                Variable fieldType = ((J.Identifier) return_.getExpression()).getFieldType();
                boolean nameMatch = method.getSimpleName().equals(LombokUtils.deriveGetterMethodName(fieldType));
                if (nameMatch){
                    ((Set<Variable>) getCursor().getNearestMessage(FIELDS_TO_DECORATE_KEY)).add(fieldType);
                }
            }
            return method;
        }

        @Override
        protected void doAfterVisit(TreeVisitor<?, ExecutionContext> visitor) {
            //pass fields to next visitor, so it can put the annotations on them
            super.doAfterVisit(new FieldAnnotator(fieldsToDecorate.stream().map(Variable::getName).collect(Collectors.toSet())));
        }
    }


    @Value
    @EqualsAndHashCode(callSuper = false)
    static class FieldAnnotator extends JavaIsoVisitor<ExecutionContext>{

        Set<String> fieldsToDecorate;

        JavaTemplate getterAnnotation = JavaTemplate
                .builder("@Getter\n")
                .javaParser(JavaParser.fromJavaVersion()
                        .classpath("lombok"))
                .imports("lombok.Getter")
                .build();

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {

            //we accept only one var decl per line, see description
            if (multiVariable.getVariables().size() > 1) {
                return multiVariable;
            }

            J.VariableDeclarations.NamedVariable variable = multiVariable.getVariables().get(0);
            if (fieldsToDecorate.contains(variable.getSimpleName())) {
                J.VariableDeclarations annotated = getterAnnotation.apply(
                        getCursor(),
                        multiVariable.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
                maybeAddImport("lombok.Getter");//todo find out why this has to be both here and in the other visitor
                return annotated;
            }
            return multiVariable;
        }
    }
}
