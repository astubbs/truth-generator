package io.stubbs.truth.generator.internal;

import com.google.common.flogger.FluentLogger;
import com.google.common.truth.Subject;
import io.stubbs.truth.generator.BaseSubjectExtension;
import io.stubbs.truth.generator.GeneratorException;
import io.stubbs.truth.generator.SourceClassSets;
import io.stubbs.truth.generator.TruthGeneratorAPI;
import io.stubbs.truth.generator.internal.model.Result;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.Validate;

import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.Set.of;
import static java.util.stream.Collectors.toSet;

/**
 * @author Antony Stubbs
 */
public class TruthGenerator implements TruthGeneratorAPI {

  private static final FluentLogger log = FluentLogger.forEnclosingClass();
  private final Path testOutputDir;
  private final Options options;

  private ClassUtils classUtils = new ClassUtils();

  @Setter
  @Getter
  private Optional<String> entryPoint = Optional.empty();

  /**
   * Base Truth subject extensions to inject into Subject tree
   *
   * @see io.stubbs.truth.generator.BaseSubjectExtension
   */
  private final Map<Class<?>, Class<? extends Subject>> subjectExtensions = new HashMap<>();

  public TruthGenerator(Path testOutputDirectory, Options options) {
    Options.setInstance(options);
    this.options = options;
    this.testOutputDir = testOutputDirectory;
    Utils.setOutputBase(this.testOutputDir);
    autoRegisterStandardSubjectExtension();
  }

  private void autoRegisterStandardSubjectExtension() {
    Set<Class<?>> nativeExtensions = classUtils.findNativeExtensions("io.stubbs");
    for (Class<?> nativeExtension : nativeExtensions) {
      BaseSubjectExtension[] annotationsByType = nativeExtension.getAnnotationsByType(BaseSubjectExtension.class);
      List<BaseSubjectExtension> list = Arrays.asList(annotationsByType);
      Validate.isTrue(list.size() == 1, "Class must be annotated exactly once - found: %s", list);
      BaseSubjectExtension baseSubjectExtension = list.get(0);
      Class<?> targetClass = baseSubjectExtension.value();
      if (Subject.class.isAssignableFrom(nativeExtension)) {
        //noinspection unchecked - checked above as assignable from
        Class<? extends Subject> nativeExtensionSubject = (Class<? extends Subject>) nativeExtension;
        registerStandardSubjectExtension(targetClass, nativeExtensionSubject);
      } else {
        throw new GeneratorException("Class that isn't a Subject incorrectly annotation with " + BaseSubjectExtension.class);
      }
    }
  }

  @Override
  public void generate(String... modelPackages) {
    Utils.requireNotEmpty(modelPackages);

    // just take the first for now
    // todo createEntryPointForPackages(modelPackages)
    String[] packageNameForOverall = modelPackages;
    OverallEntryPoint overallEntryPoint = new OverallEntryPoint(packageNameForOverall[0]);
    Set<ThreeSystem> subjectsSystems = generateSkeletonsFromPackages(stream(modelPackages).collect(toSet()), overallEntryPoint, null);

    //
    addTests(subjectsSystems);
    overallEntryPoint.createOverallAccessPoints();
  }

  private Set<ThreeSystem> generateSkeletonsFromPackages(Set<String> modelPackages, OverallEntryPoint overallEntryPoint, SourceClassSets ss) {
    Set<Class<?>> allTypes = classUtils.collectSourceClasses(ss, modelPackages.toArray(new String[0]));
    return generateSkeletons(allTypes, Optional.empty(), overallEntryPoint);
  }

  private Set<ThreeSystem> generateSkeletons(Set<Class<?>> classes, Optional<String> targetPackageName,
                                             OverallEntryPoint overallEntryPoint) {
    int sizeBeforeFilter = classes.size();
    classes = filterSubjects(classes, sizeBeforeFilter);

    Set<ThreeSystem> subjectsSystems = new HashSet<>();
    for (Class<?> clazz : classes) {
      SkeletonGenerator skeletonGenerator = new SkeletonGenerator(targetPackageName, overallEntryPoint);
      Optional<ThreeSystem> threeSystem = skeletonGenerator.threeLayerSystem(clazz);
      if (threeSystem.isPresent()) {
        ThreeSystem ts = threeSystem.get();
        subjectsSystems.add(ts);
        overallEntryPoint.add(ts);
      }
    }
    return subjectsSystems;
  }

  private Set<Class<?>> filterSubjects(Set<Class<?>> classes, int sizeBeforeFilter) {
    // filter existing subjects from inbound set
    classes = classes.stream().filter(x -> !Subject.class.isAssignableFrom(x)).collect(toSet());
    log.at(Level.FINE).log("Removed %s Subjects from inbound", classes.size() - sizeBeforeFilter);
    return classes;
  }

  private void addTests(final Set<ThreeSystem> allTypes) {
    SubjectMethodGenerator tg = new SubjectMethodGenerator(allTypes, subjectExtensions);
    tg.addTests(allTypes);
  }

  @Override
  public void generateFromPackagesOf(Class<?>... classes) {
    Optional<Class<?>> first = stream(classes).findFirst();
    if (first.isEmpty()) throw new IllegalArgumentException("Must provide at least one Class");
    SourceClassSets ss = new SourceClassSets(first.get().getPackage().getName());
    ss.generateAllFoundInPackagesOf(classes);
    generate(ss);
  }

  @Override
  public void combinedSystem(final SourceClassSets ss) {
    throw new IllegalStateException(); // todo - remove?
  }

  @Override
  public Map<Class<?>, ThreeSystem> generate(SourceClassSets ss) {
    RecursiveChecker rc = new RecursiveChecker();
    Result.ResultBuilder results = Result.builder();

    if (options.isRecursive()) {
      Set<Class<?>> referencedBuilt = rc.addReferencedIncluded(ss);
      log.at(Level.INFO)
              .log("Added classes not explicitly configured: %s", referencedBuilt);
      results.referencedBuilt(referencedBuilt);
    } else {
      Set<Class<?>> missing = rc.findReferencedNotIncluded(ss);
      if (!missing.isEmpty()) {
        results.referencedNotBuild(missing);
        log.at(Level.WARNING)
                .log("Some referenced classes in the tree are not in the list of Subjects to be generated. " +
                        "Consider using automatic recursive generation, or add the missing classes. " +
                        "Otherwise your experience will be limited in places." +
                        "Missing classes %s", missing);
      }
    }

    // from packages
    Set<String> packages = ss.getSimplePackages();

    OverallEntryPoint packageForEntryPoint = new OverallEntryPoint(ss.getPackageForEntryPoint());

    // skeletons generation is independent and should be able to be done in parallel
    Set<ThreeSystem> skeletons = packages.parallelStream().flatMap(
            aPackage ->
                    generateSkeletonsFromPackages(of(aPackage), packageForEntryPoint, ss).stream()
    ).collect(toSet());

    // custom package destination
    Set<SourceClassSets.TargetPackageAndClasses> targetPackageAndClasses = ss.getTargetPackageAndClasses();
    Set<ThreeSystem> setStream = targetPackageAndClasses.stream().flatMap(
            x -> {
              Set<Class<?>> collect = stream(x.getClasses()).collect(toSet());
              return generateSkeletons(collect, Optional.of(x.getTargetPackageName()), packageForEntryPoint).stream();
            }
    ).collect(toSet());

    // straight up classes
    // TODO support overriding target package
    Optional<String> targetPackageName = Optional.empty();
    Set<ThreeSystem> simpleClasses = generateSkeletons(ss.getSimpleClasses(), targetPackageName, packageForEntryPoint);

    // legacy classes
    Set<ThreeSystem> legacyClasses = generateSkeletons(ss.getLegacyBeans(), targetPackageName, packageForEntryPoint);
    legacyClasses.forEach(x -> x.setLegacyMode(true));

    // legacy classes with custom package destination
    Set<SourceClassSets.TargetPackageAndClasses> legacyTargetPackageAndClasses = ss.getLegacyTargetPackageAndClasses();
    Set<ThreeSystem> legacyPackageSet = legacyTargetPackageAndClasses.stream().flatMap(
            x -> {
              Set<Class<?>> collect = stream(x.getClasses()).collect(toSet());
              return generateSkeletons(collect, Optional.of(x.getTargetPackageName()), packageForEntryPoint).stream();
            }
    ).collect(toSet());
    legacyPackageSet.forEach(x -> x.setLegacyMode(true));


    // add tests
    Set<ThreeSystem> union = new HashSet<>();
    union.addAll(skeletons);
    union.addAll(setStream);
    union.addAll(simpleClasses);
    union.addAll(legacyClasses);
    union.addAll(legacyPackageSet);

    if (union.isEmpty())
      log.atWarning().log("Nothing generated. Check your settings.");

    //
    addTests(union);

    // create overall entry point
    packageForEntryPoint.createOverallAccessPoints();

    return union.stream().collect(Collectors.toMap(ThreeSystem::getClassUnderTest, x -> x));
  }

  @Override
  public Map<Class<?>, ThreeSystem> generate(Set<Class<?>> classes) {
    Utils.requireNotEmpty(classes);
    String entrypointPackage = (this.entryPoint.isPresent())
            ? entryPoint.get()
            : createEntrypointPackage(classes);
    SourceClassSets ss = new SourceClassSets(entrypointPackage);
    ss.generateFrom(classes);
    return generate(ss);
  }

  /**
   * todo change this to do this by finding the highest common package of all outputs
   */
  private String createEntrypointPackage(final Set<Class<?>> classes) {
    return classes.stream().findFirst().get().getPackageName();
  }

  @Override
  public Map<Class<?>, ThreeSystem> generate(Class<?>... classes) {
    return generate(stream(classes).collect(toSet()));
  }

  @Override
  public void registerStandardSubjectExtension(Class<?> targetType, Class<? extends Subject> subjectExtensionClass) {
    this.subjectExtensions.put(targetType, subjectExtensionClass);
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
}
