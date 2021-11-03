package io.stubbs.truth.generator;

import io.stubbs.truth.generator.plugin.GeneratorMojo;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;

public class Subjects {

  @Test
  public void makeSubjects() {
    TruthGeneratorAPI tg = TruthGeneratorAPI.create(Paths.get("").toAbsolutePath());
    SourceClassSets ss = new SourceClassSets(getClass());
    ss.generateFromShaded(File.class);
    ss.generateFrom(GeneratorMojo.class);
    tg.generate(ss);
  }

}
