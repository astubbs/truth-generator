package io.stubbs.truth.generator.internal.modelSubjectChickens;

import com.google.common.truth.FailureMetadata;
import io.stubbs.truth.generator.SubjectFactoryMethod;
import io.stubbs.truth.generator.UserManagedTruth;
import io.stubbs.truth.generator.internal.TruthGeneratorRuntimeException;
import io.stubbs.truth.generator.internal.model.GeneratedMiddleClass;
import io.stubbs.truth.generator.internal.model.MiddleClass;
import io.stubbs.truth.generator.shaded.org.jboss.forge.roaster.model.sourceChickens.JavaClassSourceSubject;
import io.stubbs.truth.generator.testModel.MyEmployee;

import javax.annotation.processing.Generated;

import static io.stubbs.truth.generator.shaded.org.jboss.forge.roaster.model.sourceChickens.JavaClassSourceSubject.javaClassSources;

// in VCS as we're still in the chicken phase of what comes first - stable maven plugin to generate this for the build before we can remove

/**
 * Optionally move this class into source control, and add your custom assertions here.
 *
 * <p>
 * If the system detects this class already exists, it won't attempt to generate a new one. Note that if the base
 * skeleton of this class ever changes, you won't automatically get it updated.
 *
 * @see MiddleClassParentSubject
 */
@UserManagedTruth(value = MiddleClass.class)
@Generated("truth-generator")
public class MiddleClassSubject extends MiddleClassParentSubject {

    /**
     * Returns an assertion builder for a {@link MiddleClass} class.
     */
    @SubjectFactoryMethod
    public static Factory<MiddleClassSubject, MiddleClass> middleClasses() {
        return MiddleClassSubject::new;
    }

    protected MiddleClassSubject(FailureMetadata failureMetadata,
                                 MiddleClass actual) {
        super(failureMetadata, actual);
    }

    public void withSamePackageAs(Class<MyEmployee> expected) {
        String actualPackage = actual.getPackage();
        check("getPackage()").that(actualPackage).isEqualTo(expected.getPackage().getName());
    }

    public JavaClassSourceSubject hasSourceText() {
        if (GeneratedMiddleClass.class.isAssignableFrom(actual.getClass())) {
            var generagedMiddle = (GeneratedMiddleClass) actual;
            return check("getSourceText()").about(javaClassSources()).that(generagedMiddle.getGenerated());
        } else {
            throw new TruthGeneratorRuntimeException("Not a generated class");
        }

    }

}
