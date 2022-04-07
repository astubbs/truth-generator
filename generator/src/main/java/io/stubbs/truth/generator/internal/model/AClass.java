package io.stubbs.truth.generator.internal.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.jboss.forge.roaster.model.source.JavaClassSource;

/**
 * @author Antony Stubbs
 */
public interface AClass {

    JavaClassSource getGenerated();

    @Getter
    @RequiredArgsConstructor
    @SuperBuilder
    class AGeneratedClassImpl implements AClass {

        protected final JavaClassSource generated;

        @Override
        public JavaClassSource getGenerated() {
            return generated;
        }
    }
}
