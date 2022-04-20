package io.stubbs.truth.generator.internal;

import io.stubbs.truth.generator.SourceClassSets;
import io.stubbs.truth.generator.TruthGeneratorAPI;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.stubbs.truth.generator.internal.TruthGeneratorTest.TEST_OUTPUT_DIRECTORY;

/**
 * @see SourceClassSets
 */
public class SourceClassSetsTests {

    /**
     * Where there are duplicate entries of classes in the target sets
     */
    // todo use bootstrapped ResultSubject
    @Test
    public void duplicates() {
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

}
