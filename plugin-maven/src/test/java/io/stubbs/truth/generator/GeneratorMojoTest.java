package io.stubbs.truth.generator;

import io.stubbs.truth.generator.internal.model.ThreeSystem;
import io.stubbs.truth.generator.plugin.GeneratorMojo;
import io.stubbs.truth.generator.testModel.MyEmployee;
import lombok.SneakyThrows;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.WithoutMojo;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.truth.Truth.assertThat;
import static io.stubbs.truth.generator.shaded.java.io.FileChildSubject.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GeneratorMojoTest {

  @Rule
  public MojoRule rule = new MojoRule() {
    @Override
    protected void before() throws Throwable {
    }

    @Override
    protected void after() {
    }
  };

  File projectToTestBaseDir = new File("target/test-classes/project-to-test/");

  /**
   * This is the entryPointClassPackage set in the pom file setting for the plugin
   */
  File entryPointPackage = new File ("io.stubbs.truth.tests.projectUnderTest");

  @Test
  public void overall() throws Exception {
    assertNotNull(projectToTestBaseDir);
    assertTrue(projectToTestBaseDir.exists());

    Path generatorBaseDir = Paths.get(projectToTestBaseDir.toString(), "target/generated-test-sources/truth-assertions-managed/");


    // instantiation
    GeneratorMojo generatorMojo = (GeneratorMojo) rule.lookupConfiguredMojo(projectToTestBaseDir, "generate");
    assertNotNull(generatorMojo);

    List<Plugin> plugins = generatorMojo.getProject().getBuildPlugins();
    assertThat(plugins.stream().map(x->x.getKey()).collect(Collectors.toList())).contains("io.stubbs.truth:truth-generator-maven-plugin");

    //
    assertThat(generatorMojo.getClasses()).asList()
            .contains("java.io.File");

    // execution
    generatorMojo.execute();

    //
    Map<Class<?>, ThreeSystem> results = generatorMojo.getResult();
    assertThat(results).containsKey(MyEmployee.class);
    assertThat(results).containsKey(File.class);

    assertThat(Paths.get(generatorBaseDir.toString(), "io/stubbs/truth/generator/testModel/MyEmployeeParentSubject.java").toFile()).exists();
    Path dir = Paths.get(generatorBaseDir.toString(), "io/stubbs/truth/generator/testModel/");
    assertThat(dir.resolve("MyEmployeeParentSubject.java").toFile()).exists();

    String entryPointDir = entryPointPackage.toString().replaceAll("\\.", String.valueOf(File.separatorChar));
    Path baseEntryPoint = Paths.get(generatorBaseDir.toString(), entryPointDir);
    Path resolve = baseEntryPoint.resolve("ManagedTruth.java");
    assertThat(resolve.toFile()).exists();
  }

  /**
   * Do not need the MojoRule.
   */
  @WithoutMojo
  @Test
  public void testSomethingWhichDoesNotNeedTheMojoAndProbablyShouldBeExtractedIntoANewClassOfItsOwn() {
    assertTrue(true);
  }

  @SneakyThrows
  @Test
  public void nullEntryPointClass(){
    GeneratorMojo generatorMojo = (GeneratorMojo) rule.lookupConfiguredMojo(projectToTestBaseDir, "generate");
    generatorMojo.entryPointClassPackage = null;

    assertThatThrownBy(() -> generatorMojo.execute())
        .isExactlyInstanceOf(GeneratorException.class)
        .hasMessageContainingAll("managed", "entrypoint", "blank");
  }

}

