package io.stubbs.truth.generator.internal;

import com.google.common.truth.Subject;
import com.google.common.truth.Truth8;
import io.stubbs.truth.generator.TestClassFactories;
import io.stubbs.truth.generator.TestModelUtils;
import io.stubbs.truth.generator.subjects.MyCollectionSubject;
import io.stubbs.truth.generator.subjects.MyMapSubject;
import io.stubbs.truth.generator.testModel.MyEmployee;
import io.stubbs.truth.generator.testModel.Project;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;

@Slf4j
public class CustomClassPathSubjectTests {

    GeneratedSubjectTypeStore subjects = TestClassFactories.newGeneratedSubjectTypeStore();

    /**
     * Checks that {@link Subject} subtypes are found and added to the Subject graph for inclusion into the assertion
     * chains.
     *
     * @see MyEmployee#getTypeParamTest()
     * @see MyMapSubject
     */
    @Test
    public void subclass() {
        // for reference
        MyEmployee employee = TestModelUtils.createEmployee();
        Map<String, Project> projectMap = employee.getProjectMap();
        //

        {
            Method getProjectMap = TestModelUtils.findMethodWithNoParamsJReflect(MyEmployee.class, "getProjectMap");
            var subjectForType = subjects.getSubjectForType(getProjectMap.getReturnType());
            Truth8.assertThat(subjectForType).isPresent();
            assertThat(subjectForType.get().getClazz()).isEqualTo(MyMapSubject.class);
        }

        {
            List<Project> projectList = employee.getProjectList();
            Method getProjectMap = TestModelUtils.findMethodWithNoParamsJReflect(MyEmployee.class, "getProjectList");
            var subjectForType = subjects.getSubjectForType(getProjectMap.getReturnType());
            Truth8.assertThat(subjectForType).isPresent();
            assertThat(subjectForType.get().getClazz()).isEqualTo(MyCollectionSubject.class);
        }
    }
}
