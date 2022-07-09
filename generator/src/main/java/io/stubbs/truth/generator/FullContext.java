package io.stubbs.truth.generator;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * @author Antony Stubbs
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class FullContext extends ReflectionContext {

    List<Path> sourceRoots;

    /**
     * Base path of test output directory - used for writing our generated subjects into subdirectories of
     * <p>
     * Can be though of as the OUTPUT side of the system.
     */
    Path testOutputDirectory;

    public FullContext(Path testOutputDirectory, List<Path> sourcePaths, ReflectionContext newReflectionContext) {
        this(testOutputDirectory, sourcePaths, newReflectionContext.getLoaders(), newReflectionContext.getBaseModelPackagesForReflectionScanning());
    }
    public FullContext(Path testOutputDirectory,
                       List<Path> sourcePaths,
                       List<ClassLoader> loaders,
                       Set<String> baseModelPackagesForReflectionScanning) {
        super(loaders, baseModelPackagesForReflectionScanning);
        this.testOutputDirectory = testOutputDirectory;
        this.sourceRoots = sourcePaths;
    }
}
