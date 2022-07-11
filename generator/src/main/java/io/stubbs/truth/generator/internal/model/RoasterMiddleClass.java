package io.stubbs.truth.generator.internal.model;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

/**
 * A {@link MiddleClass} implementation that is mapped to a {@link org.jboss.forge.roaster.Roaster} source code class.
 *
 * @author Antony Stubbs
 */
@ToString
@EqualsAndHashCode
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public abstract class RoasterMiddleClass<T> implements MiddleClass<T> {

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
        String canonicalOrigin = this.factoryMethod.getOrigin().getCanonicalName();
        return canonicalOrigin + '.' + factoryMethod.getName();
    }

    @Override
    public String getPackage() {
        return sourceCodeModel.getPackage();
    }

}
