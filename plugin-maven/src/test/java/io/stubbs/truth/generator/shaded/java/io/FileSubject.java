package io.stubbs.truth.generator.shaded.java.io;

import com.google.common.truth.FailureMetadata;
import java.io.File;

import io.stubbs.truth.generator.UserManagedTruth;

import javax.annotation.processing.Generated;

/**
 * Optionally move this class into source control, and add your custom
 * assertions here.
 * 
 * <p>
 * If the system detects this class already exists, it won't attempt to generate
 * a new one. Note that if the base skeleton of this class ever changes, you
 * won't automatically get it updated.
 * 
 * @see File
 * @see FileParentSubject
 */
@UserManagedTruth(clazz = File.class)
@Generated("truth-generator")
public class FileSubject extends FileParentSubject {

	protected FileSubject(FailureMetadata failureMetadata, File actual) {
		super(failureMetadata, actual);
	}

	/**
	 * Returns an assertion builder for a {@link File} class.
	 */
	public static Factory<FileSubject, File> files() {
		return FileSubject::new;
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
