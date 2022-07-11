package io.stubbs.truth.generator.plugin;

import com.google.common.truth.Subject;
import io.stubbs.truth.generator.FullContext;
import io.stubbs.truth.generator.SourceClassSets;
import io.stubbs.truth.generator.UserManagedSubject;
import io.stubbs.truth.generator.internal.Options;
import io.stubbs.truth.generator.internal.ReflectionUtils;
import io.stubbs.truth.generator.internal.TruthGenerator;
import io.stubbs.truth.generator.internal.Utils;
import io.stubbs.truth.generator.internal.model.Result;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;


/**
 * Maven plugin for {@link TruthGenerator}.
 *
 * @author Antony Stubbs
 * @see TruthGenerator
 */
@Getter
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES, requiresDependencyResolution = TEST, requiresProject = true)
public class GeneratorMojo extends AbstractMojo {

    private static final String[] INCLUDE_ALL_CLASSES = {".*"};

    /**
     * Current maven project
     */
    @Parameter(property = "project", required = true, readonly = true)
    public MavenProject project;

    /**
     * Package where generated {@link Subject} classes will reside.
     * <p/>
     * If not set (or set to empty), each Subject class is generated in the package of the corresponding class to
     * assert. For example the generated assertion class for com.nba.Player will be com.nba.PlayerAssert (in the same
     * package as Player). Defaults to ''.
     * <p/>
     * Note that the Subject entry point classes package is controlled by the {@link #entryPointClassPackage} property.
     */
    @Parameter(defaultValue = "", property = "truth.generateAssertionsInPackage")
    public String generateAssertionsInPackage;

    /**
     * List of packages to look for user manages Subjects in. If blank, everything will be checked, which will be a lot
     * slower.
     */
    @Parameter(property = "truth.subjectPackages")
    public String[] subjectPackages;

    /**
     * Flag specifying whether to clean the directory where assertions are generated. The default is false.
     */
    @Parameter(defaultValue = "false", property = "truth.cleanTargetDir")
    public boolean cleanTargetDir;

    /**
     * Use 'has' in generated assertion chain methods instead of 'get'.
     */
    @Parameter(defaultValue = "false", property = "truth.useHas")
    public boolean useHas;

    /**
     * List of packages to look for classes in, to generate {@link Subject}s for.
     */
    @Parameter(property = "truth.packages")
    public String[] packages;

    /**
     * List of classes to generate assertions for.
     */
    @Parameter(property = "truth.classes")
    public String[] classes;

    /**
     * List of legacy style classes to generate assertions for.
     */
    @Parameter(property = "truth.classes")
    public String[] legacyClasses;

    /**
     * When generating for legacy classes (classes that don't use Get prefixes), use a getter for the generated Subject
     * methods.
     */
    @Parameter(property = "truth.classes", defaultValue = "true")
    public boolean useGetterForLegacyClasses;

    /**
     * An optional package name for the Assertions' entry point class. If omitted, the package will be determined
     * heuristically from the generated assertions.
     */
    @Parameter(property = "truth.entryPointClassPackage")
    public String entryPointClassPackage;

    // todo
//  /**
//   * Generated assertions are limited to classes matching one of the given regular expressions, default is to include
//   * all classes.
//   */
//  @Parameter(property = "truth.includes")
//  public String[] includes = INCLUDE_ALL_CLASSES;

    // todo
//  /**
//   * If class matches one of the given regex, no assertions will be generated for it, default is not to exclude
//   * anything.
//   */
//  @Parameter(property = "truth.excludes")
//  public String[] excludes = new String[0];
    /**
     * Skip generating classes, handy way to disable the plugin.
     */
    @Parameter(property = "truth.skip", defaultValue = "false")
    public boolean skip;
    /**
     * Recursively scan for referenced classes in explicitly set classes, in order to generate a complete, chainable
     * Subject graph.
     */
    @Parameter(property = "truth.recursive", defaultValue = "true")
    public boolean recursive;

    /**
     * Advanced setting - configure the release target for class reflection. Useful for when runtime env is different
     * from the execution environment - e.g. when building on a newer jdk than running on. Same as the maven compiler
     * plugin target option. In fact, we first try to copy the same release target from there, so if that's specified,
     * this shouldn't be needed.
     */
    @Parameter(property = "truth.releaseTarget")
    public Integer releaseTarget;

    /**
     * Location of the file.
     */
    @Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
    private File outputDirectory;

    /**
     * for testing
     */
    private Map<Class<?>, ThreeSystem<?>> result;

    static String shouldHaveNonEmptyPackagesOrClasses() {
        return format(
                "Parameter 'packages' or 'classes' must be set to generate assertions.%n[Help] https://github.com/joel-costigliola/assertj-assertions-generator-maven-plugin");
    }

    public void execute() {
        if (isSkip()) {
            getLog().info("INFO: Skipping Truth generation...");
            return;
        } else {
            getLog().info("INFO: Truth generator running...");
        }

        // todo smells bad
        Utils.setOutputBase(getOutputPath());

        deleteOldContent();

        this.result = runGenerator();

        addOutputPathsToBuild();

        getLog().info("Generated " + result.keySet().size() + " Subject models.");
        List<String> classes = result.keySet().stream().map(Class::getCanonicalName).sorted().collect(toList());
        for (String aClass : classes) {
            getLog().info(aClass);
        }
    }

    private Path getOutputPath() {
        String outputDir = getProject().getBuild().getDirectory();
        return Path.of(outputDir).resolve("generated-test-sources");
    }

    private void deleteOldContent() {
        if (isCleanTargetDir()) {
            Stream.of(Utils.getManagedPath(), Utils.getTemplatesPath())
                    .map(Path::toFile)
                    .forEach(FileUtils::deleteQuietly);
        }
    }

    @SneakyThrows
    private Map<Class<?>, ThreeSystem<?>> runGenerator() {

        Optional<String> entryPointClassPackage = ofNullable(this.entryPointClassPackage);

        final Build build = getProject().getBuild();
        final List<Path> testSourcePaths = List.of(Path.of(build.getTestSourceDirectory()));

        FullContext context = new FullContext(getOutputPath(),
                testSourcePaths,
                List.of(getProjectClassLoader()),
                getBaseModelPackagesForReflectionScanning());

        ReflectionUtils reflectionUtils = new ReflectionUtils(context);

        SourceClassSets ss = new SourceClassSets(getEntryPointClassPackage(), reflectionUtils);

        ss.generateFrom(getProjectClassLoader(), getClasses());
        String[] legacyClasses = getLegacyClasses();
        ss.generateFromNonBean(getProjectClassLoader(), legacyClasses);
        ss.generateAllFoundInPackages(getPackages());

        Options options = buildOptions();
        TruthGenerator tg = new TruthGenerator(options, context);
        tg.setEntryPoint(entryPointClassPackage);
        Result generate = tg.generate(ss);
        Map<Class<?>, ThreeSystem<?>> generated = generate.getAll();

        return generated;
    }

    /**
     * Combines the packages for looking for Subject targets, as well as packages for {@link UserManagedSubject} Subject
     * sources.
     */
    private Set<String> getBaseModelPackagesForReflectionScanning() {
        var subjectPackages = Arrays.asList(getSubjectPackages());

        var classSourcePackages = Arrays.asList(getPackages());

        HashSet<String> union = new HashSet<>(subjectPackages);
        union.addAll(classSourcePackages);
        if (getEntryPointClassPackage() != null)
            union.addAll(Set.of(getEntryPointClassPackage()));
        union.addAll(classSourcePackages);

        return union;
    }

    private void addOutputPathsToBuild() {
        Path managedPath = Utils.getManagedPath();
        Path templatesPath = Utils.getTemplatesPath();
        getProject().addTestCompileSourceRoot(managedPath.toAbsolutePath().toString());
        getProject().addTestCompileSourceRoot(templatesPath.toAbsolutePath().toString());
    }

    private Options buildOptions() {
        Options.OptionsBuilder optionsBuilder = Options.builder()
                .useHasInsteadOfGet(isUseHas())
                .useGetterForLegacyClasses(isUseGetterForLegacyClasses())
                .compilationTargetLowerThanNine(isCompilationTargetBelowJavaNine());

        Integer foundReleaseTarget = getReleaseTarget();
        if (foundReleaseTarget != null) {
            optionsBuilder.releaseTarget(Optional.of(foundReleaseTarget));
        }

        return optionsBuilder.build();
    }

    private ClassLoader getProjectClassLoader() throws DependencyResolutionRequiredException, MalformedURLException {
        List<String> classpathElements = new ArrayList<>(project.getCompileClasspathElements());
        classpathElements.addAll(project.getTestClasspathElements());
        List<URL> classpathElementUrls = new ArrayList<>(classpathElements.size());
        for (String classpathElement : classpathElements) {
            classpathElementUrls.add(new File(classpathElement).toURI().toURL());
        }
        return new URLClassLoader(classpathElementUrls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());
    }

    /**
     * @return defaults to false if can't find a target
     */
    private boolean isCompilationTargetBelowJavaNine() {
        Plugin compiler = getProject().getPlugin("org.apache.maven.plugins:maven-compiler-plugin");

        boolean belowJavaNine = false;

        // spec version
        try {
            String property = System.getProperty("java.specification.version");
            int javaSpecVersion = Integer.parseInt(property);
            if (javaSpecVersion < 9)
                belowJavaNine = true;
        } catch (NumberFormatException e) {
            getLog().warn("Can't parse java spec version");
        }

        // compilation target - overrides if present
        Xpp3Dom configuration = (Xpp3Dom) compiler.getConfiguration();
        if (configuration != null) {
            Xpp3Dom target = configuration.getChild("target");
            if (target != null) {
                String rawTarget = target.getValue();
                try {
                    int compilationTarget = Integer.parseInt(rawTarget);
                    belowJavaNine = compilationTarget < 9;
                } catch (NumberFormatException e) {
                    getLog().warn("Cannot parse compilation target: " + rawTarget);
                }
            }
        }

        return belowJavaNine;
    }
}
