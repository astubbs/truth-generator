package io.stubbs.truth.generator.internal.model;

import io.stubbs.truth.generator.UserManagedTruth;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.jboss.forge.roaster.model.source.AnnotationSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

/**
 * @see UserManagedTruth
 */
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@ToString
public class UserSourceCodeManagedMiddleClass<T> extends RoasterMiddleClass<T> {

    public UserSourceCodeManagedMiddleClass(JavaClassSource sourceCodeModel, MethodSource factory) {
        super(sourceCodeModel, factory);
    }

    @Override
    public String getClassUnderTestSimpleName() {
        AnnotationSource<JavaClassSource> userManaged = super.sourceCodeModel.getAnnotation(UserManagedTruth.class);
        String underTest = userManaged.getStringValue();
        return underTest;
    }
}
