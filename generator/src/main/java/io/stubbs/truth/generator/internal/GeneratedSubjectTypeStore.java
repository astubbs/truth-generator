package io.stubbs.truth.generator.internal;

import com.google.common.flogger.FluentLogger;
import com.google.common.truth.ObjectArraySubject;
import com.google.common.truth.Subject;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import lombok.Value;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import net.jodah.typetools.TypeResolver;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.stubbs.truth.generator.internal.Utils.msg;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.lang3.ClassUtils.primitiveToWrapper;
import static org.apache.commons.lang3.StringUtils.capitalize;

/**
 * Management class for generated as well as standard Subject lookup based on classes.
 *
 * @author Antony Stubbs
 * @see BuiltInSubjectTypeStore
 */
@Slf4j
public class GeneratedSubjectTypeStore {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    private final Map<String, ThreeSystem<?>> generatedSubjects;

    @Delegate
    private final BuiltInSubjectTypeStore builtInSubjectTypeStore;

    /**
     * Useful for testing - a store with no generated skeleton types
     */
    protected GeneratedSubjectTypeStore() {
        this(Set.of(), new BuiltInSubjectTypeStore());
    }

    public GeneratedSubjectTypeStore(Set<? extends ThreeSystem<?>> allTypes, BuiltInSubjectTypeStore builtInSubjectTypeStore) {
        this.generatedSubjects = allTypes.stream().collect(Collectors.toMap(x -> x.classUnderTest.getName(), x -> x));
        this.builtInSubjectTypeStore = builtInSubjectTypeStore;
    }

    protected Optional<SubjectMethodGenerator.ClassOrGenerated> getSubjectForType(final Class<?> type) {
        String name;

        // if primitive, wrap and get wrapped Subject
        if (type.isPrimitive()) {
            Class<?> wrapped = primitiveToWrapper(type);
            name = wrapped.getName();
        } else {
            name = type.getName();
        }

        // arrays
        if (type.isArray()) {
            Class<?> componentType = type.getComponentType();
            if (componentType.isPrimitive()) {
                // PrimitiveBooleanArraySubject
                String simpleName = componentType.getSimpleName();
                simpleName = capitalize(simpleName);
                String subjectPrefix = "Primitive" + simpleName + "Array";
                var compiledSubjectForTypeName = builtInSubjectTypeStore.getCompiledSubjectForTypeName(subjectPrefix);
                return SubjectMethodGenerator.ClassOrGenerated.ofClass(compiledSubjectForTypeName);
            } else {
                return SubjectMethodGenerator.ClassOrGenerated.ofClass(ObjectArraySubject.class);
            }
        }

        return getSubjectForNotNativeType(name, type);
    }

    protected Optional<SubjectMethodGenerator.ClassOrGenerated> getSubjectForNotNativeType(
            String nameOfType, // todo redundant? and smelly
            Class<?> type) {
        // explicit extensions take priority
        Class<? extends Subject> extension = this.builtInSubjectTypeStore.getSubjectExtensions(type);
        if (extension != null)
            return SubjectMethodGenerator.ClassOrGenerated.ofClass(extension);

        //
        Optional<SubjectMethodGenerator.ClassOrGenerated> subject = getGeneratedOrCompiledSubjectFromString(nameOfType);

        // Can't find any generated ones or compiled ones - fall back to native subjects that are assignable (e.g. comparable or iterable)
        if (subject.isEmpty()) {
            Optional<Class<? extends Subject>> nativeSubjectForType = builtInSubjectTypeStore.getClosestTruthNativeSubjectForType(type);

            subject = SubjectMethodGenerator.ClassOrGenerated.ofClass(nativeSubjectForType);
            subject.ifPresent(classOrGenerated -> log.debug("For type {} - falling back to native interface subject {}", type, classOrGenerated.clazz));
        }

        return subject;
    }

    private Optional<SubjectMethodGenerator.ClassOrGenerated> getGeneratedOrCompiledSubjectFromString(
            final String name) {
        boolean isObjectArray = name.endsWith("[]");
        if (isObjectArray)
            return of(new SubjectMethodGenerator.ClassOrGenerated(ObjectArraySubject.class, null));

        Optional<ThreeSystem<?>> subjectFromGenerated = getSubjectFromGenerated(name);// takes precedence
        if (subjectFromGenerated.isPresent()) {
            return of(new SubjectMethodGenerator.ClassOrGenerated(null, subjectFromGenerated.get()));
        }

        // any matching compiled subject
        var aClass = builtInSubjectTypeStore.getCompiledSubjectForTypeName(name);
        if (aClass != null)
            return of(new SubjectMethodGenerator.ClassOrGenerated(aClass, null));

        return empty();
    }

    private Optional<ThreeSystem<?>> getSubjectFromGenerated(final String name) {
        return Optional.ofNullable(this.generatedSubjects.get(name));
    }

    public boolean isAnExtendedSubject(Class<?> clazz) {
        return builtInSubjectTypeStore.isAnExtendedSubject(clazz);
    }

    public ResolvedPair resolveSubjectForOptionals(final ThreeSystem<?> threeSystem, Method method) {
        Class<?> classUnderTest = threeSystem.classUnderTest;
        Class<?> rawReturnType = method.getReturnType();
        Type genericReturnTypeRaw = method.getGenericReturnType();

        Class<?> returnType = rawReturnType;
        boolean optionalUnwrap = false;
        if (rawReturnType.isPrimitive()) {
            returnType = getWrappedReturnType(method);
        } else {
            Type maybeParamType;
            try {
                maybeParamType = TypeResolver.reify(genericReturnTypeRaw, classUnderTest);
                if (maybeParamType instanceof Class) {
                    returnType = (Class<?>) maybeParamType;
                } else if (maybeParamType instanceof ParameterizedType && rawReturnType.isAssignableFrom(Optional.class)) {
                    ParameterizedType paramType = (ParameterizedType) maybeParamType;
                    Type[] actualTypeArguments = paramType.getActualTypeArguments();
                    if (actualTypeArguments.length == 1 && actualTypeArguments[0] instanceof Class<?>) {
                        returnType = (Class<?>) actualTypeArguments[0];
                        optionalUnwrap = true;
                    }
                }
            } catch (UnsupportedOperationException e) {
                log.warn(msg("Cannot get type param for return type of {} from {}, in ThreeSystem {}. Details: {}", rawReturnType, method, threeSystem, e.getMessage()));
            }

        }

        Optional<SubjectMethodGenerator.ClassOrGenerated> subjectForType = getSubjectForType(returnType);
        return new ResolvedPair(returnType, subjectForType, optionalUnwrap);
    }

    // todo rename unboxed
    protected Class<?> getWrappedReturnType(Method method) {
        return primitiveToWrapper(method.getReturnType());
    }

    @Value
    public static class ResolvedPair {
        @Nonnull
        Class<?> returnType;
        @Nonnull
        Optional<SubjectMethodGenerator.ClassOrGenerated> subject;
        boolean unwrapped;
    }

}
