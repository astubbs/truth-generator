package io.stubbs.truth.generator.internal;

import com.google.common.truth.Subject;
import io.stubbs.truth.generator.BaseSubjectExtension;
import io.stubbs.truth.generator.ReflectionContext;
import io.stubbs.truth.generator.UserManagedMiddleSubject;
import io.stubbs.truth.generator.UserManagedSubject;
import io.stubbs.truth.generator.internal.model.UserSuppliedCompiledMiddleClass;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Antony Stubbs
 */
@Slf4j
public class ReflectionUtils {

    @Getter
    private final ReflectionContext context;

    private Reflections reflections;

    public ReflectionUtils(ReflectionContext context) {
        this.context = context;
        setupReflections(context);
    }

    /**
     * Finds extensions to base Truth {@link Subject}s
     * <p>
     * These extensions get used in place of the base Subjects in the reference chain.
     *
     * @see BaseSubjectExtension
     */
    public Set<Class<?>> findBaseSubjectExtensions() {
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
        var basePackagesToScan = context.getBaseModelPackagesForReflectionScanning();
        basePackagesToScan.forEach(filterBuilder::includePackage);
        String[] packagesToScanArray = basePackagesToScan.toArray(new String[0]);

        ConfigurationBuilder build = new ConfigurationBuilder()
                .forPackages(packagesToScanArray)
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
            basePackagesToScan.forEach(modelPackage -> build.forPackage(modelPackage, loadersArray));
        }

        this.reflections = new Reflections(build);
    }

    /**
     * Look for compiled {@link UserManagedSubject}s for the provided Class on the classpath.
     *
     * @param clazzUnderTest the class to look for a {@link UserManagedSubject} for
     */
    public <T> Optional<UserSuppliedCompiledMiddleClass<T>> tryGetUserManagedMiddle(final Class<T> clazzUnderTest) {
        // todo cache this on startup
        var annotatedWithUserManaged =
                this.reflections.getTypesAnnotatedWith(UserManagedSubject.class);

        var foundUserManagedSubjectsWithMatchingTargetClass = annotatedWithUserManaged
                .stream()
                .filter(x -> {
                    final UserManagedSubject annotation = x.getAnnotation(UserManagedSubject.class);

                    if (annotation == null) {
                        // should never happen as we've already filtered by annotation
                        return false;
                    }

                    final Class<?> annotationValue = annotation.value();
                    final String annotatedValueClassName = annotationValue.getCanonicalName();
                    final String underTestFullName = clazzUnderTest.getCanonicalName();

                    return annotatedValueClassName.equals(underTestFullName);
                })
                .collect(Collectors.toList());

        if (foundUserManagedSubjectsWithMatchingTargetClass.size() > 1) {
            log.warn("Found more than one {} for {}. Taking first, ignoring the rest - found: {}",
                    UserManagedSubject.class, clazzUnderTest, foundUserManagedSubjectsWithMatchingTargetClass);
        }

        // this section of code, javac needs some type hints with the streams
        Stream<UserSuppliedCompiledMiddleClass<T>> wrappedUserSubjects = foundUserManagedSubjectsWithMatchingTargetClass.stream().flatMap(matchingUserSubjectClass -> {

            Stream<? extends Class<?>> maybeLoadedUserSubjectClass = context.getLoaders().stream()
                    .flatMap(classLoader -> {
                        String userSubjectFullName = matchingUserSubjectClass.getCanonicalName();
                        try {
                            // try to load the target class and return it
                            Class<?> loadedUserSubjectClass = classLoader.loadClass(userSubjectFullName);
                            return Stream.of(loadedUserSubjectClass);
                        } catch (ClassNotFoundException e) {
                            // fail loading the class for whatever reason and return empty
                            log.debug("Can't load class + " + userSubjectFullName, e);
                            return Stream.of();
                        }
                    });

            Stream<UserSuppliedCompiledMiddleClass<T>> maybeWrappedUserSubject = maybeLoadedUserSubjectClass
                    .map(userSubjectClass -> {
                        Class<? extends UserManagedMiddleSubject<T>> userSubjectClassTypeHint = (Class<? extends UserManagedMiddleSubject<T>>) userSubjectClass; // javac needs the type help
                        return new UserSuppliedCompiledMiddleClass<>(userSubjectClassTypeHint, clazzUnderTest);
                    });

            return maybeWrappedUserSubject;
        });

        // only take the first successful result
        return wrappedUserSubjects.findFirst();
    }

}
