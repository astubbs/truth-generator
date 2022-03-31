package io.stubbs.truth.generator.internal;

import io.stubbs.truth.generator.TestModelUtils;
import io.stubbs.truth.generator.subjects.MyMapSubject;
import io.stubbs.truth.generator.testModel.MyEmployee;
import io.stubbs.truth.generator.testModel.Project;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;

public class CustomClassPathSubjects {

    GeneratedSubjectTypeStore subjects = new GeneratedSubjectTypeStore(Set.of(), new BuiltInSubjectTypeStore());

    /**
     * @see MyEmployee#getTypeParamTest()
     */
    @Test
    public void subclass() {
        // for reference
        MyEmployee employee = TestModelUtils.createEmployee();
        Map<String, Project> projectMap = employee.getProjectMap();

        //
        Method getProjectMap = TestModelUtils.findMethod(MyEmployee.class, "getProjectMap");
        var subjectForType = subjects.getSubjectForType(getProjectMap.getReturnType());
        assertThat(subjectForType.get().getClazz()).isEqualTo(MyMapSubject.class);
    }
}
