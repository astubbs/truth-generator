package io.stubbs.truth.generator.internal;

import io.stubbs.truth.generator.SourceClassSets;
import io.stubbs.truth.generator.TruthGeneratorAPI;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.stubbs.truth.generator.internal.TruthGeneratorGeneratedSourceTest.TEST_OUTPUT_DIRECTORY;

/**
 * Where there are duplicate entries of classes in the target sets
 *
 * @see SourceClassSets
 */
// todo use bootstrapped ResultSubject
public class SourceClassSetsTests {

    @Test
    public void duplicatesClassInSpecifiedPackage() {
        TruthGenerator tg = TruthGeneratorAPI.createDefaultOptions(TEST_OUTPUT_DIRECTORY);
        SourceClassSets ss = new SourceClassSets(SourceClassSets.class);

        // the issue
        ss.generateFrom(ThreeSystem.class);
        ss.generateAllFoundInPackagesOf(ThreeSystem.class);

        // the test
        var allGeneratedSystems = tg.generate(ss).getAll();

        // will have crashed already if the fix didn't work
        assertThat(allGeneratedSystems).containsKey(ThreeSystem.class);
    }

    @Test
    public void duplicatesPackageAndSubPackage() {
        TruthGenerator tg = TruthGeneratorAPI.createDefaultOptions(TEST_OUTPUT_DIRECTORY);
        SourceClassSets ss = new SourceClassSets(SourceClassSets.class);

        // the issue
        ss.generateFrom(ThreeSystem.class);
        ss.generateAllFoundInPackages("io.stubbs.truth.generator",
                "io.stubbs.truth.generator.subjects");

        // the test
        var allGeneratedSystems = tg.generate(ss).getAll();

        // will have crashed already if the fix didn't work
        assertThat(allGeneratedSystems).containsKey(ThreeSystem.class);
    }


}
