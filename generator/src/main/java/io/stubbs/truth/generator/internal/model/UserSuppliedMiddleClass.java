package io.stubbs.truth.generator.internal.model;

import com.google.common.truth.Subject;
import io.stubbs.truth.generator.UserManagedTruth;
import io.stubbs.truth.generator.internal.Utils;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.jboss.forge.roaster.model.source.JavaClassSource;

import java.lang.reflect.Method;

/**
 * @see UserManagedTruth
 */
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@ToString
public class UserSuppliedMiddleClass<T> extends MiddleClass<T> {

    Method factoryMethod;

    @Getter
    Class<?> usersMiddleClass;

    public UserSuppliedMiddleClass(final Class<? extends Subject> usersMiddleClass, final Class<T> underTest) {
        super(underTest);
        this.usersMiddleClass = usersMiddleClass;
        this.factoryMethod = Utils.findFactoryMethod(usersMiddleClass);
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
        return factoryMethod.getName();
    }

    @Override
    public String getPackage() {
        return usersMiddleClass.getPackageName();
    }
}
