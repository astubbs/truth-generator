package io.stubbs.truth.generator;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;
import io.stubbs.truth.generator.internal.RecursiveClassDiscovery;
import io.stubbs.truth.generator.internal.ReflectionUtils;
import lombok.Getter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;

/**
 * Use this class to prepare the set of source classes to generate for, and settings for different types of sources.
 *
 * @author Antony Stubbs
 */
@Slf4j
@Getter
public class SourceClassSets {

    private final String packageForEntryPoint;

    private final ReflectionUtils reflectionUtils;

    /**
     * Source packages from which to scan for classes to generate Subjects for.
     */
    //todo rename
    private final Set<String> simplePackageNames = new HashSet<>();

    /**
     * todo docs
     */
    private final Set<Class<?>> simpleClasses = new HashSet<>();

    /**
     * todo docs
     */
    private final Set<TargetPackageAndClasses> targetPackageAndClasses = new HashSet<>();

    /**
     * todo docs
     */
    private final Set<Class<?>> legacyBeans = new HashSet<>();

    /**
     * todo docs
     */
    private final Set<TargetPackageAndClasses> legacyTargetPackageAndClasses = new HashSet<>();

    /**
     * todo docs
     */
    private Set<Class<?>> classSetCache;

    /**
     * Classes referenced by specified classes, which haven't been set explicitly (i.e. recursive graph feature)
     *
     * @see RecursiveClassDiscovery
     */
    private final Set<Class<?>> referencedNotSpecifiedClasses = new HashSet<>();

    /**
     * @param packageForEntryPoint the package to put the overall access points
     */
    public SourceClassSets(String packageForEntryPoint, ReflectionUtils reflectionUtils) {
        this.packageForEntryPoint = packageForEntryPoint;
        this.reflectionUtils = reflectionUtils;
    }

    /**
     * Use the package of the parameter as the base package;
     */
    public SourceClassSets(String packageForEntryPoint, ReflectionContext context) {
        this(packageForEntryPoint, new ReflectionUtils(context));
    }

    /**
     * Use the package of the parameter as the base package;
     */
    public SourceClassSets(Object packageFromObject, ReflectionUtils reflectionUtils) {
        this(packageFromObject.getClass().getPackage().getName(), reflectionUtils);
    }

    /**
     * Use the package of this class base package;
     */
    public SourceClassSets(Class<?> packageFromClass, ReflectionUtils reflectionUtils) {
        this(packageFromClass.getPackage().getName(), reflectionUtils);
    }

    /**
     * Use the package of this class base package;
     * <p>
     * Uses a default {@link ReflectionContext}.
     */
    public SourceClassSets(Class<?> packageFromClass, ReflectionContext reflectionUtils) {
        this(packageFromClass.getPackage().getName(), new ReflectionUtils(reflectionUtils));
    }

    public void generateAllFoundInPackagesOf(Class<?>... classes) {
        Set<String> collect = stream(classes).map(x -> x.getPackage().getName()).collect(toSet());
        simplePackageNames.addAll(collect);
    }

    public void generateAllFoundInPackages(Package... packages) {
        Set<String> collect = stream(packages).map(Package::getName).collect(toSet());
        simplePackageNames.addAll(collect);
    }

    public void generateAllFoundInPackages(String... packageNames) {
        // filter sub packages
        stream(packageNames).forEach(packageToAdd -> {
            Optional<String> matchingSuperPackage = simplePackageNames.stream()
                    .filter(packageToAdd::startsWith)
                    .findAny();
            if (matchingSuperPackage.isEmpty()) {
                simplePackageNames.add(packageToAdd);
            } else {
                log.info("Skipping package {}, is it is a sub package of {} which is already added", packageToAdd, matchingSuperPackage.get());
            }
        });
    }

    /**
     * Useful for generating Java module Subjects and put them in our package.
     * <p>
     * I.e. for UUID.class you can't create a Subject in the same package as it (not allowed).
     */
    public void generateFrom(String targetPackageName, Class<?>... classes) {
        targetPackageAndClasses.add(new TargetPackageAndClasses(targetPackageName, classes));
    }

    public void generateFrom(Set<Class<?>> classes) {
        this.simpleClasses.addAll(classes);
    }

    /**
     * Shades the given source classes into the base package, suffixed with the source package
     */
    public void generateFromShaded(Class<?>... classes) {
        Set<TargetPackageAndClasses> targetPackageAndClassesStream = mapToPackageSets(classes);
        this.targetPackageAndClasses.addAll(targetPackageAndClassesStream);
    }

    private Set<TargetPackageAndClasses> mapToPackageSets(Class<?>[] classes) {
        ImmutableListMultimap<Package, Class<?>> grouped = Multimaps.index(asList(classes), Class::getPackage);

        return grouped.keySet().stream().map(x -> {
            Class<?>[] classSet = grouped.get(x).toArray(new Class<?>[0]);
            TargetPackageAndClasses newSet = new TargetPackageAndClasses(getTargetPackageName(x),
                    classSet);
            return newSet;
        }).collect(toSet());
    }

    private String getTargetPackageName(Package p) {
        return this.packageForEntryPoint + ".shaded." + p.getName();
    }

    public void generateFromShadedNonBean(Class<?>... clazzes) {
        Set<TargetPackageAndClasses> targetPackageAndClassesStream = mapToPackageSets(clazzes);
        this.legacyTargetPackageAndClasses.addAll(targetPackageAndClassesStream);
    }

    public void generateFrom(ClassLoader loader, String... classes) {
        Class<?>[] as = stream(classes).map(x -> {
            try {
                return loader.loadClass(x);
            } catch (ClassNotFoundException e) {
                throw new GeneratorException("Cannot find class asked to generate from: " + x, e);
            }
        }).toArray(Class[]::new);
        generateFrom(as);
    }

    public void generateFrom(Class<?>... classes) {
        this.simpleClasses.addAll(stream(classes).collect(toSet()));
    }

    // todo docs
    // todo shouldn't be public?
    public Set<Class<?>> addIfMissing(final Set<? extends Class<?>> clazzes) {
        getAllEffectivelyConfiguredClasses(); // todo smelly, update class set cache
        var missing = clazzes.stream()
                .filter(x -> !classSetCache.contains(x)).collect(toSet());
        missing.forEach(this::generateFromReferencedNotSpecified);
        return (Set<Class<?>>) missing;
    }

    /**
     * Includes classes found in specified packages
     */
    // todo shouldn't be public?
    public Set<Class<?>> getAllEffectivelyConfiguredClasses() {
        Set<Class<?>> union = getAllSpecifiedClasses();

        var referencedNotSpecifiedClasses = getReferencedNotSpecifiedClasses();
        union.addAll(referencedNotSpecifiedClasses);

        var collected = reflectionUtils.collectSourceClasses()
                .stream().filter(aClass -> getSimplePackageNames().stream()
                        .anyMatch(configuredPackage -> configuredPackage.contains(aClass.getPackage().getName())))
                .collect(Collectors.toUnmodifiableSet());

        union.addAll(collected);

        // todo need more elegant solution than this
        this.classSetCache = union;
        return union;
    }

    public Set<Class<?>> getAllSpecifiedClasses() {
        Set<Class<?>> union = new HashSet<>();
        union.addAll(getSimpleClasses());
        union.addAll(getLegacyBeans());

        Set<Class<?>> collect = getTargetPackageAndClasses().stream().flatMap(x ->
                stream(x.classes)
        ).collect(toSet());
        union.addAll(collect);

        union.addAll(getLegacyTargetPackageAndClasses().stream().flatMap(x -> stream(x.classes)).collect(toSet()));
        return union;
    }

    public void generateFromReferencedNotSpecified(Class<?>... classes) {
        Set<Class<?>> allClasses = getAllSpecifiedClasses();
        stream(classes).forEach(x -> {
            if (!allClasses.contains(x)) {
                referencedNotSpecifiedClasses.add(x);
            }
        });
        referencedNotSpecifiedClasses.addAll(stream(classes).collect(toSet()));
    }

    // todo shouldn't be public?
    public boolean isClassIncluded(final Class<?> clazz) {
        return classSetCache.contains(clazz);
    }

    public boolean isLegacyClass(final Class<?> theClass) {
        return getLegacyBeans().contains(theClass)
                || getLegacyTargetPackageAndClasses().stream().anyMatch(x -> asList(x.classes).contains(theClass));
    }

    public void generateFromNonBean(ClassLoader loader, String[] legacyClasses) {
        if (legacyClasses == null)
            return;

        List<? extends Class<?>> collect = stream(legacyClasses).map(x -> {
            try {
                return loader.loadClass(x);
            } catch (ClassNotFoundException e) {
                throw new GeneratorException("Cannot find class asked to generate from: " + x, e);
            }
        }).collect(Collectors.toList());
        Class[] as = collect.toArray(new Class[0]);
        generateFromNonBean(as);
    }

    public void generateFromNonBean(Class<?>... nonBeanLegacyClass) {
        for (Class<?> beanLegacyClass : nonBeanLegacyClass) {
            legacyBeans.add(beanLegacyClass);
        }
    }

    /**
     * Container for classes and the target package they're to be produced into
     */
    @Value
    public static class TargetPackageAndClasses {
        String targetPackageName;
        Class<?>[] classes;
    }

}
