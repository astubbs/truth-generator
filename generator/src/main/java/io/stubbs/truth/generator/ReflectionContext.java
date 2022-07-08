package io.stubbs.truth.generator;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Holds the context in which the generator is executing - namely special class loaders (used by the plugin) and base
 * model packages which should be scanned.
 *
 * @author Antony Stubbs
 */
@Getter
@ToString
@EqualsAndHashCode
//@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ReflectionContext {

    public static final String BASE_TG_SUBJECTS_PACKAGE = "io.stubbs.truth.generator.subjects";
    public static final String GOOGLE_TRUTH_SUBJECTS_PACKAGE = "com.google.truth";

    /**
     * Special class loaders (used by the plugin)
     */
    List<ClassLoader> loaders;

    /**
     * Base model packages which should be scanned via reflection.
     * <p>
     * Can be though of as the INPUT side of the system.
     */
    Set<String> baseModelPackagesForReflectionScanning;

    public ReflectionContext(List<ClassLoader> loaders, Set<String> baseModelPackagesForReflectionScanning) {
        this.loaders = loaders;
        // todo copy?
        this.baseModelPackagesForReflectionScanning = new HashSet<>(baseModelPackagesForReflectionScanning);
        var systemPackages = Set.of(BASE_TG_SUBJECTS_PACKAGE, GOOGLE_TRUTH_SUBJECTS_PACKAGE);
        this.baseModelPackagesForReflectionScanning.addAll(systemPackages);
    }

    /**
     * A Default context with no special class loaders, or package restrictions - useful for testing.
     */
    public ReflectionContext(Set<String> baseModelPackagesFroScanning) {
        this(Collections.emptyList(), baseModelPackagesFroScanning);
    }

    public ReflectionContext(String baseModelPackageFroScanning) {
        this(Set.of(baseModelPackageFroScanning));
    }

}

