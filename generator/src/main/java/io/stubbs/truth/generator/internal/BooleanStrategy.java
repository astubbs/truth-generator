package io.stubbs.truth.generator.internal;

import io.stubbs.truth.generator.internal.model.ThreeSystem;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

import java.lang.reflect.Method;
import java.util.Optional;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.removeStart;

@Slf4j
public class BooleanStrategy extends MethodStrategy {

    @Override
    protected boolean addStrategyMaybe(ThreeSystem<?> threeSystem, Method method, JavaClassSource generated) {
        var positive = addBooleanGeneric(threeSystem, method, generated, true);
        var negative = addBooleanGeneric(threeSystem, method, generated, false);
        return positive.isPresent() && negative.isPresent();
    }

    private Optional<MethodSource<JavaClassSource>> addBooleanGeneric(ThreeSystem<?> threeSystem, Method method, JavaClassSource generated, boolean positive) {
        String testPrefix = positive ? "!" : "";
        String say = positive ? "" : "NOT ";

        String body = "" +
                "  if (%sactual.%s()) {\n" +
                "    failWithActual(simpleFact(\"expected %sto be %s\"));\n" +
                "  }\n";

        String noun = StringUtils.remove(method.getName(), "is");

        body = format(body, testPrefix, method.getName(), say, noun);

        String methodName = removeStart(method.getName(), "is");
        methodName = "is" + capitalize(say.toLowerCase()).trim() + methodName;

        if (generated.getMethod(methodName) == null && !methodAlreadyExistsInSuperAndIsFinal(methodName, threeSystem)) {
            MethodSource<JavaClassSource> booleanMethod = generated.addMethod();
            booleanMethod
                    .setName(methodName)
                    .setReturnTypeVoid()
                    .setBody(body)
                    .setPublic();

            copyThrownExceptions(method, booleanMethod);

            booleanMethod.getJavaDoc().setText("Simple is or is not expectation for boolean fields.");

            return Optional.of(booleanMethod);
        } else {
            log.warn("Method name collision, skipping adding boolean generic for {}", methodName);
            return Optional.empty();
        }
    }

}
