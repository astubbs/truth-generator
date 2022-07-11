package io.stubbs.truth.generator.integrationTests;


import io.stubbs.truth.generator.integrationTests.capturedtemplates.MyIdCardSubject;
import io.stubbs.truth.generator.testModel.IdCard;
import io.stubbs.truth.generator.testModel.MyEmployee;
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
        String charles = "charles";
        var emp = InstanceUtils.createInstance(MyEmployee.class)
                .toBuilder()
                .employmentState(MyEmployee.State.EMPLOLYED)
                .workNickName(charles)
                .build();
        var empSubject = ManagedTruth.assertThat(emp);
        empSubject.isEmployed();
        empSubject.hasId().isNotNull();
        empSubject.hasWorkNickName().ignoringTrailingWhiteSpace().equalTo(charles);


        var card = InstanceUtils.createInstance(IdCard.class).toBuilder().name(charles).build();
        MyIdCardSubject subject = ManagedTruth.assertThat(card);
        subject.hasEpoch().isAtLeast(3);
        subject.hasNameEqualTo(charles);
        subject.hasId().isNotNull();


        //todo assert that reflections can't find them, but source code scanning can
    }
}
