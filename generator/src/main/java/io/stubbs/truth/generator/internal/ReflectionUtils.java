package io.stubbs.truth.generator.internal;

import com.google.common.truth.Subject;
import io.stubbs.truth.generator.BaseSubjectExtension;
import io.stubbs.truth.generator.SourceClassSets;
import io.stubbs.truth.generator.UserManagedMiddleSubject;
import io.stubbs.truth.generator.UserManagedTruth;
import io.stubbs.truth.generator.internal.model.MiddleClass;
import io.stubbs.truth.generator.internal.model.UserSuppliedMiddleClass;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class ReflectionUtils {

    // todo not used?
    private List<ClassLoader> loaders = new ArrayList<>();
    private Reflections reflections;

    public ReflectionUtils(SourceClassSets ss, String... modelPackages) {
        this.loaders = ss.getLoaders();
        setupReflections(ss, modelPackages);
    }

    public Set<Class<?>> findNativeExtensions() {
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

    public Set<Class<?>> collectSourceClasses(SourceClassSets ss, String... modelPackages) {
        // https://github.com/ronmamo/reflections/issues/126
        Set<Class<? extends Enum>> subTypesOfEnums = reflections.getSubTypesOf(Enum.class);

        Set<Class<?>> allTypes = reflections.getSubTypesOf(Object.class)
                // remove Subject classes from previous runs
                .stream().filter(x -> !Subject.class.isAssignableFrom(x))
                .collect(Collectors.toSet());
        allTypes.addAll(subTypesOfEnums);

        return allTypes;
    }

    private void setupReflections(SourceClassSets ss, String[] modelPackages) {
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

        this.reflections = new Reflections(build);
    }

    public void addClassLoaders(List<ClassLoader> loaders) {
        this.loaders.addAll(loaders);
    }


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
