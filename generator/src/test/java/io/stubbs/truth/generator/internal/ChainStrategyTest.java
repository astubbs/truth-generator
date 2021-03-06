package io.stubbs.truth.generator.internal;

import com.google.common.truth.Truth8;
import io.stubbs.truth.generator.TestModelUtils;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import io.stubbs.truth.generator.internal.modelSubjectChickens.ThreeSystemChildSubject;
import io.stubbs.truth.generator.testModel.MyEmployee;
import io.stubbs.truth.generator.testModel.Project;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author Antony Stubbs
 * @see ChainStrategy
 */
public class ChainStrategyTest extends StrategyTest {

    ChainStrategy strat = createNewChainStrategy();

    @Test
    public void chain() {
        Options.setDefaultInstance();

        ChainStrategy chainStrategy = createNewChainStrategy();

        var threeSystem = TestModelUtils.createThreeSystem();

        //
        Method theMethod = getMethod(employeeClass, "getWeighting");

        //
        var added = chainStrategy.addChainStrategy(threeSystem, theMethod, generated);

        //
        assertThat(added.toString()).contains("hasWeightingPresent();");
        assertThat(added.toString()).contains("check(\"getWeighting().get()\")");
        assertThat(added.toString()).contains("getWeighting().get()");
    }

    private ChainStrategy createNewChainStrategy() {
        return new ChainStrategy(new GeneratedSubjectTypeStore(builtInSubjectTypeStore));
    }

    private Method getMethod(Class<?> employeeClass, String contains) {
        return Arrays.stream(employeeClass.getMethods()).filter(x -> x.getName().contains(contains)).findFirst().get();
    }

    /**
     * Shouldn't break, but also has some interesting test cases
     */
    @Test
    public void testOptionalItself() {
        Arrays.stream(Optional.class.getMethods())
                .filter(x -> StringUtils.startsWith(x.getName(), "get"))
                .forEach(method ->
                        strat.addStrategyMaybe(createThreeSystem(Optional.class), method, generated)
                );
    }

    @Test
    public void testGenericsMissing() {
        Arrays.stream(BadGenerics.class.getMethods())
                .filter(x -> StringUtils.startsWith(x.getName(), "get"))
                .forEach(method ->
                        strat.addStrategyMaybe(createThreeSystem(BadGenerics.class), method, generated)
                );
    }

    @Test
    public void legacyGetterStyle() {
        ThreeSystem threeSystem = createThreeSystem(MyEmployee.class);
        threeSystem.setLegacyMode(true);

        Method method = getMethod(employeeClass, "legacyAccessMethod");

        Options.setInstance(Options.builder()
                .useHasInsteadOfGet(false)
                .useGetterForLegacyClasses(true)
                .build());

        strat = createNewChainStrategy();

        {
            var source = strat.addChainStrategy(threeSystem, method, generated);
            assertThat(source.toString()).contains("IntegerSubject getLegacyAccessMethod(){");
        }

        Options.setInstance(Options.builder()
                .useHasInsteadOfGet(true)
                .useGetterForLegacyClasses(true)
                .build());

        strat = createNewChainStrategy();
        {
            var source = strat.addChainStrategy(threeSystem, method, generated);
            assertThat(source.toString()).contains("IntegerSubject hasLegacyAccessMethod(){");
        }

        Options.setInstance(Options.builder()
                .useHasInsteadOfGet(true)
                .useGetterForLegacyClasses(false)
                .build());

        strat = createNewChainStrategy();
        {
            var source = strat.addChainStrategy(threeSystem, method, generated);
            assertThat(source.toString()).contains("IntegerSubject legacyAccessMethod(){");
        }
    }

    @Test
    public void useHas() {
        ThreeSystem threeSystem = createThreeSystem(MyEmployee.class);
        threeSystem.setLegacyMode(false);

        Method method = getMethod(employeeClass, "getWorkNickName");

        //
        Options.setInstance(Options.builder().useHasInsteadOfGet(false).build());
        strat = createNewChainStrategy();

        {
            var source = strat.addChainStrategy(threeSystem, method, generated);
            assertThat(source.toString()).contains("StringSubject getWorkNickName(){");
        }

        //
        Options.setInstance(Options.builder().useHasInsteadOfGet(true).build());
        strat = createNewChainStrategy();

        {
            var source = strat.addChainStrategy(threeSystem, method, generated);
            assertThat(source.toString()).contains("StringSubject hasWorkNickName(){");
        }
    }

    @Test
    public void optionalChainObject() {
        var generatedSystem = Set.of(createThreeSystem(Instant.class));
        GeneratedSubjectTypeStore subjects = new GeneratedSubjectTypeStore(generatedSystem, builtInSubjectTypeStore);

        String needleExpected = "InstantSubject getStartedAt(){";

        {
            // test subject store directly
            Optional<SubjectMethodGenerator.ClassOrGenerated> subjectForType = subjects.getSubjectForType(Instant.class);
            Truth8.assertThat(subjectForType).isPresent();
            SubjectMethodGenerator.ClassOrGenerated classOrGenerated = subjectForType.get();
            assertThat(classOrGenerated.isGenerated()).isTrue();
            var generated = classOrGenerated.getGenerated();
            ThreeSystemChildSubject.assertThat(generated).hasClassUnderTest().isAssignableTo(Instant.class);
        }

        strat = new ChainStrategy(subjects);

        var threeSystem = createThreeSystem(MyEmployee.class);
        Method method = getMethod(employeeClass, "getStartedAt");

        var source = strat.addChainStrategy(threeSystem, method, this.generated);

        assertThat(source.toString()).contains(needleExpected);
        assertThat(source.toString()).contains("that((Instant)actual.getStartedAt().get()");
    }

    /**
     * A test input class which declares a field of generic type, but is missing its generics parameters
     */
    @Data
    public static class BadGenerics {
        @SuppressWarnings("rawtypes")
        Optional genericsMissing;
    }

    @Test
    public void chainSubtype() {
        MyEmployee employee = TestModelUtils.createEmployee();
        List<Project> projectList = employee.getProjectList();

        var threeSystem = createThreeSystem(MyEmployee.class);
        GeneratedSubjectTypeStore subjects = new GeneratedSubjectTypeStore(Set.of(threeSystem), builtInSubjectTypeStore);

        strat = new ChainStrategy(subjects);

        Method method = getMethod(employeeClass, "getProjectList");

        var source = strat.addChainStrategy(threeSystem, method, this.generated).toString();

        assertThat(source).contains("about(collections()).that");
    }

}
