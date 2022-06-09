package io.stubbs.truth.generator.internal;

import com.google.common.truth.Subject;
import io.stubbs.truth.generator.BaseSubjectExtension;
import io.stubbs.truth.generator.ReflectionContext;
import io.stubbs.truth.generator.UserManagedMiddleSubject;
import io.stubbs.truth.generator.UserManagedTruth;
import io.stubbs.truth.generator.internal.model.MiddleClass;
import io.stubbs.truth.generator.internal.model.UserSuppliedMiddleClass;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.Store;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.reflections.util.QueryFunction;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class ReflectionUtils {

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
        final QueryFunction<Store, Map.Entry<String, Set<String>>> storeEntryQueryFunction = ctx -> {
            final Set<Map.Entry<String, Map<String, Set<String>>>> entries = ctx.entrySet();

            final Map<String, Set<String>> stringSetMap = ctx.get(Scanners.SubTypes.index());
            final Set<Map.Entry<String, Set<String>>> collect = stringSetMap.entrySet().stream().filter(stringSetEntry -> {
                final String key = stringSetEntry.getKey();
                return packages.contains(key);
            }).collect(Collectors.toSet());

//            return entries.stream().filter(stringMapEntry -> {
//                final Collection<Set<String>> values = stringMapEntry.getValue().values();
//                return packages.contains("");
//            }).collect(Collectors.toUnmodifiableSet());
            return collect;
        };
        Set<Class<?>> allTypes = reflections.get(storeEntryQueryFunction.asClass(reflections.getConfiguration().getClassLoaders()));


        // get's all the enum types
        // todo filter by package
        // https://github.com/ronmamo/reflections/issues/126
        Set<Class<? extends Enum>> subTypesOfEnums = reflections.getSubTypesOf(Enum.class);

        Set<Class<?>> allTypesOld = reflections.getSubTypesOf(Object.class)
                // remove Subject classes from previous runs
                .stream().filter(x -> !Subject.class.isAssignableFrom(x))
                .collect(Collectors.toSet());

        allTypes.addAll(subTypesOfEnums);

        return allTypes;
    }

    private void setupReflections(ReflectionContext context) {
        FilterBuilder filterBuilder = new FilterBuilder();
        context.getBaseModelPackagesFroScanning().forEach(filterBuilder::includePackage);

        ConfigurationBuilder build = new ConfigurationBuilder()
                .forPackages(context.getBaseModelPackagesFroScanning().toArray(new String[0]))
                .filterInputsBy(filterBuilder)
                .setParallel(true)
                // don't exclude Object subtypes - don't filter out anything
                .setScanners(Scanners.TypesAnnotated, Scanners.SubTypes.filterResultsBy(s -> true))
                .setExpandSuperTypes(true);

        {
            for (ClassLoader loader : context.getLoaders()) {
                build.addClassLoaders(loader);
            }
//
//            ClassLoader[] loadersArray = context.getLoaders().toArray(new ClassLoader[0]);
//            // shouldn't need to specify loaders twice?
//            build = build.forPackage(modelPackage, loadersArray);
        }

        this.reflections = new Reflections(build);
    }

//    public void addClassLoaders(List<ClassLoader> loaders) {
//        this.loaders.addAll(loaders);
//    }

    public <T> Optional<MiddleClass<T>> tryGetUserManagedMiddle(final Class<T> clazzUnderTest) {
//        var classStreamInt = this.reflections.getSubTypesOf(UserManagedMiddleSubject.class).stream()
//                .filter(x -> x.isAnnotationPresent(UserManagedTruth.class))
//                .filter(x -> x.getAnnotation(UserManagedTruth.class).value().equals(clazzUnderTest))
//                .collect(Collectors.toList());

//        UserManagedTruth annotation = UserManagedTruth.class.getAnnotatedSupercla   ss()isAnnotation(UserManagedTruth.class);

//        var classes2 = this.reflections.getTypesAnnotatedWith(annotation);

        var annotated =
                this.reflections.get(Scanners.TypesAnnotated.with(UserManagedTruth.class)
                        .asClass(reflections.getConfiguration().getClassLoaders()));

//        var classes3 =
//                this.reflections.getTypesAnnotatedWith(UserManagedTruth.class);


        var classStream = annotated
                .stream()
                .filter(x -> x.getAnnotation(UserManagedTruth.class).value().equals(clazzUnderTest))
                .collect(Collectors.toList());

        if (classStream.size() > 1) {
            log.warn("Found more than one {} for {}. Taking first, ignoring the rest - found: {}",
                    UserManagedTruth.class, clazzUnderTest, classStream);
        }

        //noinspection unchecked
        return classStream.stream().findFirst().map(aClass ->
                new UserSuppliedMiddleClass<T>((Class<? extends UserManagedMiddleSubject<T>>) aClass, clazzUnderTest)
        );
    }

}
