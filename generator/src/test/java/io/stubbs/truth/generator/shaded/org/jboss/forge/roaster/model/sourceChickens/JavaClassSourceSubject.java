package io.stubbs.truth.generator.shaded.org.jboss.forge.roaster.model.sourceChickens;

import com.google.common.truth.FailureMetadata;
import io.stubbs.truth.generator.SubjectFactoryMethod;
import io.stubbs.truth.generator.UserManagedSubject;
import io.stubbs.truth.generator.subjects.MyStringSubject;
import io.stubbs.truth.generator.testModel.MyEmployee;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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
 * @see JavaClassSourceParentSubject
 */
@Slf4j
@UserManagedSubject(value = JavaClassSource.class)
@Generated("truth-generator")
public class JavaClassSourceSubject extends JavaClassSourceParentSubject {

    /**
     * Returns an assertion builder for a {@link JavaClassSource} class.
     */
    @SubjectFactoryMethod
    public static Factory<JavaClassSourceSubject, JavaClassSource> javaClassSources() {
        return JavaClassSourceSubject::new;
    }

    protected JavaClassSourceSubject(FailureMetadata failureMetadata, JavaClassSource actual) {
        super(failureMetadata, actual);
    }

    public MyStringSubject hasSourceText() {
        isNotNull();
        return check("toString()").about(MyStringSubject.strings()).that(actual.toString());
    }

    public void withSamePackageAs(Class<MyEmployee> expected) {
        String actualPackage = actual.getPackage();
        check("getPackage()").that(actualPackage).isEqualTo(expected.getPackage().getName());
    }

    // todo drop this step if possible?
    @SneakyThrows
    public MyStringSubject.IgnoringWhiteSpaceComparison withSourceOf() {
        return hasSourceText().ignoringTrailingWhiteSpace();
    }

}
