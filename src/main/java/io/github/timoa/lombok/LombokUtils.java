package io.github.timoa.lombok;

import com.google.common.collect.ImmutableMap;
import lombok.AccessLevel;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static lombok.AccessLevel.*;
import static org.openrewrite.java.tree.J.Modifier.Type.*;

public class LombokUtils {

    public static boolean isEffectivelyGetter(J.MethodDeclaration method) {
        boolean takesNoParameters = method.getParameters().get(0) instanceof J.Empty;
        boolean singularReturn = method.getBody() != null //abstract methods can be null
                && method.getBody().getStatements().size() == 1
                && method.getBody().getStatements().get(0) instanceof J.Return;

        if (takesNoParameters && singularReturn) {
            Expression returnExpression = ((J.Return) method.getBody().getStatements().get(0)).getExpression();
            //returns just an identifier
            if (returnExpression instanceof J.Identifier) {
                J.Identifier identifier = (J.Identifier) returnExpression;
                JavaType.Variable fieldType = identifier.getFieldType();
                boolean typeMatch = method.getType().equals(fieldType.getType());
                return typeMatch;
            }
        }
        return false;
    }

    public static boolean isEffectivelySetter(J.MethodDeclaration method) {
        boolean isVoid = "void".equals(method.getType().toString());
        List<Statement> actualParameters = method.getParameters().stream()
                .filter(s -> !(s instanceof J.Empty))
                .collect(Collectors.toList());
        boolean oneParam = actualParameters.size() == 1;
        if (!isVoid || !oneParam)
            return false;

        J.VariableDeclarations variableDeclarations = (J.VariableDeclarations) actualParameters.get(0);
        J.VariableDeclarations.NamedVariable param = variableDeclarations.getVariables().get(0);
        String paramName = param.getName().toString();

        boolean singularStatement = method.getBody() != null //abstract methods can be null
                && method.getBody().getStatements().size() == 1
                && method.getBody().getStatements().get(0) instanceof J.Assignment;

        if (!singularStatement) {
            return false;
        }
        J.Assignment assignment = (J.Assignment) method.getBody().getStatements().get(0);

        J.FieldAccess fieldAccess = (J.FieldAccess) assignment.getVariable();

        return
                // assigned value is exactly the parameter
                assignment.getAssignment().toString().equals(paramName)

                        // type of parameter and field have to match
                        && param.getType().equals(fieldAccess.getType());

    }

    public static String deriveGetterMethodName(JavaType.Variable fieldType) {
        boolean isPrimitiveBoolean = JavaType.Variable.Primitive.Boolean.equals(fieldType.getType());

        final String fieldName = fieldType.getName();

        boolean alreadyStartsWithIs = fieldName.length() >= 3
                && fieldName.substring(0, 3).matches("is[A-Z]");

        if (isPrimitiveBoolean)
            if (alreadyStartsWithIs)
                return fieldName;
            else
                return "is" + StringUtils.capitalize(fieldName);

        return "get" + StringUtils.capitalize(fieldName);
    }

    public static String deriveSetterMethodName(JavaType.Variable fieldType) {
        return "set" + StringUtils.capitalize(fieldType.getName());
    }

    public static AccessLevel getAccessLevel(Collection<J.Modifier> modifiers) {
        Map<J.Modifier.Type, AccessLevel> map = ImmutableMap.<J.Modifier.Type, AccessLevel>builder()
                .put(Public, PUBLIC)
                .put(Protected, PROTECTED)
                .put(Private, PRIVATE)
                .build();

        return modifiers.stream()
                .map(modifier -> map.getOrDefault(modifier.getType(), AccessLevel.NONE))
                .filter(a -> a != AccessLevel.NONE)
                .findAny().orElse(AccessLevel.PACKAGE);
    }

}
