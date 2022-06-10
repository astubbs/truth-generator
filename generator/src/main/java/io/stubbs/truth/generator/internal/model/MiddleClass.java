package io.stubbs.truth.generator.internal.model;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.jboss.forge.roaster.model.source.JavaClassSource;

/**
 * TODO rename to "ManagedSubjectClass"?
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public abstract class MiddleClass<T> {

    public abstract String getSimpleName();

    public abstract void makeChildExtend(JavaClassSource child);

    public abstract String getCanonicalName();

    public abstract String getFactoryMethodName();

    public abstract String getPackage();

    public abstract String getClassUnderTestSimpleName();
}
