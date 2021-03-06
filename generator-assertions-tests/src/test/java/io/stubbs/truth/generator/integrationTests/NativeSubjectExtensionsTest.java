package io.stubbs.truth.generator.integrationTests;

import com.google.common.truth.StringSubject;
import com.google.common.truth.Subject;
import io.stubbs.truth.generator.subjects.MyStringSubject;
import io.stubbs.truth.generator.testModel.MyEmployee;
import io.stubbs.truth.generator.testModel.MyEmployeeSubject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests that generated {@link Subject}s will use "native" Subject extensions when they exist - for example - instead of
 * returning {@link StringSubject} for String fields, a {@link MyStringSubject} would be returned instead.
 * <p>
 * If the mechanism isn't working, this class my appear not to compile in the marked sections "needs my" below.
 */
// TODO rename away from "native" subject - it's not very clear what that means
public class NativeSubjectExtensionsTest {

    @Test
    public void my_string() {
        String nameWithSpace = "tony  ";
        MyEmployee emp = InstanceUtils.createInstance(MyEmployee.class).toBuilder().workNickName(nameWithSpace).build();
        MyEmployeeSubject es = ManagedTruth.assertThat(emp);

        // needs my strings
        es.hasWorkNickName().ignoringTrailingWhiteSpace().equalTo("tony");

        // needs my maps
        assertThatThrownBy(() -> es.hasProjectMap().containsKeys("key1", "key2")).isInstanceOf(AssertionError.class);
        List<String> keys = new ArrayList<>(emp.getProjectMap().keySet());
        es.hasProjectMap().containsKeys(keys.get(0), keys.get(1));
    }

}
