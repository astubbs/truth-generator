package io.stubbs.truth.generator.internal.modelSubjectChickens;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.StringSubject;
import com.google.common.truth.Subject;
import io.stubbs.truth.generator.internal.model.GeneratedMiddleClass;
import io.stubbs.truth.generator.internal.model.MiddleClass;

import javax.annotation.processing.Generated;

// in VCS as we're still in the chicken phase of what comes first - stable maven plugin to generate this for the build before we can remove

/**
 * Truth Subject for the {@link GeneratedMiddleClass}.
 * <p>
 * Note that this class is generated / managed, and will change over time. So any changes you might make will be
 * overwritten.
 *
 * @see GeneratedMiddleClass
 * @see MiddleClassSubject
 * @see MiddleClassChildSubject
 */
@Generated("truth-generator")
public class MiddleClassParentSubject extends Subject {

    protected final MiddleClass actual;

    protected MiddleClassParentSubject(FailureMetadata failureMetadata,
                                       MiddleClass actual) {
        super(failureMetadata, actual);
        this.actual = actual;
    }

    public StringSubject hasCanonicalName() {
        isNotNull();
        return check("getCanonicalName").that(actual.getCanonicalName());
    }

    public StringSubject hasSimpleName() {
        isNotNull();
        return check("getSimpleName").that(actual.getSimpleName());
    }
}
