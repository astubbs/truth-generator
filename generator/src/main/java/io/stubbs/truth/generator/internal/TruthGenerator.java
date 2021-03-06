package io.stubbs.truth.generator.internal;

import com.google.common.flogger.FluentLogger;
import com.google.common.truth.Subject;
import io.stubbs.truth.generator.FullContext;
import io.stubbs.truth.generator.ReflectionContext;
import io.stubbs.truth.generator.SourceClassSets;
import io.stubbs.truth.generator.TruthGeneratorAPI;
import io.stubbs.truth.generator.internal.SourceCodeScanner.CPPackage;
import io.stubbs.truth.generator.internal.model.Result;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;

import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;

/**
 * Main engine for Truth Generator
 *
 * @author Antony Stubbs
 */
@Slf4j
public class TruthGenerator implements TruthGeneratorAPI {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private final Path testOutputDir;
    private final Options options;
    private final ReflectionUtils reflectionUtils;
    private final BuiltInSubjectTypeStore builtInStore;
    private final SourceCodeScanner sourceCodeScanner;

    @Setter
    @Getter
    private Optional<String> entryPoint = Optional.empty();

    public TruthGenerator(Options options, FullContext context) {
        Options.setInstance(options);
        this.options = options;
        this.testOutputDir = context.getTestOutputDirectory();
        Utils.setOutputBase(this.testOutputDir);
        this.reflectionUtils = new ReflectionUtils(context);
        this.builtInStore = new BuiltInSubjectTypeStore(this.reflectionUtils);

        Set<CPPackage> cpPackages = Set.of(new CPPackage("io.stubbs"),
                new CPPackage("io.confluent.parallelconsumer.truth"));

        Set<CPPackage> sourcePackagesToScanForSubjects = context.getBaseModelPackagesForReflectionScanning().stream()
                .distinct()
                .map(CPPackage::new)
                .collect(toSet());
        this.sourceCodeScanner = new SourceCodeScanner(context, sourcePackagesToScanForSubjects);
    }

    /**
     * todo change this to do this by finding the highest common package of all outputs
     */
    private String createEntrypointPackage(final Set<Class<?>> classes) {
        return classes.stream().findFirst().get().getPackageName();
    }

    @Override
    public String maintain(final Class source, final Class userAndGeneratedMix) {
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public <T> String combinedSystem(final Class<T> source) {
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public void combinedSystem(final String... modelPackages) {
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public void generate(String... modelPackages) {
        Utils.requireNotEmpty(modelPackages);

        // just take the first for now
        // todo createEntryPointForPackages(modelPackages)
        String[] packageNameForOverall = modelPackages;
        OverallEntryPoint overallEntryPoint = new OverallEntryPoint(packageNameForOverall[0], builtInStore);
        Set<ThreeSystem<?>> subjectsSystems = generateSkeletonsFromPackages(overallEntryPoint, null);

        //
        addTests(subjectsSystems);
        overallEntryPoint.create();
    }

    private Set<ThreeSystem<?>> generateSkeletonsFromPackages(OverallEntryPoint overallEntryPoint, SourceClassSets ss) {
        Set<Class<?>> distinctTypesFoundInPackages = reflectionUtils.findClassesInPackages(ss.getSimplePackageNames());

        // filter out already added
        if (ss != null) {
            var alreadyAdded = ss.getAllSpecifiedClasses();
            distinctTypesFoundInPackages.removeAll(alreadyAdded);
        }

        return generateSkeletons(distinctTypesFoundInPackages, Optional.empty(), overallEntryPoint);
    }

    private void addTests(final Set<ThreeSystem<?>> allTypes) {
        SubjectMethodGenerator tg = new SubjectMethodGenerator(allTypes, builtInStore);
        tg.addTests(allTypes);
    }

    private Set<ThreeSystem<?>> generateSkeletons(Set<Class<?>> classes, Optional<String> targetPackageName,
                                                  OverallEntryPoint overallEntryPoint) {
        int sizeBeforeFilter = classes.size();
        classes = filterSubjects(classes, sizeBeforeFilter);

        Set<ThreeSystem<?>> subjectsSystems = new HashSet<>();
        for (Class<?> clazz : classes) {
            SkeletonGenerator skeletonGenerator = new SkeletonGenerator(targetPackageName,
                    overallEntryPoint,
                    this.builtInStore,
                    this.reflectionUtils,
                    this.sourceCodeScanner);
            var threeSystem = skeletonGenerator.threeLayerSystem(clazz);
            if (threeSystem.isPresent()) {
                ThreeSystem<?> ts = threeSystem.get();
                subjectsSystems.add(ts);
                overallEntryPoint.add(ts);
            }
        }
        return subjectsSystems;
    }

    private Set<Class<?>> filterSubjects(Set<Class<?>> classes, int sizeBeforeFilter) {
        // filter existing subjects from inbound set
        classes = classes.stream().filter(x -> !Subject.class.isAssignableFrom(x)).collect(toSet());
        logger.at(Level.FINE).log("Removed %s Subjects from inbound", classes.size() - sizeBeforeFilter);
        return classes;
    }

    @Override
    public Result generate(SourceClassSets ss) {
        RecursiveClassDiscovery rc = new RecursiveClassDiscovery();
        Result.ResultBuilder results = Result.builder();

        if (options.isRecursive()) {
            Set<Class<?>> referencedBuilt = rc.addReferencedIncluded(ss);
            var reduce = StreamEx.of(referencedBuilt)
                    .sorted(Comparator.comparing(Class::toString))
                    .joining("\n", "\n", "");
            log.info("Added classes not explicitly configured: {}", referencedBuilt.isEmpty() ? "none" : reduce);
            results.referencedBuilt(referencedBuilt);
        } else {
            Set<Class<?>> missing = rc.findReferencedNotIncluded(ss);
            if (!missing.isEmpty()) {
                results.referencedNotBuild(missing);
                logger.at(Level.WARNING)
                        .log("Some referenced classes in the tree are not in the list of Subjects to be generated. " +
                                "Consider using automatic recursive generation, or add the missing classes. " +
                                "Otherwise your experience will be limited in places." +
                                "Missing classes %s", missing);
            }
        }

        OverallEntryPoint packageForEntryPoint = new OverallEntryPoint(ss.getPackageForEntryPoint(), builtInStore);

        Set<ThreeSystem<?>> fromPackage = generateSkeletonsFromPackages(packageForEntryPoint, ss);

        // custom package destination
        Set<SourceClassSets.TargetPackageAndClasses> targetPackageAndClasses = ss.getTargetPackageAndClasses();
        Set<ThreeSystem<?>> setStream = targetPackageAndClasses.stream().flatMap(
                x -> {
                    Set<Class<?>> collect = stream(x.getClasses()).collect(toSet());
                    return generateSkeletons(collect, Optional.of(x.getTargetPackageName()), packageForEntryPoint).stream();
                }
        ).collect(toSet());

        // straight up classes
        // TODO support overriding target package
        Optional<String> targetPackageName = Optional.empty();
        Set<ThreeSystem<?>> fromExplicitClasses = generateSkeletons(ss.getSimpleClasses(), targetPackageName, packageForEntryPoint);

        // recursive discovery
        Set<ThreeSystem<?>> fromRecursion = generateSkeletons(ss.getReferencedNotSpecifiedClasses(), targetPackageName, packageForEntryPoint);

        // legacy classes
        Set<ThreeSystem<?>> fromLegacyClasses = generateSkeletons(ss.getLegacyBeans(), targetPackageName, packageForEntryPoint);
        fromLegacyClasses.forEach(x -> x.setLegacyMode(true));

        // legacy classes with custom package destination
        Set<SourceClassSets.TargetPackageAndClasses> legacyTargetPackageAndClasses = ss.getLegacyTargetPackageAndClasses();
        Set<ThreeSystem<?>> fromLegacyPackageSet = legacyTargetPackageAndClasses.stream().flatMap(
                x -> {
                    Set<Class<?>> collect = stream(x.getClasses()).collect(toSet());
                    return generateSkeletons(collect, Optional.of(x.getTargetPackageName()), packageForEntryPoint).stream();
                }
        ).collect(toSet());
        fromLegacyPackageSet.forEach(x -> x.setLegacyMode(true));


        // add tests
        Set<ThreeSystem<?>> union = new HashSet<>();
        union.addAll(fromPackage);
        union.addAll(setStream);
        union.addAll(fromExplicitClasses);
        union.addAll(fromRecursion);
        union.addAll(fromLegacyClasses);
        union.addAll(fromLegacyPackageSet);

        if (union.isEmpty())
            logger.atWarning().log("Nothing generated. Check your settings.");

        //
        addTests(union);

        // create overall entry point
        packageForEntryPoint.create();
        results.overallEntryPoint(packageForEntryPoint);

        Map<Class<?>, ThreeSystem<?>> all = union.stream().collect(Collectors.toMap(ThreeSystem::getClassUnderTest, x -> x));
        results.all(all);

        return results.build();
    }


    /**
     * Convenience method for testing that doesn't uses a default Context - see {@link ReflectionContext#Context()}.
     */
    @Override
    public Result generate(Set<Class<?>> classes) {
        Utils.requireNotEmpty(classes);
        String entrypointPackage = (this.entryPoint.isPresent())
                ? entryPoint.get()
                : createEntrypointPackage(classes);
        SourceClassSets ss = new SourceClassSets(entrypointPackage, reflectionUtils);
        ss.generateFrom(classes);
        return generate(ss);
    }

    /**
     * Convenience method for testing that doesn't uses a default Context - see {@link ReflectionContext#Context()}.
     */
    @Override
    public Result generate(Class<?>... classes) {
        return generate(stream(classes).collect(toSet()));
    }

    @Override
    public void registerStandardSubjectExtension(Class<?> targetType, Class<? extends Subject> subjectExtensionClass) {
        builtInStore.registerStandardSubjectExtension(targetType, subjectExtensionClass);
    }

}
