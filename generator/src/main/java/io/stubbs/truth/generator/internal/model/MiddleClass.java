package io.stubbs.truth.generator.internal.model;

import com.google.common.truth.Subject;
import org.jboss.forge.roaster.model.source.JavaClassSource;

/**
 * A {@link Subject} implementation.
 * <p>
 * TODO rename to "ManagedSubjectClass"?
 */
public interface MiddleClass<T> {

    public abstract String getSimpleName();

    public abstract void makeChildExtend(JavaClassSource child);

    public abstract String getCanonicalName();

    public abstract String getFactoryMethodName();

    public abstract String getPackage();

    public abstract String getClassUnderTestSimpleName();
}
