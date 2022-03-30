package io.stubbs.truth.generator.internal;

import io.stubbs.truth.generator.SourceClassSets;
import io.stubbs.truth.generator.TruthGeneratorAPI;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import io.stubbs.truth.generator.testModel.MyEmployee;
import org.junit.Test;

import java.time.Instant;

import static io.stubbs.truth.generator.internal.TruthGeneratorTest.testOutputDirectory;
import static io.stubbs.truth.generator.internal.modelSubjectChickens.ThreeSystemChildSubject.assertThat;

/**
 * @author Antony Stubbs
 * @see SkeletonGenerator
 */
public class SkeletonGeneratorTest {

    @Test
    public void dynamicExtensionsDirect() {
        TruthGenerator tg = TruthGeneratorAPI.createDefaultOptions(testOutputDirectory);
        SourceClassSets ss = new SourceClassSets(getClass().getPackage().getName());
        ss.generateFrom(Instant.class);
        var generate = tg.generate(ss);
        ThreeSystem<?> threeSystem = generate.get(Instant.class);
        assertThat(threeSystem).hasParent().hasGenerated().hasSourceText().contains("InstantParentSubject extends ComparableSubject");
    }

    @Test
    public void dynamicExtensionIndirect() {
        TruthGenerator tg = TruthGeneratorAPI.createDefaultOptions(testOutputDirectory);
        SourceClassSets ss = new SourceClassSets(getClass().getPackage().getName());
        ss.generateFrom(MyEmployee.class);
        var generate = tg.generate(ss);

        {
            var threeSystem = generate.get(MyEmployee.class);
            assertThat(threeSystem).hasParent().hasGenerated().hasSourceText().contains("public InstantSubject getStartedAt()");
        }
        {
            var threeSystem = generate.get(Instant.class);
            assertThat(threeSystem).hasParent().hasGenerated().hasSourceText().contains("InstantParentSubject extends ComparableSubject");
        }

    }

}
