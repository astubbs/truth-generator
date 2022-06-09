package io.stubbs.truth.generator;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * todo docs
 */
@AllArgsConstructor
@Value
public class Context {

    /**
     * todo docs
     */
    List<ClassLoader> loaders;

    /**
     * todo docs
     */
    //todo rename
    Set<String> modelPackages;

    /**
     * A Default context with no special class loaders, or package restrictions - useful for testing.
     */
    public Context() {
        this.loaders = Collections.emptyList();
        this.modelPackages = Collections.emptySet();
    }
}
