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
public class GeneratedMiddleClass<T> extends MiddleClass<T> implements AGeneratedClass {

    JavaClassSource generated;

    MethodSource<JavaClassSource> factoryMethod;

    public GeneratedMiddleClass(final JavaClassSource generated, final MethodSource factory, final Class<T> classUnderTest) {
        super(classUnderTest);
        this.generated = generated;
        this.factoryMethod = factory;
    }

    @Override
    public String getSimpleName() {
        return generated.getName();
    }

    @Override
    public void makeChildExtend(JavaClassSource child) {
        child.extendSuperType(generated);
    }

    @Override
    public String getCanonicalName() {
        return generated.getCanonicalName();
    }

    @Override
    public String getFactoryMethodName() {
        return this.factoryMethod.getName();
    }

    @Override
    public String getPackage() {
        return generated.getPackage();
    }

    @Override
    public JavaClassSource getGenerated() {
        return generated;
    }

}