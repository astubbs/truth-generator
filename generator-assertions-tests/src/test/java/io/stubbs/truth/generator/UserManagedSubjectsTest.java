package io.stubbs.truth.generator;

import com.google.common.truth.Truth;
import io.stubbs.truth.generator.capturedtemplates.MyIdCardSubject;
import io.stubbs.truth.generator.testModel.IdCard;
import io.stubbs.truth.tests.ManagedTruth;
import org.junit.Test;

/**
 * Example capture of a generated template
 *
 * @see UserManagedSubjectsTest
 */
public class UserManagedSubjectsTest {

    /**
     * Make sure user managed subjects are picked up and inserted into the graph
     */
    @Test
    public void basic() {
        var card = InstanceUtils.createInstance(IdCard.class);
        var myCapturedSubjectTemplate = ManagedTruth.assertThat(card);
        Truth.assertThat(myCapturedSubjectTemplate).isInstanceOf(MyIdCardSubject.class);

        //todo assert that reflections can't find them, but source code scanning can
    }
}
