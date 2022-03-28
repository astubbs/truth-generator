package io.stubbs.truth.generator.internal;

import io.stubbs.truth.generator.testModel.MyEmployee;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static com.google.common.truth.Truth.assertThat;

public class OptionalStrategyTest extends StrategyTest {

    OptionalStrategy strat = new OptionalStrategy();

    /**
     * Doesn't assert anything as was aborted test for optional chaining
     */
    @Test
    public void generatesPrimitiveWrapperOK() {
        Method theMethod = Arrays.stream(employeeClass.getMethods()).filter(x -> x.getName().contains("getWeighting")).findFirst().get();

        boolean added = strat.addStrategyMaybe(createThreeSystem(MyEmployee.class), theMethod, generated);

        assertThat(added).isTrue();

        assertThat(generated.toString()).contains("void hasWeightingNotPresent() {");
    }

    @Test
    public void generatesInstantWrapperOK() {
        Method theMethod = Arrays.stream(employeeClass.getMethods()).filter(x -> x.getName().contains("getStartedAt")).findFirst().get();

        boolean added = strat.addStrategyMaybe(createThreeSystem(MyEmployee.class), theMethod, generated);

        assertThat(added).isTrue();

        assertThat(generated.toString()).contains("public void hasStartedAtPresent() {");
    }

}
