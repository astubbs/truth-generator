package io.stubbs.truth.generator.internal;

import io.stubbs.truth.generator.TestModelUtils;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static com.google.common.truth.Truth.assertThat;

class BooleanStrategyTest extends StrategyTest {

    BooleanStrategy strat = new BooleanStrategy();

    @Test
    void generatesPrimitiveWrapperOK() {
        boolean shouldBeGivenAPromotionNextCycle = myEmployee.isDueToBeGivenAPromotionNextCycle();

        Method isEmployedWrapped = TestModelUtils.findMethodWithNoParamsJReflect(employeeClass, "isDueToBeGivenAPromotionNextCycle");
        String noun = strat.buildNoun(isEmployedWrapped, false);
        //         check("isAllowedMoreRecords() 'is allowed more records'").that(actual.isAllowedMoreRecords()).isTrue();
        assertThat(noun).contains("'due to be given a promotion next cycle' (`isDueToBeGivenAPromotionNextCycle`)");
    }

}
