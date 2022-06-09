package io.stubbs.truth.generator.shaded.java.io;

import com.google.common.truth.FailureMetadata;
import io.stubbs.truth.generator.SubjectFactoryMethod;
import io.stubbs.truth.generator.UserManagedMiddleSubject;
import io.stubbs.truth.generator.UserManagedTruth;

import javax.annotation.processing.Generated;
import java.io.File;

/**
 * Optionally move this class into source control, and add your custom assertions here.
 *
 * <p>
 * If the system detects this class already exists, it won't attempt to generate a new one. Note that if the base
 * skeleton of this class ever changes, you won't automatically get it updated.
 *
 * @see File
 * @see FileParentSubject
 * @see FileChildSubject
 */
@UserManagedTruth(value = File.class)
@Generated("truth-generator")
public class FileSubject extends FileParentSubject implements UserManagedMiddleSubject<File> {

    /**
     * Returns an assertion builder for a {@link File} class.
     */
    @SubjectFactoryMethod
    public static Factory<FileSubject, File> files() {
        return FileSubject::new;
    }

    protected FileSubject(FailureMetadata failureMetadata, File actual) {
        super(failureMetadata, actual);
    }

    public void exists() {
        boolean exists = actual.exists();
//		if(!exists){
//			String absolutePath = actual.getAbsolutePath();
//			Files.walkFileTree()
//		}
        check("exists()").that(exists).isTrue();
    }

}
