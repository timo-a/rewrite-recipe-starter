package io.github.timoa.lombok;

import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.function.Predicate;

public class LombokUtils {

    public static boolean isEffectivelyGetter(J.MethodDeclaration method) {
        boolean takesNoParameters = method.getParameters().get(0) instanceof J.Empty;
        boolean singularReturn = method.getBody().getStatements().size() == 1
                && method.getBody().getStatements().get(0) instanceof J.Return;

        if (takesNoParameters && singularReturn) {
            Expression returnExpression = ((J.Return) method.getBody().getStatements().get(0)).getExpression();
            if (returnExpression instanceof J.Identifier) {
                J.Identifier identifier = (J.Identifier) returnExpression;
                JavaType.Variable fieldType = identifier.getFieldType();
                boolean typeMatch = method.getType().equals(fieldType.getType());
                return typeMatch;
            }
        }
        return false;
    }

    public static String deriveGetterMethodName(JavaType.Variable fieldType) {
        boolean isPrimitiveBoolean = JavaType.Variable.Primitive.Boolean.equals(fieldType.getType());

        final String fieldName = fieldType.getName();

        boolean alreadyStartsWithIs = fieldName.length() >= 3
                && fieldName.substring(0,3).matches("is[A-Z]");

        if (isPrimitiveBoolean)
            if(alreadyStartsWithIs)
                return fieldName;
            else
                return "is" + StringUtils.capitalize(fieldName);

        return "get" + StringUtils.capitalize(fieldName);
    }

}
