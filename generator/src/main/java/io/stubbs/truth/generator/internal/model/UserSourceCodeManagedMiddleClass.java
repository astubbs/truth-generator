package io.stubbs.truth.generator.internal.model;

import io.stubbs.truth.generator.UserManagedTruth;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

/**
 * @see UserManagedTruth
 */
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@ToString
public class UserSourceCodeManagedMiddleClass<T> extends RoasterMiddleClass<T> {

    public UserSourceCodeManagedMiddleClass(JavaClassSource finalParse, MethodSource factory, Class<T> classUnderTest) {
        super(finalParse, factory, classUnderTest);
    }

}
