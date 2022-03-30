package io.stubbs.truth.generator.internal;

import com.google.common.truth.Subject;
import io.stubbs.truth.generator.BaseSubjectExtension;
import io.stubbs.truth.generator.SourceClassSets;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Antony Stubbs
 */
public class ClassUtils {

    // todo not used?
    private List<ClassLoader> loaders = new ArrayList<>();

    public static String maybeGetSimpleName(Type elementType) {
        return (elementType instanceof Class<?>) ? ((Class<?>) elementType).getSimpleName() : elementType.getTypeName();
    }

    static Class getStrippedReturnTypeFirstGenericParam(Method method) {
        Type genericReturnType = method.getGenericReturnType();
        return (Class) getStrippedReturnTypeFirstGenericParam(genericReturnType);
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

    public Set<Class<?>> findNativeExtensions(String... modelPackages) {
        // TODO share Reflections instance?
        ConfigurationBuilder build = new ConfigurationBuilder()
                .forPackages(modelPackages)
                .filterInputsBy(new FilterBuilder().includePackage(modelPackages[0])) // TODO test different packages work?
                .setParallel(true)
                .setScanners(Scanners.TypesAnnotated, Scanners.SubTypes);

        Reflections reflections = new Reflections(build);

        return reflections.getTypesAnnotatedWith(BaseSubjectExtension.class);
    }

    // TODO cleanup
    public Set<Class<?>> collectSourceClasses(SourceClassSets ss, String... modelPackages) {
        // TODO share Reflections instance for all classes in package?

        String modelPackage = modelPackages[0];
        ConfigurationBuilder build = new ConfigurationBuilder()
                .forPackages(modelPackages)
                .filterInputsBy(new FilterBuilder().includePackage(modelPackage)) // TODO test different packages work?
                // don't exclude Object sub types - don't filter out anything
                .setScanners(Scanners.SubTypes.filterResultsBy(s -> true))
                .setExpandSuperTypes(true);

        // todo smelly
        // attach class loaders
        if (ss != null) {
            List<ClassLoader> loaders = ss.getLoaders();

            for (ClassLoader loader : loaders) {
                build.addClassLoaders(loader);
            }

            ClassLoader[] loadersArray = loaders.toArray(new ClassLoader[0]);
            build = build.forPackage(modelPackage, loadersArray);
        }

        Reflections reflections = new Reflections(build);

        // https://github.com/ronmamo/reflections/issues/126
        Set<Class<? extends Enum>> subTypesOfEnums = reflections.getSubTypesOf(Enum.class);

        Set<Class<?>> allTypes = reflections.getSubTypesOf(Object.class)
                // remove Subject classes from previous runs
                .stream().filter(x -> !Subject.class.isAssignableFrom(x))
                .collect(Collectors.toSet());
        allTypes.addAll(subTypesOfEnums);

        return allTypes;
    }

    public void addClassLoaders(List<ClassLoader> loaders) {
        this.loaders.addAll(loaders);
    }
}
