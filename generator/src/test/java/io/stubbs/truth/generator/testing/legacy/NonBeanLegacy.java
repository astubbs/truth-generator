package io.stubbs.truth.generator.testing.legacy;


import lombok.Builder;
import lombok.RequiredArgsConstructor;

/**
 * @author Antony Stubbs
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

    public void aVoidReturningMethodInALegacyBean() {
    }
}
