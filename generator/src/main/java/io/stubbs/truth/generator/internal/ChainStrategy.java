package io.stubbs.truth.generator.internal;

import com.google.common.flogger.FluentLogger;
import com.google.common.truth.BooleanSubject;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jboss.forge.roaster.model.source.Import;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

import static io.stubbs.truth.generator.internal.Utils.msg;
import static java.lang.String.format;
import static java.util.logging.Level.WARNING;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.removeStart;

@RequiredArgsConstructor
@Slf4j
public class ChainStrategy extends MethodStrategy {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    private final Options options = Options.get();

    private final GeneratedSubjectTypeStore subjects;

    @Override
    protected boolean addStrategyMaybe(ThreeSystem threeSystem, Method method, JavaClassSource generated) {
        addChainStrategy(threeSystem, method, generated);
        return true;
    }

    protected MethodSource<JavaClassSource> addChainStrategy(ThreeSystem threeSystem, Method method, JavaClassSource generated) {
        Class<?> returnType = getWrappedReturnType(method);
        Optional<SubjectMethodGenerator.ClassOrGenerated> subjectForType;
        boolean optionalUnwrap = false;

        if (returnType.isAssignableFrom(Optional.class) && returnType != Object.class) {
            // unwrap
            Type genericReturnTypeRaw = method.getGenericReturnType();
            if (ParameterizedType.class.isAssignableFrom(genericReturnTypeRaw.getClass())) {
                Type[] actualTypeArguments = ((ParameterizedType) genericReturnTypeRaw).getActualTypeArguments();
                if (actualTypeArguments.length == 1) {
                    Type optionalReturnType = actualTypeArguments[0];
                    if (optionalReturnType.getClass().isAssignableFrom(Class.class)) {
                        returnType = (Class<?>) optionalReturnType;
                        subjectForType = subjects.getSubjectForType(returnType);
                        optionalUnwrap = true;
                    } else {
                        throw new IllegalStateException("Can't cast optionally wrapped type to class");
                    }
                } else {
                    throw new IllegalStateException("Wrong number of type arguments for Optional - should be only one");
                }
            } else {
                log.warn("Optional type without type parameter, can't add optional chain.");
                return null;
            }
        } else {
            subjectForType = subjects.getSubjectForType(returnType);
        }

        boolean isCoveredByNonPrimitiveStandardSubjects = subjects.isTypeCoveredUnderStandardSubjects(returnType);

        // no subject to chain
        if (subjectForType.isEmpty()) {
            logger.at(WARNING).log("Cant find subject for " + returnType);
            return null;
        }

        SubjectMethodGenerator.ClassOrGenerated subjectClass = subjectForType.get();

        // check if should skip as there are more specific versions that would clash
        if (subjectClass.getSubjectSimpleName().equals(BooleanSubject.class.getSimpleName())) {
            return null;
        }

        String nameForChainMethod = createNameForChainMethod(threeSystem, method);
        MethodSource<JavaClassSource> has = generated.addMethod()
                .setName(nameForChainMethod)
                .setPublic();

        StringBuilder body = new StringBuilder("isNotNull();\n");

        if (optionalUnwrap) {
            String name = capitalize(removeStart(method.getName(), "get"));
            body.append("has" + name + "Present();\n");
        }

        String check = format("return check(\"%s()", method.getName());
        body.append(check);

        if (optionalUnwrap) {
            body.append(".get()");
        }

        body.append("\")");

        //
        boolean isAnExtendedSubject = subjects.isAnExtendedSubject(subjectClass.clazz);
        boolean notPrimitive = !returnType.isPrimitive();
        boolean needsAboutCall = notPrimitive && !isCoveredByNonPrimitiveStandardSubjects || isAnExtendedSubject;

        if (needsAboutCall || subjectClass.isGenerated()) {
            String aboutName;
            aboutName = Utils.getFactoryName(returnType); // take a guess
            body.append(format(".about(%s())", aboutName));

            // import
            String factoryContainer = subjectClass.getFactoryContainerName();
            Import anImport = generated.addImport(factoryContainer);
            String name = factoryContainer + "." + aboutName;
            anImport.setName(name) // todo better way to do static method import?
                    .setStatic(true);
        }

        /* Also add cast for return type for older JVMs that don't seem to be able to correctly check for the
         * generic return type from Optional.get, otherwise we seem to get a compilation error on older JVMs.
         */
        String maybeCast = optionalUnwrap ? msg("({})",returnType.getSimpleName()) : "";

        if (methodIsStatic(method)) {
            body.append(format(".that(%s%s.%s()",
                    maybeCast,
                    method.getDeclaringClass().getSimpleName(),
                    method.getName()));
        } else {
            body.append(format(".that(%sactual.%s()",
                    maybeCast,
                    method.getName()));
        }

        if (optionalUnwrap) {
            body.append(".get()");
        }

        body.append(");\n");

        //
        has.setBody(body.toString());

        has.setReturnType(subjectClass.getSubjectSimpleName());

        generated.addImport(subjectClass.getSubjectQualifiedName());
        generated.addImport(returnType);

        has.getJavaDoc().

                setText("Returns the Subject for the given field type, so you can chain on other assertions.");

        copyThrownExceptions(method, has);

        return has;
    }

    /**
     * Attempt to swap get for has, but only if it starts with get - otherwise leave it alone.
     */
    private String createNameForChainMethod(ThreeSystem threeSystem, Method method) {
        String name = method.getName();

        if (threeSystem.isLegacyMode()) {
            if (options.useGetterForLegacyClasses) {
                if (options.useHasInsteadOfGet) {
                    return "has" + capitalize(name);
                } else {
                    return "get" + capitalize(name);
                }
            } else {
                return name;
            }
        }

        if (name.startsWith("get")) {
            if (options.isUseHasInsteadOfGet()) {
                name = removeStart(name, "get");
                return "has" + name;
            } else {
                return name;
            }
        } else if (name.startsWith("is")) {
            return "has" + removeStart(name, "is");
        } else if (name.startsWith("to")) {
            return "has" + capitalize(name);
        } else
            return name;
    }

}
