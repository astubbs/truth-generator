package io.stubbs.truth.generator.internal;

import io.stubbs.truth.generator.UserManagedTruth;
import io.stubbs.truth.generator.internal.model.MiddleClass;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.jboss.forge.roaster.model.JavaClass;
import org.jboss.forge.roaster.model.JavaType;
import org.jboss.forge.roaster.model.source.JavaClassSource;

/**
 * @see UserManagedTruth
 */
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@ToString
public class UserSourceCodeManagedMiddleClass<T> extends MiddleClass<T> {

    private final JavaClass<?> source;

//    public UserSourceCodeManagedMiddleClass(JavaType<?> parse) {
//    }

    public UserSourceCodeManagedMiddleClass(JavaType<?> finalParse, Class<T> classUnderTest) {
        super(classUnderTest);
        this.source = (JavaClass<?>) finalParse;
    }

    @Override
    public String getSimpleName() {
        return source.getName();
    }

    @Override
    public void makeChildExtend(JavaClassSource child) {
        child.extendSuperType(source);
    }

    @Override
    public String getCanonicalName() {
        return source.getCanonicalName();
    }

    @Override
    public String getFactoryMethodName() {
        // todo find factory method
        return null;
    }

    @Override
    public String getPackage() {
        return source.getPackage();
    }
}
