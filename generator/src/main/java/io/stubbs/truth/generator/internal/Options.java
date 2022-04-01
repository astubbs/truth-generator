package io.stubbs.truth.generator.internal;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Optional;

/**
 * @author Antony Stubbs
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Getter
@Builder(toBuilder = true)
@ToString
public class Options {
    @Setter
    private static Options instance;
    @Builder.Default
    boolean useHasInsteadOfGet = false;
    @Builder.Default
    boolean useGetterForLegacyClasses = false;
    @Builder.Default
    boolean compilationTargetLowerThanNine = false;
    @Builder.Default
    Optional<Integer> releaseTarget = Optional.empty();
    /**
     * Marks whether to try to find all referenced types from the source types, to generate Subjects for all of them,
     * and use them all in the Subject tree.
     */
    @Builder.Default
    private boolean recursive = true;

    public static Options get() {
        return instance;
    }

    /**
     * Used for testing
     */
    public static void setDefaultInstance() {
        setInstance(Options.builder().build());
    }
}
