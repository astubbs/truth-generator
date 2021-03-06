package io.stubbs.truth.generator.internal;

import io.stubbs.truth.generator.TestClassFactories;
import io.stubbs.truth.generator.TestModelUtils;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import io.stubbs.truth.generator.testModel.MyEmployee;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;

import java.util.Optional;

import static io.stubbs.truth.generator.TestClassFactories.TEST_OUTPUT_DIRECTORY;
import static io.stubbs.truth.generator.TestClassFactories.newScanner;

/**
 * Base setup for assertion generation strategies
 *
 * @author Antony Stubbs
 * @see BooleanStrategyTest etc...
 */
public abstract class StrategyTest {

    static {
        Options.setDefaultInstance();
        Utils.setOutputBase(TEST_OUTPUT_DIRECTORY);
    }

    JavaClassSource generated = Roaster.create(JavaClassSource.class);
    MyEmployee myEmployee = TestModelUtils.createEmployee();
    Class<? extends MyEmployee> employeeClass = myEmployee.getClass();
    BuiltInSubjectTypeStore builtInSubjectTypeStore = TestClassFactories.newBuiltInSubjectTypeStore();

    protected <T> ThreeSystem<T> createThreeSystem(Class<T> clazzUnderTest) {
        final OverallEntryPoint overallEntryPoint = new OverallEntryPoint(getDefaultPackage(), builtInSubjectTypeStore);
        SkeletonGenerator skeletonGenerator = new SkeletonGenerator(Optional.empty(),
                overallEntryPoint,
                builtInSubjectTypeStore,
                new ReflectionUtils(TestClassFactories.newReflectionContext()),
                newScanner()
        );
        Optional<ThreeSystem<T>> instantThreeSystem = skeletonGenerator.threeLayerSystem(clazzUnderTest);
        return instantThreeSystem.get();
    }

    private String getDefaultPackage() {
        return getClass().getPackageName();
    }

}
