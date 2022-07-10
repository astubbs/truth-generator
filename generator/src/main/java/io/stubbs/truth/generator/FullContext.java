package io.stubbs.truth.generator;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * The context the system is running in.
 *
 * @author Antony Stubbs
 * @see ReflectionContext
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class FullContext extends ReflectionContext {

    /**
     * Source directory roots to work in
     */
    List<Path> sourceRoots;

    /**
     * Base path of test output directory - used for writing our generated subjects into subdirectories of
     * <p>
     * Can be though of as the OUTPUT side of the system.
     */
    Path testOutputDirectory;

    public FullContext(Path newTestOutputDirectory, List<Path> sourcePaths, ReflectionContext newReflectionContext) {
        this(newTestOutputDirectory, sourcePaths, newReflectionContext.getLoaders(), newReflectionContext.getBaseModelPackagesForReflectionScanning());
    }

    public FullContext(Path newTestOutputDirectory,
                       List<Path> sourcePaths,
                       List<ClassLoader> loaders,
                       Set<String> baseModelPackagesForReflectionScanning) {
        super(loaders, baseModelPackagesForReflectionScanning);
        this.testOutputDirectory = newTestOutputDirectory;
        this.sourceRoots = sourcePaths;
    }
}
