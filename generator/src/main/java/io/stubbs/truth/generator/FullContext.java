package io.stubbs.truth.generator;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = true)
public class FullContext extends ReflectionContext {

    /**
     * Base path of test output directory - used for writing our generated subjects into subdirectories of
     * <p>
     * Can be though of as the OUTPUT side of the system.
     */
    Path testOutputDirectory;

    public FullContext(Path testOutputDirectory, List<ClassLoader> loaders, Set<String> baseModelPackagesFroScanning) {
        super(loaders, baseModelPackagesFroScanning);
        this.testOutputDirectory = testOutputDirectory;
    }

    public FullContext(Path testOutputDirectory, Set<String> baseModelPackagesFroScanning) {
        super(baseModelPackagesFroScanning);
        this.testOutputDirectory = testOutputDirectory;
    }

    public FullContext(Path testOutputDirectory, String baseModelPackageFroScanning) {
        super(baseModelPackageFroScanning);
        this.testOutputDirectory = testOutputDirectory;

    }

    public FullContext(Path testOutputDirectory, ReflectionContext newReflectionContext) {
        this(testOutputDirectory, newReflectionContext.getLoaders(), newReflectionContext.getBaseModelPackagesFroScanning());
    }
}
