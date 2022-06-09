package io.stubbs.truth.generator;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Holds the context in which the generator is executing - namely special class loaders (used by the plugin) and base
 * model packages which should be scanned.
 */
@AllArgsConstructor
@Value
public class Context {

    /**
     * Base path of test output directory - used for writing our generated subjects into subdirectories of
     * <p>
     * Can be though of as the OUTPUT side of the system.
     */
    Path testOutputDirectory;

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
    public Context(Path testOutputDirectory, Set<String> baseModelPackagesFroScanning) {
        this.testOutputDirectory = testOutputDirectory;
        this.loaders = Collections.emptyList();
//        this.baseModelPackagesFroScanning = Collections.emptySet();
        this.baseModelPackagesFroScanning = baseModelPackagesFroScanning;

    }

    public Context(Path testOutputDirectory, String baseModelPackageFroScanning) {
        this(testOutputDirectory, Set.of(baseModelPackageFroScanning));
    }
}
