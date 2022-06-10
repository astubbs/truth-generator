package io.stubbs.truth.generator.capturedtemplates;

import com.google.common.truth.FailureMetadata;
import io.stubbs.truth.generator.SubjectFactoryMethod;
import io.stubbs.truth.generator.UserManagedMiddleSubject;
import io.stubbs.truth.generator.UserManagedTruth;
import io.stubbs.truth.generator.testModel.IdCard;
import io.stubbs.truth.generator.testModel.IdCardParentSubject;

import javax.annotation.processing.Generated;

/**
 * Optionally move this class into source control, and add your custom assertions here.
 *
 * <p>
 * If the system detects this class already exists, it won't attempt to generate a new one. Note that if the base
 * skeleton of this class ever changes, you won't automatically get it updated.
 *
 * @see IdCard
 * @see IdCardParentSubject
 * @see IdCardChildSubject
 */
@UserManagedTruth(IdCard.class)
@Generated(value = "io.stubbs.truth.generator.internal.TruthGenerator", date = "1970-01-01T00:00:00Z")
public class MyIdCardSubject extends IdCardParentSubject implements UserManagedMiddleSubject {

	protected MyIdCardSubject(FailureMetadata failureMetadata, IdCard actual) {
		super(failureMetadata, actual);
	}

	/**
	 * Returns an assertion builder for a {@link IdCard} class.
	 */
	@SubjectFactoryMethod
	public static Factory<MyIdCardSubject, IdCard> idCards() {
		return MyIdCardSubject::new;
	}
}
