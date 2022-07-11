package io.stubbs.truth.generator.internal;

import io.stubbs.truth.generator.SourceClassSets;
import io.stubbs.truth.generator.TestClassFactories;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import io.stubbs.truth.generator.testModel.MyEmployee;
import org.junit.Test;

import java.time.Instant;

import static io.stubbs.truth.generator.internal.modelSubjectChickens.ThreeSystemChildSubject.assertThat;

/**
 * @author Antony Stubbs
 * @see SkeletonGenerator
 */
public class SkeletonGeneratorTest {

    @Test
    public void dynamicExtensionsDirect() {
        TruthGenerator tg = TestClassFactories.newTruthGenerator();
        SourceClassSets ss = TestClassFactories.newSourceClassSets();
        ss.generateFrom(Instant.class);
        var generate = tg.generate(ss).getAll();
        ThreeSystem<?> threeSystem = generate.get(Instant.class);
        assertThat(threeSystem).hasParent().hasGenerated().hasSourceText().contains("InstantParentSubject extends ComparableSubject");
    }

    @Test
    public void dynamicExtensionIndirect() {
        TruthGenerator tg = TestClassFactories.newTruthGenerator();
        SourceClassSets ss = TestClassFactories.newSourceClassSets();
        ss.generateFrom(MyEmployee.class);
        var generate = tg.generate(ss).getAll();

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
