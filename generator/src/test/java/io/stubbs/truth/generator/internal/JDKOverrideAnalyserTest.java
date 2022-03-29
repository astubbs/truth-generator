package io.stubbs.truth.generator.internal;

import one.util.streamex.StreamEx;
import org.junit.Test;

import java.lang.reflect.Method;
import java.time.Duration;

import static com.google.common.truth.Truth.assertThat;

public class JDKOverrideAnalyserTest {

    @Test
    public void missing() {
        JDKOverrideAnalyser JDKOverrideAnalyser = new JDKOverrideAnalyser();
        Class<Duration> clazz = Duration.class;
        Method toSeconds = StreamEx.of(clazz.getMethods()).filter(x -> x.getName().contains("toSeconds")).findFirst().get();
        boolean contains = JDKOverrideAnalyser.doesOverrideClassContainMethod(clazz, toSeconds);
        assertThat(contains).isFalse();
    }

    @Test
    public void present() {
        JDKOverrideAnalyser JDKOverrideAnalyser = new JDKOverrideAnalyser();
        Class<Duration> clazz = Duration.class;
        Method toSeconds = StreamEx.of(clazz.getMethods()).filter(x -> x.getName().contains("toMillis")).findFirst().get();
        boolean contains = JDKOverrideAnalyser.doesOverrideClassContainMethod(clazz, toSeconds);
        assertThat(contains).isTrue();
    }

}
