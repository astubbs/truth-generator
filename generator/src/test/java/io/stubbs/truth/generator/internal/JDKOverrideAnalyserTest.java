package io.stubbs.truth.generator.internal;

import one.util.streamex.StreamEx;
import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;

/**
 * These tests require a JDK override to be present
 */
public class JDKOverrideAnalyserTest {

    Options options = Options.builder()
            .runtimeJavaClassSourceOverride(
                    Optional.of(new File("C:\\Users\\anton\\.jdks\\adopt-openjdk-1.8.0_292\\jre\\lib\\rt.jar"))
            ).build();

    @Test
    public void missing() {
        JDKOverrideAnalyser jdkOverrideAnalyser = new JDKOverrideAnalyser(options);
        Assume.assumeTrue(jdkOverrideAnalyser.isOverrideConfigured());
        Class<Duration> clazz = Duration.class;
        Method toSeconds = StreamEx.of(clazz.getMethods()).filter(x -> x.getName().contains("toSeconds")).findFirst().get();
        boolean contains = jdkOverrideAnalyser.doesOverrideClassContainMethod(clazz, toSeconds);
        assertThat(contains).isFalse();
    }

    @Test
    public void present() {
        JDKOverrideAnalyser jdkOverrideAnalyser = new JDKOverrideAnalyser(options);
        Assume.assumeTrue(jdkOverrideAnalyser.isOverrideConfigured());
        Class<Duration> clazz = Duration.class;
        Method toSeconds = StreamEx.of(clazz.getMethods()).filter(x -> x.getName().contains("toMillis")).findFirst().get();
        boolean contains = jdkOverrideAnalyser.doesOverrideClassContainMethod(clazz, toSeconds);
        assertThat(contains).isTrue();
    }

}
