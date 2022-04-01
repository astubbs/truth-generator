package io.stubbs.truth.generator.internal;

import com.sun.tools.javac.platform.JDKPlatformProvider;
import com.sun.tools.javac.platform.PlatformDescription;
import com.sun.tools.javac.platform.PlatformProvider;
import com.sun.tools.javac.platform.PlatformUtils;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import org.junit.Assume;
import org.junit.Test;

import javax.lang.model.SourceVersion;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;

/**
 * These tests require a JDK override to be present
 *
 * @author Antony Stubbs
 * @see JDKOverrideAnalyser
 */
@Slf4j
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

    @Test
    public void madscientest() {

        JavaCompiler systemJavaCompiler = ToolProvider.getSystemJavaCompiler();
        Set<SourceVersion> sourceVersions = systemJavaCompiler.getSourceVersions();


        ServiceLoader<PlatformProvider> load = ServiceLoader.load(PlatformProvider.class);

        JDKPlatformProvider jdkPlatformProvider = new JDKPlatformProvider();
        Iterable<String> supportedPlatformNames = jdkPlatformProvider.getSupportedPlatformNames();

        PlatformDescription platformDescription = PlatformUtils.lookupPlatformDescription("");

        log.error("");
    }

}
