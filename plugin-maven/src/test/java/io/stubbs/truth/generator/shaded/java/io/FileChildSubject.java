package io.stubbs.truth.generator.shaded.java.io;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Truth;

import javax.annotation.processing.Generated;
import java.io.File;

/**
 * Entry point for assertions for @{File}. Import the static accessor methods from this class and use them. Combines the
 * generated code from {@FileParentSubject}and the user code from {@FileSubject}.
 *
 * @see java.io.File
 * @see FileSubject
 * @see FileParentSubject
 */
@Generated("truth-generator")
public class FileChildSubject extends FileSubject {

    /**
     * This constructor should not be used, instead see the parent's.
     *
     * @see FileSubject
     */
    private FileChildSubject(FailureMetadata failureMetadata, File actual) {
        super(failureMetadata, actual);
    }

    /**
     * Convenience entry point for {@link File} assertions when being mixed with other "assertThat" assertion
     * libraries.
     *
     * @see #assertThat
     */
    public static FileSubject assertTruth(java.io.File actual) {
        return assertThat(actual);
    }

    /**
     * Entry point for {@link File} assertions.
     */
    public static FileSubject assertThat(java.io.File actual) {
        return Truth.assertAbout(files()).that(actual);
    }
}
