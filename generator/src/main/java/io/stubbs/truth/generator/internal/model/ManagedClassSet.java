package io.stubbs.truth.generator.internal.model;

import lombok.Value;
import org.jboss.forge.roaster.model.source.JavaClassSource;

@Value
public class ManagedClassSet<T> {
    Class<T> sourceClass;
    JavaClassSource generatedClass;
}
