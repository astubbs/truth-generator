package io.stubbs.truth.generator;

import com.google.common.truth.Truth;
import io.stubbs.truth.generator.internal.TruthGeneratorTest;
import io.stubbs.truth.generator.internal.legacy.NonBeanLegacyChildSubject;
import io.stubbs.truth.generator.internal.legacy.NonBeanLegacySubject;
import io.stubbs.truth.generator.testModel.ManagedTruth;
import io.stubbs.truth.generator.testModel.MyEmployee;
import io.stubbs.truth.generator.testModel.MyEmployeeChildSubject;
import io.stubbs.truth.generator.testModel.MyEmployeeSubject;
import io.stubbs.truth.generator.testing.legacy.NonBeanLegacy;
import org.junit.Test;
import uk.co.jemos.podam.api.PodamFactoryImpl;


/**
 * Uses output from packages completed tests run from the generator module.
 *
 * @see TruthGeneratorTest#generate_code
 */
public class GeneratedAssertionTests {

  public static final PodamFactoryImpl PODAM_FACTORY = new PodamFactoryImpl();

  @Test
  public void try_out_assertions() {
    // all asserts should be available
    MyEmployee hi = InstanceUtils.createInstance(MyEmployee.class);
    hi = hi.toBuilder()
            .name("Zeynep")
            .boss(InstanceUtils.createInstance(MyEmployee.class).toBuilder().name("Tony").build())
            .build();

    MyEmployeeChildSubject.assertTruth(hi).hasBirthYear().isAtLeast(200);
    Truth.assertThat(hi.getBirthYear()).isAtLeast(200);

    Truth.assertThat(hi.getBoss().getName()).contains("Tony");
    MyEmployeeChildSubject.assertTruth(hi).hasBoss().hasName().contains("Tony");
    MyEmployeeChildSubject.assertTruth(hi).hasCard().hasEpoch().isAtLeast(20);
    MyEmployeeChildSubject.assertTruth(hi).hasProjectList().hasSize(5);
    MyEmployeeSubject myEmployeeSubject = MyEmployeeChildSubject.assertTruth(hi);

    MyEmployeeChildSubject.assertThat(TestModelUtils.createEmployee()).hasProjectMapWithKey("key");
  }

  /**
   * @see TruthGeneratorTest#test_legacy_mode
   */
  @Test
  public void test_legacy_mode() {
    NonBeanLegacy nonBeanLegacy = InstanceUtils.createInstance(NonBeanLegacy.class).toBuilder().name("lilan").build();
    NonBeanLegacySubject nonBeanLegacySubject = NonBeanLegacyChildSubject.assertThat(nonBeanLegacy);
    nonBeanLegacySubject.hasAge().isNotNull();
    nonBeanLegacySubject.hasName().isEqualTo("lilan");
  }

  @Test
  public void recursive() {
    MyEmployee emp = InstanceUtils.createInstance(MyEmployee.class);
    MyEmployeeSubject es = ManagedTruth.assertThat(emp);

    es.hasAnniversary().hasToLocalDate().hasEra().hasValue().isNotNull();
    es.hasAnniversary().hasToLocalDate().hasChronology().hasId().isNotNull();
  }

  @Test
  public void as_type_chain_transformers() {
    MyEmployee emp = InstanceUtils.createInstance(MyEmployee.class);
    MyEmployeeSubject es = ManagedTruth.assertThat(emp);

    es.hasAnniversary().hasToLocalDate().hasEra();
    es.hasAnniversary().hasToLocalDateTime().hasToLocalDate().hasEra().isNotNull();
  }

  @Test
  public void enums(){
    MyEmployee emp = InstanceUtils.createInstance(MyEmployee.class).toBuilder().employmentState(MyEmployee.State.NEVER_EMPLOYED).build();
    MyEmployeeSubject es = ManagedTruth.assertThat(emp);
    es.hasEmploymentState().isEqualTo(MyEmployee.State.NEVER_EMPLOYED);
  }

}
