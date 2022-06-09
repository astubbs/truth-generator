package io.stubbs.truth.generator.internal;

import com.google.common.truth.Subject;
import io.stubbs.truth.generator.BaseSubjectExtension;
import io.stubbs.truth.generator.UserManagedMiddleSubject;
import io.stubbs.truth.generator.UserManagedTruth;
import io.stubbs.truth.generator.internal.model.MiddleClass;
import io.stubbs.truth.generator.internal.model.UserSuppliedMiddleClass;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class ReflectionUtils {

    // todo not used?
//    private List<ClassLoader> loaders = new ArrayList<>();
    private Reflections reflections;

    public ReflectionUtils(List<ClassLoader> loaders, Set<String> modelPackages) {
//        this.loaders = ss.getLoaders();
        setupReflections(loaders, modelPackages);
    }

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
//                .forPackages(modelPackages)
//                .filterInputsBy(new FilterBuilder().includePackage(modelPackages[0])) // TODO test different packages work?
//                .setParallel(true)
//                .setScanners(Scanners.TypesAnnotated, Scanners.SubTypes);
//
//        Reflections reflections = new Reflections(build);
//
        return reflections.getTypesAnnotatedWith(BaseSubjectExtension.class);
    }

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

    private void setupReflections(List<ClassLoader> loaders, Set<String> modelPackages) {
        // todo big smell - introduce config item to specify places to look for things
        String modelPackage = modelPackages.stream().findFirst().get();
        ConfigurationBuilder build = new ConfigurationBuilder()
                .forPackages(modelPackages.toArray(new String[0]))
                // TODO test different packages work?
                .filterInputsBy(new FilterBuilder().includePackage(modelPackage))
                // don't exclude Object sub types - don't filter out anything
                .setScanners(Scanners.SubTypes.filterResultsBy(s -> true))
                .setExpandSuperTypes(true);

        // todo smelly
        {
            for (ClassLoader loader : loaders) {
                build.addClassLoaders(loader);
            }
            ClassLoader[] loadersArray = loaders.toArray(new ClassLoader[0]);
            // shouldn't need to specify loaders twice?
            build = build.forPackage(modelPackage, loadersArray);
        }

        this.reflections = new Reflections(build);
    }

//    public void addClassLoaders(List<ClassLoader> loaders) {
//        this.loaders.addAll(loaders);
//    }

    public <T> Optional<MiddleClass<T>> tryGetUserManagedMiddle(final Class<T> clazzUnderTest) {
        var classStreamInt = reflections.getSubTypesOf(UserManagedMiddleSubject.class).stream()
                .filter(x -> x.isAnnotationPresent(UserManagedTruth.class))
                .filter(x -> x.getAnnotation(UserManagedTruth.class).value().equals(clazzUnderTest))
                .collect(Collectors.toList());

        var classStream = this.reflections.getTypesAnnotatedWith(UserManagedTruth.class)
                .stream()
                .filter(x -> x.getAnnotation(UserManagedTruth.class).value().equals(clazzUnderTest))
                .collect(Collectors.toList());
        if (classStream.size() > 1) {
            log.warn("Found more than one {} for {}. Taking first, ignoring the rest - found: {}",
                    UserManagedTruth.class, clazzUnderTest, classStream);
        }

        return classStream.stream().findFirst().map(aClass ->
                new UserSuppliedMiddleClass<T>((Class<? extends UserManagedMiddleSubject<T>>) aClass, clazzUnderTest)
        );
    }

}
