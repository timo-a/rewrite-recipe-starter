package io.github.timoa.misc;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.StringJoiner;

@Value
@EqualsAndHashCode(callSuper = false)
public class NormalizeBigDecimalCreation extends Recipe {

    @Override
    public String getDisplayName() {
        return "Use Strings for BigDecimal instantiation";
    }

    @Override
    public String getDescription() {
        return new StringJoiner("\n")
                .add("Converts BigDecimal instantiation with a double literal to instantiation with a String. ")
                .add("CAUTION: This Recipe can change semantics. ")
                .add("When using doubles to instantiate BigDecimal objects it is not obvious what the result will be. " +
                        "`new BigDecimal(0.1)` for instance results in a value of " +
                        "`0.1000000000000000055511151231257827021181583404541015625` and a scale of `55`. " +
                        "Other than rounding, the scale is also not transparent. While `new BigDecimal(\"1.00\")` will " +
                        "have a value of `1.00` and a scale 2, neither `new BigDecimal(1.00)` nor `BigDecimal.valueOf(1.00)` will.")
                .add("This recipe makes the assumption that a developer initializing a BigDecimal with the double " +
                        "`some.digits` really wants the object resulting from `new BigDecimal(\"some.digit\")` but is not aware of the difference.")
                .toString();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {

            final MethodMatcher VALUE_OF = new MethodMatcher("java.math.BigDecimal valueOf(double)");

            final MethodMatcher CONSTRUCTOR = new MethodMatcher("java.math.BigDecimal <constructor>(double)");

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (VALUE_OF.matches(method)) {
                    Expression doubleExpression = method.getArguments().get(0);
                    if (doubleExpression instanceof J.Literal) {
                        J.Literal doubleLiteral = (J.Literal) method.getArguments().get(0);
                        return makeBigDecimal(doubleLiteral, getCursor(), method.getCoordinates().replace());
                    }
                }
                return method;
            }

            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                if (CONSTRUCTOR.matches(newClass)) {
                    Expression doubleExpression = newClass.getArguments().get(0);
                    if (doubleExpression instanceof J.Literal) {
                        J.Literal doubleLiteral = (J.Literal) newClass.getArguments().get(0);
                        return makeBigDecimal(doubleLiteral, getCursor(), newClass.getCoordinates().replace());
                    }
                }
                return newClass;
            }

            private J.NewClass makeBigDecimal(J.Literal doubleLiteral,  org.openrewrite.Cursor scope, org.openrewrite.java.tree.JavaCoordinates coordinates) {
                String converted = doubleLiteral.getValueSource().replace("d", "");//remove trailing 'd' if present
                return JavaTemplate
                        .builder("new BigDecimal(\"" + converted + "\")")
                        .imports("java.math.BigDecimal")
                        .build()
                        .apply(scope, coordinates);
            }
        };
    }

}
