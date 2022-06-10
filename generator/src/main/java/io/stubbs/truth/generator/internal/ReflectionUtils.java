package io.stubbs.truth.generator.internal;

import com.google.common.truth.Subject;
import io.stubbs.truth.generator.BaseSubjectExtension;
import io.stubbs.truth.generator.ReflectionContext;
import io.stubbs.truth.generator.UserManagedMiddleSubject;
import io.stubbs.truth.generator.UserManagedTruth;
import io.stubbs.truth.generator.internal.model.UserSuppliedMiddleClass;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.Store;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.reflections.util.QueryFunction;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class ReflectionUtils {

    @Getter
    private final ReflectionContext context;

    // todo not used?
//    private List<ClassLoader> loaders = new ArrayList<>();
    private Reflections reflections;

    public ReflectionUtils(ReflectionContext context) {
//        this.loaders = ss.getLoaders();
        this.context = context;
        setupReflections(context);
    }

//    /**
//     * Reflection utils with no special class loaders, or specific model packages to restrict scanning to. Useful for
//     * running outside of MOJO (maven plugin) contexts, e.g. tests.
//     */
//    public ReflectionUtils(Context ) {
//        setupReflections(new Context(baseModelPackagesFroScanning));
//    }

    /**
     * Finds extensions to base Truth {@link Subject}s
     * <p>
     * These extensions get used in place of the base Subjects in the reference chain.
     *
     * @see BaseSubjectExtension
     */
    public Set<Class<?>> findBaseSubjectExtensions() {
//    public Set<Class<?>> findNativeExtensions(String... modelPackages) {
//        // TODO share Reflections instance?
//        ConfigurationBuilder build = new ConfigurationBuilder()
//                .forPackages("io.stubbs")
//                .filterInputsBy(new FilterBuilder().includePackage("io.stubbs")) // TODO test different packages work?
//                .setParallel(true)
//                .setScanners(Scanners.TypesAnnotated, Scanners.SubTypes);
//
//        Reflections reflectionsold = new Reflections(build);
//        Set<Class<?>> old = reflectionsold.getTypesAnnotatedWith(BaseSubjectExtension.class);

        Set<Class<?>> typesAnnotatedWith = reflections.getTypesAnnotatedWith(BaseSubjectExtension.class);
        return typesAnnotatedWith;
    }

    /**
     * Finds all the classes within the configured {@link ReflectionContext}.
     */
    public Set<Class<?>> collectSourceClasses() {
        // https://github.com/ronmamo/reflections/issues/126
        Set<Class<? extends Enum>> subTypesOfEnums = reflections.getSubTypesOf(Enum.class);

        Set<Class<?>> allTypes = reflections.getSubTypesOf(Object.class)
                // remove Subject classes from previous runs
                .stream().filter(x -> !Subject.class.isAssignableFrom(x))
                .collect(Collectors.toSet());

        allTypes.addAll(subTypesOfEnums);

        return allTypes;
    }

    public Set<Class<?>> findClassesInPackages(Set<String> packages) {
        // todo remove
        final QueryFunction<Store, Map.Entry<String, Set<String>>> storeEntryQueryFunction = ctx -> {
            final Set<Map.Entry<String, Map<String, Set<String>>>> entries = ctx.entrySet();

            final Map<String, Set<String>> stringSetMap = ctx.get(Scanners.SubTypes.index());
            final Set<Map.Entry<String, Set<String>>> collect = stringSetMap.entrySet().stream().filter(stringSetEntry -> {
                final String key = stringSetEntry.getKey();
                final boolean contains = packages.contains(key);
                return contains;
            }).collect(Collectors.toSet());

//            return entries.stream().filter(stringMapEntry -> {
//                final Collection<Set<String>> values = stringMapEntry.getValue().values();
//                return packages.contains("");
//            }).collect(Collectors.toUnmodifiableSet());
            return collect;
        };
        // todo remove
        // is missing MyEmployee subclass
        Set<Class<?>> collect = reflections.get(storeEntryQueryFunction.asClass(reflections.getConfiguration().getClassLoaders()));

//        final Set<T> ts = reflections.get(Scanners.SubTypes.filterResultsBy(s -> true).);

        // get's all the enum types
        // todo filter by package
        // https://github.com/ronmamo/reflections/issues/126
        Set<Class<? extends Enum>> subTypesOfEnums = reflections.getSubTypesOf(Enum.class);

        Set<Class<?>> allTypesOld = reflections.getSubTypesOf(Object.class)
                .stream()
                // remove Subject classes from previous runs
                .filter(x -> !Subject.class.isAssignableFrom(x))
                .collect(Collectors.toSet());
        // todo possible to query this directly on reflections?
        var allTypes = allTypesOld.stream().filter(x -> packages.contains(x.getPackageName())).collect(Collectors.toSet());

        allTypes.addAll(subTypesOfEnums);

        return allTypes;
    }

    private void setupReflections(ReflectionContext context) {
        FilterBuilder filterBuilder = new FilterBuilder();
        var basePackages = context.getBaseModelPackagesFroScanning();
        basePackages.forEach(filterBuilder::includePackage);

        ConfigurationBuilder build = new ConfigurationBuilder()
                .forPackages(basePackages.toArray(new String[0]))
                .filterInputsBy(filterBuilder)
                .setParallel(true)
                // don't exclude Object subtypes - don't filter out anything
                .setScanners(Scanners.TypesAnnotated, Scanners.SubTypes.filterResultsBy(s -> true))
                .setExpandSuperTypes(false);

        {
            // this approach doesn't work? - try removing and testing with PC
            for (ClassLoader loader : context.getLoaders()) {
                build.addClassLoaders(loader);
            }

            // is this duplicate approach required? correct?
            ClassLoader[] loadersArray = context.getLoaders().toArray(new ClassLoader[0]);
            // shouldn't need to specify loaders twice?
            basePackages.forEach(modelPackage -> build.forPackage(modelPackage, loadersArray));
        }

        this.reflections = new Reflections(build);
    }

//    public void addClassLoaders(List<ClassLoader> loaders) {
//        this.loaders.addAll(loaders);
//    }

    public <T> Optional<UserSuppliedMiddleClass<T>> tryGetUserManagedMiddle(final Class<T> clazzUnderTest) {
//        var classStreamInt = this.reflections.getSubTypesOf(UserManagedMiddleSubject.class).stream()
//                .filter(x -> x.isAnnotationPresent(UserManagedTruth.class))
//                .filter(x -> x.getAnnotation(UserManagedTruth.class).value().equals(clazzUnderTest))
//                .collect(Collectors.toList());

//        UserManagedTruth annotation = UserManagedTruth.class.getAnnotatedSupercla   ss()isAnnotation(UserManagedTruth.class);

//        var classes2 = this.reflections.getTypesAnnotatedWith(annotation);

        var annotatedOld =
                this.reflections.get(Scanners.TypesAnnotated.with(UserManagedTruth.class)
                        .asClass(reflections.getConfiguration().getClassLoaders()));

        // todo cache this on startup
        var annotated =
                this.reflections.getTypesAnnotatedWith(UserManagedTruth.class);


        var classStream = annotated
                .stream()
                .filter(x -> {
                    final UserManagedTruth annotation = x.getAnnotation(UserManagedTruth.class);
                    if (annotation == null)
                        return false;
                    final Class<?> value = annotation.value();
                    final ClassLoader classLoader = value.getClassLoader();
                    final String canonicalName = value.getCanonicalName();

                    final String canonicalName1 = clazzUnderTest.getCanonicalName();
                    final ClassLoader classLoader1 = clazzUnderTest.getClassLoader();

                    return canonicalName.equals(canonicalName1);
                })
                .collect(Collectors.toList());

        if (classStream.size() > 1) {
            log.warn("Found more than one {} for {}. Taking first, ignoring the rest - found: {}",
                    UserManagedTruth.class, clazzUnderTest, classStream);
        }

        //noinspection unchecked
        final Optional<Class<?>> first = classStream.stream().findFirst();

//        context.getLoaders().stream().forEach(x -> x.loadClass());

//        final Optional<UserSuppliedMiddleClass<T>> tUserSuppliedMiddleClass = first.map(aClass ->
//                {
//                    final String canonicalName = aClass.getCanonicalName();
//                    Class<?> aClass1 = null;
//                    try {
//                        aClass1 = Class.forName(canonicalName);
//                    } catch (ClassNotFoundException e) {
//                        e.printStackTrace();
//                    }
//                    return new UserSuppliedMiddleClass<T>((Class<? extends UserManagedMiddleSubject<T>>) aClass1, clazzUnderTest);
//                }
//        );

        final Optional<UserSuppliedMiddleClass<T>> tMiddleClass = first.map(aClass ->
                {
//                    final Class<?> aClass1 = aClass;
                    final List<ClassLoader> loaders = context.getLoaders();
                    final Class<?> aClass1 = loaders.stream().flatMap(classLoader -> {
                        try {
                            final ClassLoader classLoader1 = aClass.getClassLoader();
                            final String canonicalName = aClass.getCanonicalName();
                            final Class<?> t = classLoader.loadClass(canonicalName);
                            return Stream.of(t);
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                            return Stream.of();
                        }
                    }).findFirst().get();

                    return new UserSuppliedMiddleClass<T>((Class<? extends UserManagedMiddleSubject<T>>) aClass1, clazzUnderTest);
                }
        );

        return tMiddleClass;
    }

}
