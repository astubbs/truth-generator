package io.stubbs.truth.generator.subjects;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.MapSubject;
import io.stubbs.truth.generator.BaseSubjectExtension;
import io.stubbs.truth.generator.SubjectFactoryMethod;

import java.util.Map;

/**
 * @author Antony Stubbs
 */
@BaseSubjectExtension(Map.class)
public class MyMapSubject extends MapSubject {

    protected final Map<?, ?> actual;

    @SubjectFactoryMethod
    public static Factory<MyMapSubject, Map<?, ?>> maps() {
        return MyMapSubject::new;
    }

    protected MyMapSubject(FailureMetadata failureMetadata, Map<?, ?> actual) {
        super(failureMetadata, actual);
        this.actual = actual;
    }

    public void containsKeys(Object... keys) {
        isNotNull();
        check("keySet()").that(actual.keySet()).containsAtLeastElementsIn(keys);
    }
}
