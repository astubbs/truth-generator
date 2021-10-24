package io.stubbs.truth.generator;


import io.stubbs.truth.generator.internal.model.ThreeSystem;
import io.stubbs.truth.generator.plugin.GeneratorMojo;
import io.stubbs.truth.generator.testModel.MyEmployee;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.WithoutMojo;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
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

  /**
   * @throws Exception if any
   */
  @Test
  public void testSomething() throws Exception {
    File pomBaseDir = new File("target/test-classes/project-to-test/");
    assertNotNull(pomBaseDir);
    assertTrue(pomBaseDir.exists());

    // instantiation
    GeneratorMojo generatorMojo = (GeneratorMojo) rule.lookupConfiguredMojo(pomBaseDir, "generate");
    assertNotNull(generatorMojo);

    //
    assertThat(generatorMojo.getClasses()).asList()
            .contains("java.io.File");

    // execution
    generatorMojo.execute();

    //
    Map<Class<?>, ThreeSystem> results = generatorMojo.getResult();
    assertThat(results).containsKey(MyEmployee.class);
    assertThat(results).containsKey(File.class);

//    File outputDirectory = (File) rule.getVariableValueFromObject(generatorMojo, "outputDirectory");
//    assertThat(outputDirectory).exists();
//
//    File touch = new File(outputDirectory, "touch.txt");
//    assertThat(touch).exists();

//    String dir = "target/generated-test-sources/truth-assertions-managed/com/google/common/truth/extensions/generator/testModel";
//    File subject = new File(dir, "MyEmployeeParentSubject.java");
//    assertThat(subject).exists();
  }

  /**
   * Do not need the MojoRule.
   */
  @WithoutMojo
  @Test
  public void testSomethingWhichDoesNotNeedTheMojoAndProbablyShouldBeExtractedIntoANewClassOfItsOwn() {
    assertTrue(true);
  }

}

