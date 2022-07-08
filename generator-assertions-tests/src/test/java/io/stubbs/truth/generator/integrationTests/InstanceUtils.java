package io.stubbs.truth.generator.integrationTests;

public class InstanceUtils {
    static <T> T createInstance(Class<T> clazz) {
        return GeneratedAssertionTests.PODAM_FACTORY.manufacturePojo(clazz);
    }
}
