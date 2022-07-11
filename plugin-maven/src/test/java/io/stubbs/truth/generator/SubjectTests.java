package io.stubbs.truth.generator;

import io.stubbs.truth.generator.plugin.GeneratorMojo;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author Antony Stubbs
 */
public class SubjectTests {

    // todo not used? is this the actual one?
    public static final Path testOutputDirectory = Paths.get("").resolve("target").resolve("generated-test-sources").toAbsolutePath();

    @Test
    public void makeSubjects() {
        TruthGeneratorAPI tg = TestClassFactories.newTruthGenerator();
        SourceClassSets ss = TestClassFactories.newSourceClassSets();
        ss.generateFromShaded(File.class);
        ss.generateFrom(GeneratorMojo.class);
        var generate = tg.generate(ss);
        assertThat(generate).isNotNull();
    }

}
