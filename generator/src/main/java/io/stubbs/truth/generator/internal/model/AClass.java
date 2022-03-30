package io.stubbs.truth.generator.internal.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.jboss.forge.roaster.model.source.JavaClassSource;

@Getter
@RequiredArgsConstructor
@SuperBuilder
public class AClass {
    protected final JavaClassSource generated;
}
