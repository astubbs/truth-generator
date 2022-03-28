package io.stubbs.truth.generator;

import io.stubbs.truth.generator.internal.TruthGeneratorTest;
import io.stubbs.truth.generator.testModel.MyEmployee;
import io.stubbs.truth.generator.testModel.MyEmployeeChildSubject;
import io.stubbs.truth.generator.testModel.MyEmployeeSubject;
import io.stubbs.truth.generator.testing.legacy.NonBeanLegacy;
import io.stubbs.truth.generator.testing.legacy.NonBeanLegacyChildSubject;
import io.stubbs.truth.generator.testing.legacy.NonBeanLegacySubject;
import io.stubbs.truth.tests.ManagedTruth;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import uk.co.jemos.podam.api.PodamFactoryImpl;

import java.io.File;
import java.time.Instant;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;


/**
 * Uses output from packages completed tests run from the generator module.
 *
 * @see TruthGeneratorTest#generate_code
 */
public class GeneratedAssertionTests {

  public static final PodamFactoryImpl PODAM_FACTORY = new PodamFactoryImpl();

  @Test
  public void filesExist() {
    Assertions.assertThat(new File("target/generated-test-sources/truth-assertions-managed/io/stubbs/truth/tests/autoShaded/java/time/chrono/EraParentSubject.java")).exists();
  }

  @Test
  public void try_out_assertions() {
    // all asserts should be available
    MyEmployee hi = InstanceUtils.createInstance(MyEmployee.class);
    hi = hi.toBuilder()
            .name("Zeynep")
            .boss(InstanceUtils.createInstance(MyEmployee.class).toBuilder().name("Tony").build())
            .build();

    MyEmployeeChildSubject.assertTruth(hi).hasBirthYear().isAtLeast(200);
    assertThat(hi.getBirthYear()).isAtLeast(200);

    assertThat(hi.getBoss().getName()).contains("Tony");
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

  @Test
  public void boolean_methods() {
    MyEmployee emp = TestModelUtils.createInstance(MyEmployee.class)
            // not sure how or why, but PODAM is always setting our FALSE test boolean to true, so... ->
            .toBuilder().testBooleanIsFalse(false).build();
    String santity = emp.getSantity();
    boolean boss = emp.isBoss();
    boolean testBoolean = emp.isTestBooleanIsFalse();

    assertThat(testBoolean).isFalse();
    MyEmployeeChildSubject.assertThat(emp).isNotTestBooleanIsFalse();

    {
      MyEmployee build = emp.toBuilder().employmentState(MyEmployee.State.EMPLOLYED).build();
      MyEmployeeChildSubject.assertThat(build).isNotBoss();
    }
    {
      MyEmployee build = emp.toBuilder().employmentState(MyEmployee.State.IS_A_BOSS).build();
      MyEmployeeChildSubject.assertThat(build).isBoss();
    }
  }

  @Test
  public void optionalPresenceTest() {
    MyEmployee emp = TestModelUtils.createInstance(MyEmployee.class).toBuilder()
            .weighting(Optional.empty())
            .build();
    Assertions.assertThatThrownBy(
            () -> ManagedTruth.assertThat(emp).hasWeighting().isZero()
    ).hasMessageContaining("expected Weighting to be present");
  }

  @Test
  public void instantsAreComparableSubjectsAsWell(){
    MyEmployee emp = TestModelUtils.createInstance(MyEmployee.class).toBuilder().build();
    ManagedTruth.assertThat(emp).hasStartedAt().isGreaterThan(Instant.MIN);
  }

}
