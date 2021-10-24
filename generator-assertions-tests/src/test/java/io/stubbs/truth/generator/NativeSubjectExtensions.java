package io.stubbs.truth.generator;

import io.stubbs.truth.generator.internal.extensions.ManagedTruth;
import io.stubbs.truth.generator.internal.extensions.MyEmployeeSubject;
import io.stubbs.truth.generator.testModel.MyEmployee;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class NativeSubjectExtensions {

  @Test
  public void my_string() {
    String nameWithSpace = "tony  ";
    MyEmployee emp = InstanceUtils.createInstance(MyEmployee.class).toBuilder().workNickName(nameWithSpace).build();
    MyEmployeeSubject es = ManagedTruth.assertThat(emp);

    // my strings
    es.hasWorkNickName().ignoringTrailingWhiteSpace().equalTo("tony");

    // my maps
    assertThatThrownBy(() -> es.hasProjectMap().containsKeys("key1", "key2")).isInstanceOf(AssertionError.class);
    List<String> keys = new ArrayList<>(emp.getProjectMap().keySet());
    es.hasProjectMap().containsKeys(keys.get(0), keys.get(1));
  }

}
