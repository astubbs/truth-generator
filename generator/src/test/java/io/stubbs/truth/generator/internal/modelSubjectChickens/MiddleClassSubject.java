package io.stubbs.truth.generator.internal.modelSubjectChickens;

import com.google.common.truth.FailureMetadata;
import io.stubbs.truth.generator.UserManagedTruth;
import io.stubbs.truth.generator.internal.model.MiddleClass;

import javax.annotation.processing.Generated;

// in VCS as we're still in the chicken phase of what comes first - stable maven plugin to generate this for the build before we can remove

/**
 * Optionally move this class into source control, and add your custom assertions here.
 *
 * <p>
 * If the system detects this class already exists, it won't attempt to generate a new one. Note that if the base
 * skeleton of this class ever changes, you won't automatically get it updated.
 *
 * @see MiddleClassParentSubject
 */
@UserManagedTruth(value = MiddleClass.class)
@Generated("truth-generator")
public class MiddleClassSubject extends MiddleClassParentSubject {

  protected MiddleClassSubject(FailureMetadata failureMetadata,
                               MiddleClass actual) {
    super(failureMetadata, actual);
  }

  /**
   * Returns an assertion builder for a {@link MiddleClass} class.
   */
  public static Factory<MiddleClassSubject, MiddleClass> middleClasses() {
    return MiddleClassSubject::new;
  }
}
