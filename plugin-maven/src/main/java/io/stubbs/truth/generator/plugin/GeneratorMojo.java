package io.stubbs.truth.generator.plugin;

import io.stubbs.truth.generator.SourceClassSets;
import io.stubbs.truth.generator.TruthGeneratorAPI;
import io.stubbs.truth.generator.internal.TruthGenerator;
import io.stubbs.truth.generator.internal.Utils;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

  // todo?
//  /**
//   * Flag specifying whether to clean the directory where assertions are generated. The default is false.
//   */
//  @Parameter(defaultValue = "false", property = "truth.cleanTargetDir")
//  public boolean cleanTargetDir;

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

  // todo
//  /**
//   * Skip generating classes, handy way to disable the plugin.
//   */
//  @Parameter(property = "truth.skip")
//  public boolean skip = false;


  // todo
//  @Parameter(property = "truth.recursive")
//  public boolean recursive = true;

  /**
   * for testing
   */
  private Map<Class<?>, ThreeSystem> result;

  static String shouldHaveNonEmptyPackagesOrClasses() {
    return format(
            "Parameter 'packages' or 'classes' must be set to generate assertions.%n[Help] https://github.com/joel-costigliola/assertj-assertions-generator-maven-plugin");
  }

  public void execute() throws MojoExecutionException {
    getLog().info("INFO: Truth generator running...");

    this.result = runGenerator();

    addOutputPathsToBuild();

    getLog().info("Generated " + result.keySet().size() + " Subject models.");
    List<String> classes = result.keySet().stream().map(Class::getCanonicalName).sorted().collect(toList());
    for (String aClass : classes) {
      getLog().info(aClass);
    }
  }

  private void addOutputPathsToBuild() {
    String outputDirectory = getProject().getBuild().getOutputDirectory();
    Path managedPath = Paths.get(outputDirectory, Utils.DIR_TRUTH_ASSERTIONS_MANAGED);
    Path templatesPath = Paths.get(outputDirectory, Utils.DIR_TRUTH_ASSERTIONS_TEMPLATES);
    getProject().addTestCompileSourceRoot(managedPath.toAbsolutePath().toString());
    getProject().addTestCompileSourceRoot(templatesPath.toAbsolutePath().toString());
  }

  @SneakyThrows
  private Map<Class<?>, ThreeSystem> runGenerator() {
    String outputDir = getProject().getBuild().getDirectory();
    TruthGenerator tg = TruthGeneratorAPI.create(Path.of(outputDir));

    Optional<String> entryPointClassPackage = ofNullable(this.entryPointClassPackage);
    tg.setEntryPoint(entryPointClassPackage);

    SourceClassSets ss = new SourceClassSets(getEntryPointClassPackage());

    ClassLoader projectClassLoader = getProjectClassLoader();
    ss.addClassLoader(projectClassLoader);

    ss.generateFrom(projectClassLoader, getClasses());
    String[] legacyClasses = getLegacyClasses();
    ss.generateFromNonBean(projectClassLoader, legacyClasses);
    ss.generateAllFoundInPackages(getPackages());

    Map<Class<?>, ThreeSystem> generated = tg.generate(ss);

    return generated;
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
