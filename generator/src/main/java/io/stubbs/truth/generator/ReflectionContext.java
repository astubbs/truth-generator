package io.stubbs.truth.generator;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Collections;
import java.util.List;
import java.util.Set;


/**
 * Holds the context in which the generator is executing - namely special class loaders (used by the plugin) and base
 * model packages which should be scanned.
 */
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ReflectionContext {

    /**
     * Special class loaders (used by the plugin)
     */
    List<ClassLoader> loaders;

    /**
     * Base model packages which should be scanned.
     * <p>
     * Can be though of as the INPUT side of the system.
     */
    Set<String> baseModelPackagesFroScanning;

    /**
     * A Default context with no special class loaders, or package restrictions - useful for testing.
     */
    public ReflectionContext(Set<String> baseModelPackagesFroScanning) {
        this.loaders = Collections.emptyList();
        this.baseModelPackagesFroScanning = baseModelPackagesFroScanning;

    }

    public ReflectionContext(String baseModelPackageFroScanning) {
        this(Set.of(baseModelPackageFroScanning));
    }
}

