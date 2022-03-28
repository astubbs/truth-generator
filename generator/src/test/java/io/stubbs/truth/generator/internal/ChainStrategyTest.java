package io.stubbs.truth.generator.internal;

import io.stubbs.truth.generator.TestModelUtils;
import io.stubbs.truth.generator.internal.model.MiddleClass;
import io.stubbs.truth.generator.internal.model.ParentClass;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import io.stubbs.truth.generator.testModel.MyEmployee;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;

public class ChainStrategyTest extends StrategyTest {

    ChainStrategy strat = new ChainStrategy(Set.of(), Map.of());

    @Test
    public void chain() {
        Options.setDefaultInstance();

        ChainStrategy chainStrategy = new ChainStrategy(Set.of(), Map.of());

        JavaClassSource generated = Roaster.create(JavaClassSource.class);

        MyEmployee employee = TestModelUtils.createEmployee();
        Class<? extends MyEmployee> employeeClass = employee.getClass();
        ThreeSystem threeSystem = new ThreeSystem(employeeClass, new ParentClass(Roaster.create(JavaClassSource.class)), MiddleClass.of(employeeClass), generated);

        //
        Method theMethod = getMethod(employeeClass, "getWeighting");

        //
        var added = chainStrategy.addChainStrategy(threeSystem, theMethod, generated, theMethod.getReturnType());

        //
        assertThat(added.toString()).contains("hasWeightingPresent();");
        assertThat(added.toString()).contains("check(\"getWeighting().get()\")");
        assertThat(added.toString()).contains("getWeighting().get()");
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

    @Data
    static
    class BadGenerics {
        @SuppressWarnings("rawtypes")
        Optional genericsMissing;
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

        strat = new ChainStrategy(Set.of(), Map.of());

        {
            var source = strat.addChainStrategy(threeSystem, method, generated, method.getReturnType());
            assertThat(source.toString()).contains("IntegerSubject getLegacyAccessMethod(){");
        }

        Options.setInstance(Options.builder()
                .useHasInsteadOfGet(true)
                .useGetterForLegacyClasses(true)
                .build());

        strat = new ChainStrategy(Set.of(), Map.of());
        {
            var source = strat.addChainStrategy(threeSystem, method, generated, method.getReturnType());
            assertThat(source.toString()).contains("IntegerSubject hasLegacyAccessMethod(){");
        }

        Options.setInstance(Options.builder()
                .useHasInsteadOfGet(true)
                .useGetterForLegacyClasses(false)
                .build());

        strat = new ChainStrategy(Set.of(), Map.of());
        {
            var source = strat.addChainStrategy(threeSystem, method, generated, method.getReturnType());
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
        strat = new ChainStrategy(Set.of(), Map.of());

        {
            var source = strat.addChainStrategy(threeSystem, method, generated, method.getReturnType());
            assertThat(source.toString()).contains("StringSubject getWorkNickName(){");
        }

        //
        Options.setInstance(Options.builder().useHasInsteadOfGet(true).build());
        strat = new ChainStrategy(Set.of(), Map.of());

        {
            var source = strat.addChainStrategy(threeSystem, method, generated, method.getReturnType());
            assertThat(source.toString()).contains("StringSubject hasWorkNickName(){");
        }
    }

}
