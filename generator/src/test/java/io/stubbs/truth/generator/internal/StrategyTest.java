package io.stubbs.truth.generator.internal;

import io.stubbs.truth.generator.TestModelUtils;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import io.stubbs.truth.generator.testModel.MyEmployee;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;

import java.util.Optional;

/**
 * @author Antony Stubbs
 */
public abstract class StrategyTest {

    static {
        Options.setDefaultInstance();
        Utils.setOutputBase(TruthGeneratorTest.testOutputDirectory);
    }

    JavaClassSource generated = Roaster.create(JavaClassSource.class);
    MyEmployee employee = TestModelUtils.createEmployee();
    Class<? extends MyEmployee> employeeClass = employee.getClass();
    BuiltInSubjectTypeStore builtInSubjectTypeStore = new BuiltInSubjectTypeStore();

    protected <T> ThreeSystem<T> createThreeSystem(Class<T> clazzUnderTest) {
        SkeletonGenerator skeletonGenerator = new SkeletonGenerator(Optional.empty(), new OverallEntryPoint(getClass().getPackageName()), builtInSubjectTypeStore);
        Optional<ThreeSystem<T>> instantThreeSystem = skeletonGenerator.threeLayerSystem(clazzUnderTest);
        return instantThreeSystem.get();
    }

}
