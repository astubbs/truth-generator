package io.stubbs.truth.generator.integrationTests.capturedtemplates;

import com.google.common.truth.FailureMetadata;
import io.stubbs.truth.generator.SubjectFactoryMethod;
import io.stubbs.truth.generator.UserManagedMiddleSubject;
import io.stubbs.truth.generator.UserManagedTruth;
import io.stubbs.truth.generator.integrationTests.UserManagedSubjectsTest;
import io.stubbs.truth.generator.internal.model.MiddleClass;
import io.stubbs.truth.generator.testModel.IdCard;
import io.stubbs.truth.generator.testModel.IdCardParentSubject;

import javax.annotation.processing.Generated;

/**
 * Test user managed {@link MiddleClass} to ensure it is picked up and used in the graph.
 *
 * @see UserManagedSubjectsTest
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
