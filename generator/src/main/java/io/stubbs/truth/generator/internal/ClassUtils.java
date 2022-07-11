package io.stubbs.truth.generator.internal;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

/**
 * @author Antony Stubbs
 */
@Slf4j
@UtilityClass
public class ClassUtils {

    public static String maybeGetSimpleName(Type elementType) {
        return (elementType instanceof Class<?>) ? ((Class<?>) elementType).getSimpleName() : elementType.getTypeName();
    }

    static Class<?> getStrippedReturnTypeFirstGenericParam(Method method) {
        Type genericReturnType = method.getGenericReturnType();
        return (Class<?>) getStrippedReturnTypeFirstGenericParam(genericReturnType);
    }

    private static Type getStrippedReturnTypeFirstGenericParam(Type genericReturnType) {
        Class<?> keyType = Object.class; // default fall back
        if (genericReturnType instanceof ParameterizedType) {
            ParameterizedType parameterizedReturnType = (ParameterizedType) genericReturnType;
            Type[] actualTypeArguments = parameterizedReturnType.getActualTypeArguments();
            if (actualTypeArguments.length > 0) { // must have at least 1
                Type key = actualTypeArguments[0];
                return getStrippedReturnType(key);
            }
        } else if (genericReturnType instanceof Class<?>) {
            return genericReturnType; // terminal
        }
        return keyType;
    }

    private static Type getStrippedReturnType(Type key) {
        if (key instanceof ParameterizedType) {
            // strip type arguments
            // could potentially add this as a type parameter to the method instead?
            ParameterizedType parameterizedKey = (ParameterizedType) key;
            Type rawType = parameterizedKey.getRawType();
            Type recursive = getStrippedReturnTypeFirstGenericParam(rawType);
            return recursive;
        } else if (key instanceof WildcardType) {
            // strip type arguments
            // could potentially add this as a type parameter to the method instead?
            WildcardType wildcardKey = (WildcardType) key;
            Type[] upperBounds = wildcardKey.getUpperBounds();
            if (upperBounds.length > 0) {
                Type upperBound = upperBounds[0];
                Type recursive = getStrippedReturnType(upperBound);
                return recursive;
            }
        }
        // else
        return key;
    }

}
