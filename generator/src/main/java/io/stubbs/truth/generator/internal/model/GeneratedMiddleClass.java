package io.stubbs.truth.generator.internal.model;


import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

/**
 * @author Antony Stubbs
 */
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@ToString
public class GeneratedMiddleClass<T> extends RoasterMiddleClass<T> implements AGeneratedClass {

    public GeneratedMiddleClass(JavaClassSource generated, MethodSource factory, Class<T> classUnderTest) {
        super(generated, factory, classUnderTest);
    }

    @Override
    public JavaClassSource getGenerated() {
        return super.generated;
    }

}
