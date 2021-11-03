package io.stubbs.truth.generator.testing.legacy;


import lombok.Builder;
import lombok.RequiredArgsConstructor;

// TODO remove - should use from other module

/**
 * Used for testing support for classes that aren't bean compliant
 */
@Builder(toBuilder = true)
@RequiredArgsConstructor
public class NonBeanLegacy {
  final int age;
  final String name;

  public int age() {
    return age;
  }

  public String name() {
    return name;
  }
}
