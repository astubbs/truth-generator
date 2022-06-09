package io.stubbs.truth.generator.internal;

import com.google.common.truth.IntegerSubject;
import com.google.common.truth.OptionalSubject;
import com.google.common.truth.Subject;
import com.google.common.truth.Truth8;
import io.stubbs.truth.generator.TestClassFactories;
import io.stubbs.truth.generator.TestModelUtils;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import io.stubbs.truth.generator.subjects.MyMapSubject;
import io.stubbs.truth.generator.subjects.MyStringSubject;
import io.stubbs.truth.generator.testModel.MyEmployee;
import io.stubbs.truth.generator.testModel.Person;
import lombok.SneakyThrows;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;

/**
 * Not possible to unwrap an Optional<TYPE> return type for a class with type parameters. Can only do so with a subtype
 * where the type parameter has been set.
 */
public class OptionalUnwrapChainForGenericTypeArgsTest {

    GeneratedSubjectTypeStore subjects = TestClassFactories.newGeneratedSubjectTypeStore();


    /**
     * @see MyEmployee#getTypeParamTest()
     */
    @Test
    public void subclass() {
        var resolvedPair = testResolution(MyEmployee.class, "getTypeParamTest", String.class);
        Truth8.assertThat(resolvedPair.getSubject()).isPresent();
        assertThat(resolvedPair.getSubject().get().getClazz()).isEqualTo(MyStringSubject.class);
    }

    @Test
    public void genericReturnTypeWithoutSpecialHandling() {
        var resolvedPair = testResolution(MyEmployee.class, "getProjectMap", Map.class);
        Truth8.assertThat(resolvedPair.getSubject()).isPresent();
        assertThat(resolvedPair.getSubject().get().getClazz()).isEqualTo(MyMapSubject.class);
    }

    /**
     * @see MyEmployee#getTypeParamTestOptional()
     */
    @SneakyThrows
    @Test
    public void subclassOptional() {
        var resolvedPair = testResolution(MyEmployee.class, "getTypeParamTestOptional", String.class);
        Truth8.assertThat(resolvedPair.getSubject()).isPresent();
        assertThat(resolvedPair.getSubject().get().getClazz()).isEqualTo(MyStringSubject.class);
    }


    /**
     * @see MyEmployee#getTypeParamTest()
     */
    @Test
    public void directClass() {
        var resolvedPair = testResolution(Person.class, "getTypeParamTest", Object.class);
        Truth8.assertThat(resolvedPair.getSubject()).isPresent();
        assertThat(resolvedPair.getSubject().get().getClazz()).isEqualTo(Subject.class);
    }


    /**
     * @see Person#getTypeParamTestOptional()
     */
    @Test
    public void directClassOptional() {
        var resolvedPair = testResolution(Person.class, "getTypeParamTestOptional", Object.class);
        Truth8.assertThat(resolvedPair.getSubject()).isPresent();
        assertThat(resolvedPair.getSubject().get().getClazz()).isEqualTo(Subject.class);
    }


    /**
     * @see Person#getMyWildcardTypeWithUpperBoundsIdCard()
     */
    @Test
    public void wildCardTypeDirect() {
        var resolvedPair = testResolution(Person.class, "getMyWildcardTypeWithUpperBoundsIdCard", Integer.class);
        Truth8.assertThat(resolvedPair.getSubject()).isPresent();
        assertThat(resolvedPair.getSubject().get().getClazz()).isEqualTo(IntegerSubject.class);
    }


    /**
     * @see Person#getMyWildcardTypeWithUpperBoundsIdCard()
     */
    @Test
    public void wildCardTypeFromSubtype() {
        var resolvedPair = testResolution(MyEmployee.class, "getMyWildcardTypeWithUpperBoundsIdCard", Integer.class);
        Truth8.assertThat(resolvedPair.getSubject()).isPresent();
        assertThat(resolvedPair.getSubject().get().getClazz()).isEqualTo(IntegerSubject.class);
    }

    /**
     * @see Person#getMyWildcardTypeWithLowerAndUpperBoundsGeneric()
     */
    @Test
    public void wildCardTypeDirectUpperBoundGeneric() {
        var resolvedPair = testResolution(Person.class, "getMyWildcardTypeWithLowerAndUpperBoundsGeneric", Optional.class);
        Truth8.assertThat(resolvedPair.getSubject()).isPresent();
        assertThat(resolvedPair.getSubject().get().getClazz()).isEqualTo(OptionalSubject.class);
    }


    /**
     * @see Person#getMyWildcardTypeWithLowerAndUpperBoundsGeneric()
     */
    @Test
    public void wildCardTypeFromSubtypeUpperBoundGeneric() {
        var resolvedPair = testResolution(MyEmployee.class, "getMyWildcardTypeWithLowerAndUpperBoundsGeneric", Optional.class);
        Truth8.assertThat(resolvedPair.getSubject()).isPresent();
        assertThat(resolvedPair.getSubject().get().getClazz()).isEqualTo(OptionalSubject.class);
    }

    /**
     * @see Person#getMyWildcardTypeWithLowerAndUpperBoundsIdCard()
     */
    @Test
    public void wildCardTypeDirectUpperBoundIdCard() {
        var resolvedPair = testResolution(Person.class, "getMyWildcardTypeWithLowerAndUpperBoundsIdCard", Optional.class);
        Truth8.assertThat(resolvedPair.getSubject()).isPresent();
        assertThat(resolvedPair.getSubject().get().getClazz()).isEqualTo(OptionalSubject.class);
    }


    /**
     * @see Person#getMyWildcardTypeWithLowerAndUpperBoundsIdCard()
     */
    @Test
    public void wildCardTypeFromSubtypeUpperBoundIdCard() {
        var resolvedPair = testResolution(MyEmployee.class, "getMyWildcardTypeWithLowerAndUpperBoundsIdCard", Optional.class);
        Truth8.assertThat(resolvedPair.getSubject()).isPresent();
        assertThat(resolvedPair.getSubject().get().getClazz()).isEqualTo(OptionalSubject.class);
    }


    private <T> GeneratedSubjectTypeStore.ResolvedPair testResolution(Class<T> classType, String methodName, Class<?> expectedReturnType) {
        Method getIterationStartingPoint = TestModelUtils.findMethodWithNoParamsJReflect(classType, methodName);

        ThreeSystem<T> myEmployeeThreeSystem = new ThreeSystem<T>(classType, null, null, null);

        GeneratedSubjectTypeStore.ResolvedPair resolvedPair = subjects.resolveSubjectForOptionals(myEmployeeThreeSystem, getIterationStartingPoint);

        assertThat(resolvedPair.getReturnType()).isEqualTo(expectedReturnType);
        return resolvedPair;
    }


}
