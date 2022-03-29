package io.stubbs.truth.generator.plugin;

import io.stubbs.truth.generator.SourceClassSets;
import io.stubbs.truth.generator.TruthGeneratorAPI;
import io.stubbs.truth.generator.internal.Options;
import io.stubbs.truth.generator.internal.TruthGenerator;
import io.stubbs.truth.generator.internal.Utils;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;


/**
 * Goal which touches a timestamp file.
 */
@Getter
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES, requiresDependencyResolution = TEST, requiresProject = true)
public class GeneratorMojo extends AbstractMojo {

  private static final String[] INCLUDE_ALL_CLASSES = { ".*" };

  /**
   * Current maven project
   */
  @Parameter(property = "project", required = true, readonly = true)
  public MavenProject project;

  /**
   * Location of the file.
   */
  @Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
  private File outputDirectory;

  /**
   * Package where generated assertion classes will reside.
   * <p/>
   * If not set (or set to empty), each assertion class is generated in the package of the corresponding class to assert.
   * For example the generated assertion class for com.nba.Player will be com.nba.PlayerAssert (in the same package as Player).
   * Defaults to ''.<br>
   * <p/>
   * Note that the Assertions entry point classes package is controlled by the entryPointClassPackage property.
   */
  @Parameter(defaultValue = "", property = "truth.generateAssertionsInPackage")
  public String generateAssertionsInPackage;

  /**
   * Flag specifying whether to clean the directory where assertions are generated. The default is false.
   */
  @Parameter(defaultValue = "false", property = "truth.cleanTargetDir")
  public boolean cleanTargetDir;

  @Parameter(defaultValue = "false", property = "truth.useHas")
  public boolean useHas;

  /**
   * List of packages to generate assertions for.
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
   * When generatoring for legacy classes (classes that don't use Get prefxes), use a getter for the generated Subject methods.
   */
  @Parameter(property = "truth.classes", defaultValue = "true")
  public boolean useGetterForLegacyClasses;

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
   * An optional package name for the Assertions' entry point class. If omitted, the package will be determined
   * heuristically from the generated assertions.
   */
  @Parameter(property = "truth.entryPointClassPackage")
  public String entryPointClassPackage;

  /**
  * Skip generating classes, handy way to disable the plugin.
  */
  @Parameter(property = "truth.skip", defaultValue = "false")
  public boolean skip;

  @Parameter(property = "truth.recursive", defaultValue = "true")
  public boolean recursive;

  /**
   * for testing
   */
  private Map<Class<?>, ThreeSystem<?>> result;

  static String shouldHaveNonEmptyPackagesOrClasses() {
    return format(
            "Parameter 'packages' or 'classes' must be set to generate assertions.%n[Help] https://github.com/joel-costigliola/assertj-assertions-generator-maven-plugin");
  }

  public void execute() throws MojoExecutionException {
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

  private boolean isCompilationTargetBelowJavaNine() {
    Plugin compiler = getProject().getPlugin("org.apache.maven.plugins:maven-compiler-plugin");
    Xpp3Dom configuration = (Xpp3Dom) compiler.getConfiguration();
    String rawTarget = configuration.getChild("target").getValue();
    try {
      int compilationTarget = Integer.parseInt(rawTarget);
      return compilationTarget < 9;
    } catch (NumberFormatException e) {
      getLog().warn("Cannot parse compilation target: " + rawTarget);
      return false;
    }
  }

  private void deleteOldContent() {
    if (isCleanTargetDir()) {
      Stream.of(Utils.getManagedPath(), Utils.getTemplatesPath())
              .map(Path::toFile)
              .forEach(FileUtils::deleteQuietly);
    }
  }

  private void addOutputPathsToBuild() {
    Path managedPath = Utils.getManagedPath();
    Path templatesPath = Utils.getTemplatesPath();
    getProject().addTestCompileSourceRoot(managedPath.toAbsolutePath().toString());
    getProject().addTestCompileSourceRoot(templatesPath.toAbsolutePath().toString());
  }

  @SneakyThrows
  private Map<Class<?>, ThreeSystem<?>> runGenerator() {
    Options options = buildOptions();
    TruthGenerator tg = TruthGeneratorAPI.create(getOutputPath(), options);

    Optional<String> entryPointClassPackage = ofNullable(this.entryPointClassPackage);
    tg.setEntryPoint(entryPointClassPackage);

    SourceClassSets ss = new SourceClassSets(getEntryPointClassPackage());

    ClassLoader projectClassLoader = getProjectClassLoader();
    ss.addClassLoader(projectClassLoader);

    ss.generateFrom(projectClassLoader, getClasses());
    String[] legacyClasses = getLegacyClasses();
    ss.generateFromNonBean(projectClassLoader, legacyClasses);
    ss.generateAllFoundInPackages(getPackages());

    Map<Class<?>, ThreeSystem<?>> generated = tg.generate(ss);

    return generated;
  }

  private Options buildOptions() {
    return Options.builder()
            .useHasInsteadOfGet(isUseHas())
            .useGetterForLegacyClasses(isUseGetterForLegacyClasses())
            .compilationTargetLowerThanNine(isCompilationTargetBelowJavaNine())
            .build();
  }

  private Path getOutputPath() {
    String outputDir = getProject().getBuild().getDirectory();
    return Path.of(outputDir).resolve("generated-test-sources");
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
}
