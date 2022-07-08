package io.stubbs.truth.generator.shaded.org.jboss.forge.roaster.model.sourceChickens;

import com.google.common.io.Resources;
import com.google.common.truth.FailureMetadata;
import io.stubbs.truth.generator.SubjectFactoryMethod;
import io.stubbs.truth.generator.UserManagedTruth;
import io.stubbs.truth.generator.internal.model.TextFile;
import io.stubbs.truth.generator.subjects.MyStringSubject;
import io.stubbs.truth.generator.testModel.MyEmployee;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jboss.forge.roaster.model.source.JavaClassSource;

import javax.annotation.processing.Generated;
import java.net.URL;
import java.nio.charset.Charset;

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
@UserManagedTruth(value = JavaClassSource.class)
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
        return check("toString").about(MyStringSubject.strings()).that(actual.toString());
    }

    public void withSamePackageAs(Class<MyEmployee> expected) {
        String actualPackage = actual.getPackage();
        check("getPackage()").that(actualPackage).isEqualTo(expected.getPackage().getName());
    }

    @SneakyThrows
    public void withSourceOf(TextFile child) {
        URL resource = Resources.getResource(child.getResourcePath());
        String expected = Resources.toString(resource, Charset.defaultCharset());
        log.error("Checking {}", resource);
        hasSourceText().ignoringTrailingWhiteSpace().equalTo(expected);
    }

}
