package io.stubbs.truth.generator.internal.model;

import lombok.*;
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

    @Getter
    Class<T> classUnderTest;

    public abstract String getSimpleName();

    public abstract void makeChildExtend(JavaClassSource child);

    public abstract String getCanonicalName();

    public abstract String getFactoryMethodName();

    public abstract String getPackage();
}
