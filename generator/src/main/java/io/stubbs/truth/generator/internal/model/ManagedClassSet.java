package io.stubbs.truth.generator.internal.model;

import lombok.Value;
import org.jboss.forge.roaster.model.source.JavaClassSource;

/**
 * @author Antony Stubbs
 */
@Value
public class ManagedClassSet<T> {
    Class<T> sourceClass;
    JavaClassSource generatedClass;
}
