package io.stubbs.truth.generator.internal.model;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

/**
 * @author Antony Stubbs
 */
@EqualsAndHashCode(callSuper = true)
@Value
@SuperBuilder
public class MiddleClass extends AClass {
    MethodSource<JavaClassSource> factoryMethod;
    Class<?> usersMiddleClass;
    Class<?> classUnderTest;

    public static MiddleClass of(Class<?> aClass, Class<?> classUnderTest) {
        return MiddleClass.builder()
                .usersMiddleClass(aClass)
                .classUnderTest(classUnderTest)
                .build();
    }

    public static MiddleClass of(JavaClassSource middle, MethodSource factory, Class<?> classUnderTest) {
        return MiddleClass.builder()
                .generated(middle)
                .classUnderTest(classUnderTest)
                .factoryMethod(factory).build();
    }

    public String getSimpleName() {
        return (usersMiddleClass == null)
                ? super.generated.getName()
                : usersMiddleClass.getName();
    }

    public void makeChildExtend(JavaClassSource child) {
        if (usersMiddleClass == null)
            child.extendSuperType(generated);
        else
            child.extendSuperType(usersMiddleClass);
    }

    public String getCanonicalName() {
        return (usersMiddleClass == null)
                ? super.generated.getCanonicalName()
                : usersMiddleClass.getCanonicalName();
    }
}
