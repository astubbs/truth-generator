package io.stubbs.truth.generator.internal;

import io.stubbs.truth.generator.internal.model.ThreeSystem;
import lombok.extern.slf4j.Slf4j;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

import java.lang.reflect.Method;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.removeStart;

/**
 * @author Antony Stubbs
 */
@Slf4j
public class EqualityStrategy extends AssertionMethodStrategy {


    @Override
    public boolean addStrategyMaybe(ThreeSystem<?> threeSystem, Method method, JavaClassSource generated) {
        equalityStrategyGeneric(method, generated, false);
        equalityStrategyGeneric(method, generated, true);
        return true;
    }

    private void equalityStrategyGeneric(Method method, JavaClassSource generated, boolean positive) {
        Class<?> returnType = method.getReturnType();
        boolean primitive = returnType.isPrimitive();
        String equality = primitive ? " == expected" : ".equals(expected)";

        String body = "" +
                "  if (%s(actual.%s()%s)) {\n" +
                "    failWithActual(fact(\"expected %s %sto be equal to\", expected));\n" +
                "  }\n";

        body = "" +
                "  check().that().isEqualTo()if (%s(actual.%s()%s)) {\n" +
                "    failWithActual(fact(\"expected %s %sto be equal to\", expected));\n" +
                "  }\n";

        String testPrefix = positive ? "!" : "";
        String say = positive ? "" : "NOT ";
        String fieldName = removeStart(method.getName(), "get");
        body = format(body, testPrefix, method.getName(), equality, fieldName, say);

        String methodName = "has" + capitalize(fieldName) + capitalize(say.toLowerCase()).trim() + "EqualTo";
        MethodSource<JavaClassSource> newMethod = generated.addMethod();
        newMethod.setName(methodName)
                .setReturnTypeVoid()
                .setBody(body)
                .setPublic();
        newMethod.addParameter(returnType, "expected");

        newMethod.getJavaDoc().setText("Simple check for equality for all fields.");

        copyThrownExceptions(method, newMethod);
    }


}
