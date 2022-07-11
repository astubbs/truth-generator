package io.stubbs.truth.generator.subjects;

import com.google.common.primitives.Primitives;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.IterableSubject;
import com.google.common.truth.Subject;
import io.stubbs.truth.generator.BaseSubjectExtension;
import io.stubbs.truth.generator.SubjectFactoryMethod;
import lombok.SneakyThrows;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;

/**
 * @author Antony Stubbs
 */
@BaseSubjectExtension(Collection.class)
public class MyCollectionSubject extends IterableSubject {

    protected final Collection<?> actual;

    /**
     * Returns an assertion builder for a {@link Collection} class.
     */
    @SubjectFactoryMethod
    public static Subject.Factory<MyCollectionSubject, Collection<?>> collections() {
        return MyCollectionSubject::new;
    }

    protected MyCollectionSubject(FailureMetadata failureMetadata, Collection<?> actual) {
        super(failureMetadata, actual);
        this.actual = actual;
    }

    public void hasSameSizeAs(Collection<?> expected) {
        isNotNull();
        check("size()").that(actual.size()).isEqualTo(expected.size());
    }

    public void hasSameSizeAs(HashMap<?, ?> expected) {
        isNotNull();
        check("size()").that(actual.size()).isEqualTo(expected.size());
    }

    /**
     * Tries to call #size() method on {@code objetWithSizeMethod} using Reflection.
     *
     * @throws AssertionException is no {@code size} method can be found
     */
    @SneakyThrows
    public void hasSameSizeAs(Object objetWithSizeMethod) {
        isNotNull();
        Class<?> aClass = objetWithSizeMethod.getClass();
        Method[] methods = aClass.getMethods();
        Optional<Method> size = Arrays.stream(methods)
                .filter(x -> {
                    Class<?> returnType = Primitives.wrap(x.getReturnType());
                    boolean noParams = x.getParameterCount() == 0;
                    boolean returnTypeIsNumberLike = Number.class.isAssignableFrom(returnType);
                    boolean name = x.getName().equals("size");
                    return name && noParams && returnTypeIsNumberLike;
                })
                .findFirst();
        if (size.isEmpty())
            throw new AssertionException("No method called 'size' with zero args returning int exists in " + aClass);
        else {
            Method sizeMethod = size.get();
            Number expectedSize = (Number) sizeMethod.invoke(objetWithSizeMethod);
            check("size()").that(actual.size()).isEqualTo(expectedSize);
        }
    }

}
