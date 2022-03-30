package io.stubbs.truth.generator;

import io.stubbs.truth.generator.plugin.GeneratorMojo;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Antony Stubbs
 */
public class SubjectTests {

  public static final Path testOutputDirectory = Paths.get("").resolve("target").resolve("generated-test-sources").toAbsolutePath();

  @Test
  public void makeSubjects() {
    TruthGeneratorAPI tg = TruthGeneratorAPI.createDefaultOptions(testOutputDirectory);
    SourceClassSets ss = new SourceClassSets(getClass());
    ss.generateFromShaded(File.class);
    ss.generateFrom(GeneratorMojo.class);
    tg.generate(ss);
  }

}
