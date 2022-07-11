package io.stubbs.truth.generator.internal.model;

import io.stubbs.truth.generator.internal.model.AGeneratedClass.AGeneratedClassImpl;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jboss.forge.roaster.model.source.JavaClassSource;

/**
 * @author Antony Stubbs
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class ParentClass extends AGeneratedClassImpl {
    public ParentClass(JavaClassSource generated) {
        super(generated);
    }
}
