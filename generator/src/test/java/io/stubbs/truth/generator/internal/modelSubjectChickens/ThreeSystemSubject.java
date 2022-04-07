package io.stubbs.truth.generator.internal.modelSubjectChickens;

import com.google.common.truth.FailureMetadata;
import io.stubbs.truth.generator.UserManagedTruth;
import io.stubbs.truth.generator.internal.TruthGeneratorRuntimeException;
import io.stubbs.truth.generator.internal.model.GeneratedMiddleClass;
import io.stubbs.truth.generator.internal.model.MiddleClass;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import io.stubbs.truth.generator.internal.model.UserSuppliedMiddleClass;
import io.stubbs.truth.generator.shaded.org.jboss.forge.roaster.model.sourceChickens.JavaClassSourceSubject;
import org.jboss.forge.roaster.model.source.JavaClassSource;

import javax.annotation.processing.Generated;

// in VCS as we're still in the chicken phase of what comes first - stable maven plugin to generate this for the build before we can remove

/**
 * Optionally move this class into source control, and add your custom assertions here.
 *
 * <p>
 * If the system detects this class already exists, it won't attempt to generate a new one. Note that if the base
 * skeleton of this class ever changes, you won't automatically get it updated.
 *
 * @see ThreeSystemParentSubject
 */
@UserManagedTruth(value = ThreeSystem.class)
@Generated("truth-generator")
public class ThreeSystemSubject extends ThreeSystemParentSubject {

    /**
     * Returns an assertion builder for a {@link ThreeSystem} class.
     */
    public static Factory<ThreeSystemSubject, ThreeSystem> threeSystems() {
        return ThreeSystemSubject::new;
    }

    protected ThreeSystemSubject(FailureMetadata failureMetadata,
                                 ThreeSystem actual) {
        super(failureMetadata, actual);
    }

    public void hasParentSource(String expected) {
        hasParent().hasGenerated().hasSourceText().ignoringTrailingWhiteSpace().equalTo(expected);
    }

    public void hasMiddleSource(String expected) {
        MiddleClass middle = actual.getMiddle();
        if (GeneratedMiddleClass.class.isAssignableFrom(middle.getClass())) {
            JavaClassSource generated = ((GeneratedMiddleClass) middle).getGenerated();
            check(".getMiddle().getGenerated()")
                    .about(JavaClassSourceSubject.javaClassSources())
                    .that(generated)
                    .hasSourceText()
                    .ignoringTrailingWhiteSpace()
                    .equalTo(expected);
        } else {
            throw new TruthGeneratorRuntimeException("Not a generated class");
        }
    }

    public void hasChildSource(String expected) {
        hasChild().hasSourceText().ignoringTrailingWhiteSpace().equalTo(expected);
    }
}
