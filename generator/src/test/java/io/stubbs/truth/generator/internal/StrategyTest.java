package io.stubbs.truth.generator.internal;

import io.stubbs.truth.generator.TestClassFactories;
import io.stubbs.truth.generator.TestModelUtils;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import io.stubbs.truth.generator.testModel.MyEmployee;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;

import java.util.Optional;

import static io.stubbs.truth.generator.TestClassFactories.TEST_OUTPUT_DIRECTORY;

/**
 * @author Antony Stubbs
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
//
//    {
//        ReflectionContext context = new ReflectionContext(Set.of(getDefaultPackage()));
//        builtInSubjectTypeStore = TestClassFactories.
//    }

    protected <T> ThreeSystem<T> createThreeSystem(Class<T> clazzUnderTest) {
//        BuiltInSubjectTypeStore subjectTypeStore =
        final OverallEntryPoint overallEntryPoint = new OverallEntryPoint(getDefaultPackage(), builtInSubjectTypeStore);
        SkeletonGenerator skeletonGenerator = new SkeletonGenerator(Optional.empty(),
                overallEntryPoint,
                builtInSubjectTypeStore,
                new ReflectionUtils(TestClassFactories.newReflectionContext())
        );
        Optional<ThreeSystem<T>> instantThreeSystem = skeletonGenerator.threeLayerSystem(clazzUnderTest);
        return instantThreeSystem.get();
    }

    private String getDefaultPackage() {
        return getClass().getPackageName();
    }

}
