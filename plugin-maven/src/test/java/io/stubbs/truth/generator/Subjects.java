package io.stubbs.truth.generator;

import io.stubbs.truth.generator.plugin.GeneratorMojo;
import org.junit.Test;

import java.io.File;

public class Subjects {

  @Test
  public void makeSubjects() {
    TruthGeneratorAPI tg = TruthGeneratorAPI.create();
    SourceClassSets ss = new SourceClassSets(getClass());
    ss.generateFromShaded(File.class);
    ss.generateFrom(GeneratorMojo.class);
    tg.generate(ss);
  }

}
