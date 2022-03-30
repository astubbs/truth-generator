package io.stubbs.truth.generator.internal;

import io.stubbs.truth.generator.internal.model.ThreeSystem;
import lombok.extern.slf4j.Slf4j;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

import java.lang.reflect.Method;
import java.util.Optional;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.removeStart;

/**
 * @author Antony Stubbs
 */
@Slf4j
public class OptionalStrategy extends AssertionMethodStrategy {

    @Override
    protected boolean addStrategyMaybe(ThreeSystem threeSystem, Method method, JavaClassSource generated) {
        if (Optional.class.isAssignableFrom(getWrappedReturnType(method))) {
            addOptionalStrategy(method, generated);
            return true;
        }
        return false;
    }

    public void addOptionalStrategy(Method method, JavaClassSource generated) {
        addOptionalStrategyGeneric(method, generated, false);
        addOptionalStrategyGeneric(method, generated, true);
    }

    public MethodSource<JavaClassSource> addOptionalStrategyGeneric(Method method, JavaClassSource generated, boolean positive) {
        String testPrefix = positive ? "!" : "";
        String body = "" +
                "  if (%sactual.%s().isPresent()) {\n" +
                "    failWithActual(simpleFact(\"expected %s %sto be present\"));\n" +
                "  }\n";

        String say = positive ? "" : "NOT ";
        String fieldName = removeStart(method.getName(), "get");
        body = format(body, testPrefix, method.getName(), fieldName, say);

        String methodName = "has" + capitalize(fieldName) + capitalize(say.toLowerCase()).trim() + "Present";
        MethodSource<JavaClassSource> newMethod = generated.addMethod();
        newMethod
                .setName(methodName)
                .setReturnTypeVoid()
                .setBody(body)
                .setPublic();

        newMethod.getJavaDoc().setText("Checks Optional fields for presence.");

        copyThrownExceptions(method, newMethod);

        return newMethod;
    }

}
