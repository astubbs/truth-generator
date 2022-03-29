package io.stubbs.truth.generator.internal;

import io.stubbs.truth.generator.internal.model.ThreeSystem;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import static java.lang.reflect.Modifier.STATIC;
import static org.apache.commons.lang3.ClassUtils.primitiveToWrapper;
import static org.reflections.util.ReflectionUtilsPredicates.withModifier;

public abstract class MethodStrategy {

    protected abstract boolean addStrategyMaybe(ThreeSystem<?> threeSystem, Method method, JavaClassSource generated);

    protected void copyThrownExceptions(Method method, MethodSource<JavaClassSource> generated) {
        Class<? extends Exception>[] exceptionTypes = (Class<? extends Exception>[]) method.getExceptionTypes();
        Stream<Class<? extends Exception>> runtimeExceptions = Arrays.stream(exceptionTypes)
                .filter(x -> !RuntimeException.class.isAssignableFrom(x));
        runtimeExceptions.forEach(generated::addThrows);
    }

    protected Class<?> getWrappedReturnType(Method method) {
        Class<?> wrapped = primitiveToWrapper(method.getReturnType());
        return wrapped;
    }

    protected boolean methodIsStatic(Method method) {
        return withModifier(STATIC).test(method);
    }

    protected boolean methodAlreadyExistsInSuperAndIsFinal(String suggestedName, ThreeSystem<?> three) {
        var parent = three.getParent().getGenerated();
        String superType = parent.getSuperType();
        Class<?> superClass = null;
        try {
            superClass = Class.forName(superType);
        } catch (ClassNotFoundException e) {
            throw new TruthGeneratorRuntimeException(Utils.msg("Cannot load class ({}) that our parent ({}) extends in system {}", superType, parent, three), e);
        }

        Optional<Method> first = Arrays.stream(superClass.getDeclaredMethods())
                .filter(method -> method.getName().equals(suggestedName) && method.getParameterCount() == 0)
                .findFirst();

        if (first.isEmpty()) {
            return false;
        } else {
            var method = first.get();
            return Modifier.isFinal(method.getModifiers());
        }
    }

}
