package io.stubbs.truth.generator.internal.modelSubjectChickens;

import com.google.common.truth.FailureMetadata;
import io.stubbs.truth.generator.SubjectFactoryMethod;
import io.stubbs.truth.generator.UserManagedSubject;
import io.stubbs.truth.generator.internal.model.ParentClass;
import io.stubbs.truth.generator.shaded.org.jboss.forge.roaster.model.sourceChickens.JavaClassSourceSubject;
import io.stubbs.truth.generator.testModel.MyEmployee;

import javax.annotation.processing.Generated;

import static io.stubbs.truth.generator.shaded.org.jboss.forge.roaster.model.sourceChickens.JavaClassSourceSubject.javaClassSources;

/**
 * Optionally move this class into source control, and add your custom assertions here.
 *
 * <p>
 * If the system detects this class already exists, it won't attempt to generate a new one. Note that if the base
 * skeleton of this class ever changes, you won't automatically get it updated.
 *
 * @see ParentClassParentSubject
 */
@UserManagedSubject(value = ParentClass.class)
@Generated("truth-generator")
public class ParentClassSubject extends ParentClassParentSubject {

    /**
     * Returns an assertion builder for a {@link ParentClass} class.
     */
    @SubjectFactoryMethod
    public static Factory<ParentClassSubject, ParentClass> parentClasses() {
        return ParentClassSubject::new;
    }

    protected ParentClassSubject(FailureMetadata failureMetadata,
                                 ParentClass actual) {
        super(failureMetadata, actual);
    }

    public void withSamePackageAs(Class<MyEmployee> expected) {
        String actualPackage = actual.getGenerated().getPackage();
        String expected1 = expected.getPackage().getName();
        check("getPackage()").that(actualPackage).isEqualTo(expected1);
    }

    public JavaClassSourceSubject hasSourceText() {
        return check("getSourceText()").about(javaClassSources()).that(actual.getGenerated());
    }
}
