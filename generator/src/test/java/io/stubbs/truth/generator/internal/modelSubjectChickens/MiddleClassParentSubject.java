package io.stubbs.truth.generator.internal.modelSubjectChickens;

import com.google.common.truth.ClassSubject;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.StringSubject;
import com.google.common.truth.Subject;
import io.stubbs.truth.generator.internal.model.MiddleClass;
import io.stubbs.truth.generator.shaded.org.jboss.forge.roaster.model.sourceChickens.JavaClassSourceSubject;

import javax.annotation.processing.Generated;

// in VCS as we're still in the chicken phase of what comes first - stable maven plugin to generate this for the build before we can remove

/**
 * Truth Subject for the {@link MiddleClass}.
 * <p>
 * Note that this class is generated / managed, and will change over time. So any changes you might make will be
 * overwritten.
 *
 * @see MiddleClass
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

    public ClassSubject hasUsersMiddleClass() {
        isNotNull();
        return check("getUsersMiddleClass").that(actual.getUsersMiddleClass());
    }

    public JavaClassSourceSubject hasGenerated() {
        isNotNull();
        return check("getGenerated").about(JavaClassSourceSubject.javaClassSources()).that(actual.getGenerated());
    }

    public StringSubject hasSimpleName() {
        isNotNull();
        return check("getSimpleName").that(actual.getSimpleName());
    }
}
