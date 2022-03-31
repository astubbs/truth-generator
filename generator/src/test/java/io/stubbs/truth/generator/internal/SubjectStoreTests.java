package io.stubbs.truth.generator.internal;

import com.google.common.truth.Truth8;
import io.stubbs.truth.generator.internal.model.ThreeSystem;

import java.lang.reflect.Method;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;
import static io.stubbs.truth.generator.TestModelUtils.findMethod;

public abstract class SubjectStoreTests {

    GeneratedSubjectTypeStore subjects = new GeneratedSubjectTypeStore(Set.of(), new BuiltInSubjectTypeStore());

    protected <T> GeneratedSubjectTypeStore.ResolvedPair testResolution(Class<T> classType, String methodName, Class<?> expectedReturnType) {
        Method getIterationStartingPoint = findMethod(classType, methodName);

        ThreeSystem<T> myEmployeeThreeSystem = new ThreeSystem<T>(classType, null, null, null);

        GeneratedSubjectTypeStore.ResolvedPair resolvedPair = subjects.resolveSubjectForOptionals(myEmployeeThreeSystem, getIterationStartingPoint);


        assertThat(resolvedPair.getReturnType()).isEqualTo(expectedReturnType);

        Truth8.assertThat(resolvedPair.getSubject()).isPresent();

        return resolvedPair;
    }
}
