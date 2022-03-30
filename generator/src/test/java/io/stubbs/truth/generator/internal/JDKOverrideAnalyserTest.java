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
 *
 * @author Antony Stubbs
 * @see JDKOverrideAnalyser
 */
public class JDKOverrideAnalyserTest {

    File overrideJar = new File("C:\\Users\\anton\\.jdks\\adopt-openjdk-1.8.0_292\\jre\\lib\\rt.jar");
    Options options = Options.builder()
            .runtimeJavaClassSourceOverride(
                    Optional.of(overrideJar)
            ).build();


    @Test
    public void missing() {
        JDKOverrideAnalyser jdkOverrideAnalyser = new JDKOverrideAnalyser(options);
        Assume.assumeTrue(overrideJar.exists());
        Class<Duration> clazz = Duration.class;
        Method toSeconds = StreamEx.of(clazz.getMethods()).filter(x -> x.getName().contains("toSeconds")).findFirst().get();
        boolean contains = jdkOverrideAnalyser.doesOverrideClassContainMethod(clazz, toSeconds);
        assertThat(contains).isFalse();
    }

    @Test
    public void present() {
        JDKOverrideAnalyser jdkOverrideAnalyser = new JDKOverrideAnalyser(options);
        Assume.assumeTrue(overrideJar.exists());
        Class<Duration> clazz = Duration.class;
        Method toSeconds = StreamEx.of(clazz.getMethods()).filter(x -> x.getName().contains("toMillis")).findFirst().get();
        boolean contains = jdkOverrideAnalyser.doesOverrideClassContainMethod(clazz, toSeconds);
        assertThat(contains).isTrue();
    }

}
