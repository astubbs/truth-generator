package io.stubbs.truth.generator.internal.model;

import com.google.common.truth.Truth;
import io.stubbs.truth.generator.TestModelUtils;
import io.stubbs.truth.generator.testModel.AnInterface;
import org.junit.Test;

/**
 * @see ThreeSystem
 */
public class ThreeSystemTest {

    @Test
    public void equality() {
        {
            var one = AnInterface.class;
            var two = AnInterface.class;
            Truth.assertThat(one).isEqualTo(two);
        }

        {
            var one = TestModelUtils.createThreeSystem();
            var two = TestModelUtils.createThreeSystem();
            Truth.assertThat(one).isEqualTo(two);
        }
    }

}
