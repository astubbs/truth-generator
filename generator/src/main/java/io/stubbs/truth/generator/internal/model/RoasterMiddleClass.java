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
public abstract class RoasterMiddleClass<T> extends MiddleClass<T> {

    protected JavaClassSource sourceCodeModel;

    MethodSource<JavaClassSource> factoryMethod;

    public RoasterMiddleClass(JavaClassSource sourceCodeModel, MethodSource factory) {
        super();
        this.sourceCodeModel = sourceCodeModel;
        this.factoryMethod = factory;
    }

    @Override
    public String getSimpleName() {
        return sourceCodeModel.getName();
    }

    @Override
    public void makeChildExtend(JavaClassSource child) {
        child.extendSuperType(sourceCodeModel);
    }

    @Override
    public String getCanonicalName() {
        return sourceCodeModel.getCanonicalName();
    }

    @Override
    public String getFactoryMethodName() {
        return this.factoryMethod.getName();
    }

    @Override
    public String getPackage() {
        return sourceCodeModel.getPackage();
    }

}
