package io.stubbs.truth.generator.internal.model;

import io.stubbs.truth.generator.UserManagedMiddleSubject;
import io.stubbs.truth.generator.UserManagedSubject;
import io.stubbs.truth.generator.internal.Utils;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.jboss.forge.roaster.model.source.JavaClassSource;

import java.lang.reflect.Method;

/**
 * @see UserManagedSubject
 */
@ToString
@EqualsAndHashCode
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class UserSuppliedCompiledMiddleClass<T> implements MiddleClass<T> {

    Method factoryMethod;

    @Getter
    Class<? extends UserManagedMiddleSubject<T>> usersMiddleClass;

    @Getter
    Class<T> classUnderTest;

    public UserSuppliedCompiledMiddleClass(final Class<? extends UserManagedMiddleSubject<T>> usersMiddleClass, final Class<T> underTest) {
        super();
        this.usersMiddleClass = usersMiddleClass;
        this.factoryMethod = Utils.findFactoryMethod(usersMiddleClass);
        this.classUnderTest = underTest;
    }

    public String getSimpleName() {
        return usersMiddleClass.getName();
    }

    public void makeChildExtend(JavaClassSource child) {
        child.extendSuperType(usersMiddleClass);
    }

    public String getCanonicalName() {
        return usersMiddleClass.getCanonicalName();
    }

    public String getFactoryMethodName() {
        String canonicalName = factoryMethod.getDeclaringClass().getCanonicalName();
        return canonicalName + '.' + factoryMethod.getName();
    }

    @Override
    public String getPackage() {
        return usersMiddleClass.getPackageName();
    }

    @Override
    public String getClassUnderTestSimpleName() {
        return classUnderTest.getSimpleName();
    }
}



