package io.stubbs.truth.generator.internal;

import com.google.common.truth.Subject;
import com.google.common.truth.Truth8;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import io.stubbs.truth.generator.subjects.MyMapSubject;
import io.stubbs.truth.generator.subjects.MyStringSubject;
import io.stubbs.truth.generator.testModel.MyEmployee;
import io.stubbs.truth.generator.testModel.Person;
import lombok.SneakyThrows;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static io.stubbs.truth.generator.TestModelUtils.findMethod;

/**
 * Not possible to unwrap an Optional<TYPE> return type for a class with type parameters. Can only do so with a subtype
 * where the type parameter has been set.
 */
public class OptionalUnwrapChainForGenericTypeArgsTest extends SubjectStoreTests {

    /**
     * @see MyEmployee#getTypeParamTest()
     */
    @Test
    public void subclass() {
        var resolvedPair = testResolution(MyEmployee.class, "getTypeParamTest", String.class);
        assertThat(resolvedPair.getSubject().get().getClazz()).isEqualTo(MyStringSubject.class);
    }

    @Test
    public void genericReturnTypeWithoutSpecialHandling() {
        var resolvedPair = testResolution(MyEmployee.class, "getProjectMap", Map.class);
        assertThat(resolvedPair.getSubject().get().getClazz()).isEqualTo(MyMapSubject.class);
    }

    /**
     * @see MyEmployee#getTypeParamTestOptional()
     */
    @SneakyThrows
    @Test
    public void subclassOptional() {
        var resolvedPair = testResolution(MyEmployee.class, "getTypeParamTestOptional", String.class);
        assertThat(resolvedPair.getSubject().get().getClazz()).isEqualTo(MyStringSubject.class);
    }


    /**
     * @see MyEmployee#getTypeParamTest()
     */
    @Test
    public void directClass() {
        var resolvedPair = testResolution(Person.class, "getTypeParamTest", Object.class);
        assertThat(resolvedPair.getSubject().get().getClazz()).isEqualTo(Subject.class);
    }


    /**
     * @see Person#getTypeParamTestOptional()
     */
    @Test
    public void directClassOptional() {
        var resolvedPair = testResolution(Person.class, "getTypeParamTestOptional", Object.class);
        assertThat(resolvedPair.getSubject().get().getClazz()).isEqualTo(Subject.class);
    }


}
