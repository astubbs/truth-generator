package io.stubbs.truth.generator.internal;

import com.google.common.truth.LongStreamSubject;
import com.google.common.truth.OptionalDoubleSubject;
import io.stubbs.truth.generator.TestModelUtils;
import io.stubbs.truth.generator.testModel.MyEmployee;
import org.junit.Test;

import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.LongStream;

import static com.google.common.truth.Truth.assertThat;

public class Truth8Support extends SubjectStoreTests {

    GeneratedSubjectTypeStore subjects = new GeneratedSubjectTypeStore(Set.of(), new BuiltInSubjectTypeStore());

    @Test
    public void doubleStream() {
        // for reference
        MyEmployee employee = TestModelUtils.createEmployee();
        LongStream myLongStream = employee.getMyLongStream();

        //
        var res = super.testResolution(MyEmployee.class, "getMyLongStream", LongStream.class);
        assertThat(res.getSubject().get().getClazz()).isEqualTo(LongStreamSubject.class);
    }

    @Test
    public void optionalDouble() {
        // for reference
        MyEmployee employee = TestModelUtils.createEmployee();
        OptionalDouble myOptionalDouble = employee.getMyOptionalDouble();

        //
        var res = super.testResolution(MyEmployee.class, "getMyOptionalDouble", OptionalDouble.class);
        assertThat(res.getSubject().get().getClazz()).isEqualTo(OptionalDoubleSubject.class);
    }
}
