package io.stubbs.truth.generator.internal;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.MapSubject;
import io.stubbs.truth.generator.BaseSubjectExtension;
import io.stubbs.truth.generator.SubjectFactoryMethod;

import java.util.Map;

@BaseSubjectExtension(Map.class)
public class MyMapSubject extends MapSubject {

  private Map<?, ?> actual;

  protected MyMapSubject(FailureMetadata failureMetadata, Map<?, ?> actual) {
    super(failureMetadata, actual);
    this.actual = actual;
  }

  @SubjectFactoryMethod
  public static Factory<MyMapSubject, Map> maps() {
    return MyMapSubject::new;
  }

  public void containsKeys(Object... keys) {
    isNotNull();
    check("keySet()").that(actual.keySet()).containsAtLeastElementsIn(keys);
  }
}
