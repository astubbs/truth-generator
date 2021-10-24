package io.stubbs.truth.generator.internal.modelSubjectChickens;

import com.google.common.truth.ClassSubject;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import io.stubbs.truth.generator.shaded.org.jboss.forge.roaster.model.sourceChickens.JavaClassSourceSubject;

import javax.annotation.processing.Generated;

// in VCS as we're still in the chicken phase of what comes first - stable maven plugin to generate this for the build before we can remove

/**
 * Truth Subject for the {@link ThreeSystem}.
 * <p>
 * Note that this class is generated / managed, and will change over time. So any changes you might make will be
 * overwritten.
 *
 * @see ThreeSystem
 * @see ThreeSystemSubject
 * @see ThreeSystemChildSubject
 */
@Generated("truth-generator")
public class ThreeSystemParentSubject extends Subject {

  protected final ThreeSystem actual;

  protected ThreeSystemParentSubject(FailureMetadata failureMetadata,
                                     ThreeSystem actual) {
    super(failureMetadata, actual);
    this.actual = actual;
  }

  public JavaClassSourceSubject hasChild() {
    isNotNull();
    return check("getChild").about(JavaClassSourceSubject.javaClassSources()).that(actual.getChild());
  }

  public ParentClassSubject hasParent() {
    isNotNull();
    return check("getParent").about(ParentClassSubject.parentClasses()).that(actual.getParent());
  }

  public MiddleClassSubject hasMiddle() {
    isNotNull();
    return check("getMiddle").about(MiddleClassSubject.middleClasses()).that(actual.getMiddle());
  }

  public ClassSubject hasClassUnderTest() {
    isNotNull();
    return check("getClassUnderTest").that(actual.getClassUnderTest());
  }
}
