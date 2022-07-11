package io.stubbs.truth.generator.internal.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.jboss.forge.roaster.model.source.JavaClassSource;

/**
 * @author Antony Stubbs
 */
public interface AGeneratedClass {

    JavaClassSource getGenerated();

    @Getter
    @RequiredArgsConstructor
    @SuperBuilder
    class AGeneratedClassImpl implements AGeneratedClass {

        protected final JavaClassSource generated;

        @Override
        public JavaClassSource getGenerated() {
            return generated;
        }
    }
}
