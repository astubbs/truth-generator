package io.stubbs.truth.generator.internal;

import io.stubbs.truth.generator.SourceClassSets;
import io.stubbs.truth.generator.TestClassFactories;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * Where there are duplicate entries of classes in the target sets
 *
 * @see SourceClassSets
 */
// todo use bootstrapped ResultSubject
public class SourceClassSetsTests {

    @Test
    public void duplicatesClassInSpecifiedPackage() {
        TruthGenerator tg = TestClassFactories.newTruthGenerator();
        SourceClassSets ss = TestClassFactories.newSourceClassSets();

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
        TruthGenerator tg = TestClassFactories.newTruthGenerator();
        SourceClassSets ss = TestClassFactories.newSourceClassSets();

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
