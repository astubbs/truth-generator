package io.stubbs.truth.generator.internal;

import com.google.common.truth.Truth8;
import javassist.bytecode.ClassFile;
import javassist.bytecode.MethodInfo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.truth.Truth.assertThat;

/**
 * These tests require a JDK override to be present
 * <p>
 * {@code https://gunnarmorling.github.io/jdk-api-diff/jdk10-jdk11-api-diff.html}
 *
 * @author Antony Stubbs
 * @see JDKOverrideAnalyser
 */
@Slf4j
public class JDKOverrideAnalyserTest {

    JDKOverrideAnalyser jdkOverrideAnalyser;

    @Before
    public void setup() {
        jdkOverrideAnalyser = new JDKOverrideAnalyser(Options.builder().build());
    }

    @Test
    public void missing() {
        Class<Duration> clazz = Duration.class;
        jdkOverrideAnalyser = new JDKOverrideAnalyser(Options.builder().releaseTarget(Optional.of(8)).build());
        Method toSeconds = StreamEx.of(clazz.getMethods()).filter(x -> x.getName().contains("toSeconds")).findFirst().get();
        boolean contains = jdkOverrideAnalyser.doesOverrideClassContainMethod(clazz, toSeconds);
        assertThat(contains).isFalse();
    }

    @Test
    public void present() {
        Class<Duration> clazz = Duration.class;
        jdkOverrideAnalyser = new JDKOverrideAnalyser(Options.builder().releaseTarget(Optional.of(9)).build());
        Method toSeconds = StreamEx.of(clazz.getMethods()).filter(x -> x.getName().contains("toMillis")).findFirst().get();
        boolean contains = jdkOverrideAnalyser.doesOverrideClassContainMethod(clazz, toSeconds);
        assertThat(contains).isTrue();
    }

    @SneakyThrows
    @Test
    public void staticMethodInSetCopyOfNotIn9andIsIn10() {
        {
            ClassFile classRepresentation = jdkOverrideAnalyser.getClassFileEclipse(9, Set.class);
            Optional<MethodInfo> method = JDKOverrideAnalyser.findMethodJA(classRepresentation, "copyOf", 1);

            Truth8.assertThat(method).isEmpty();
        }

        {
            ClassFile classRepresentation = jdkOverrideAnalyser.getClassFileEclipse(10, Set.class);
            Optional<MethodInfo> method = JDKOverrideAnalyser.findMethodJA(classRepresentation, "copyOf", 1);

            Truth8.assertThat(method).isPresent();
        }
    }

    @SneakyThrows
    @Test
    public void OptionalOrElseThrow9and10() {
        String s = Optional.of("").orElseThrow();

        String methodName = "orElseThrow";
        Class<Optional> clazz = Optional.class;

        test9and10MissingPresent(methodName, clazz);
    }

    @SneakyThrows
    private void test9and10MissingPresent(String methodName, Class<?> clazz) {
        {
            {
                ClassFile classRepresentation = jdkOverrideAnalyser.getClassFileEclipse(9, clazz);
                Optional<MethodInfo> method = JDKOverrideAnalyser.findMethodWithNoParamsJA(classRepresentation, methodName);

                Truth8.assertThat(method).isEmpty();

            }
        }

        {
            ClassFile classRepresentation = jdkOverrideAnalyser.getClassFileEclipse(10, clazz);
            Optional<MethodInfo> method = JDKOverrideAnalyser.findMethodWithNoParamsJA(classRepresentation, methodName);

            Truth8.assertThat(method).isPresent();
        }
    }

    @SneakyThrows
    @Test
    public void Collector() {
        String s = Optional.of("").orElseThrow();

        String methodName = "toUnmodifiableList";
        Class<Collectors> clazz = Collectors.class;

        test9and10MissingPresent(methodName, clazz);
    }

    @SneakyThrows
    @Test
    public void durationInJava9() {
        long l = Duration.ofSeconds(0).toSeconds();

        ClassFile classRepresentation = jdkOverrideAnalyser.getClassFileEclipse(9, Duration.class);
        Optional<MethodInfo> toSeconds = JDKOverrideAnalyser.findMethodWithNoParamsJA(classRepresentation, "toSeconds");

        Truth8.assertThat(toSeconds).isPresent();
    }

    @SneakyThrows
    @Test
    public void durationInJava8() {
        ClassFile classRepresentation = jdkOverrideAnalyser.getClassFileEclipse(8, Duration.class);

        Optional<MethodInfo> toSeconds = JDKOverrideAnalyser.findMethodWithNoParamsJA(classRepresentation, "toSeconds");

        Truth8.assertThat(toSeconds).isEmpty();
    }

    /**
     * https://gunnarmorling.github.io/jdk-api-diff/jdk10-jdk11-api-diff.html
     */
    @SneakyThrows
//    @Test
    // todo can't get a configuration for this test that works well between jdk runtimes, but satisfied we don't really need this coverage
    public void java11NewInnerClasses() {
        Class<?> clazz = HttpRequest.Builder.class;

        ClassFile classInJava9 = jdkOverrideAnalyser.getClassFileEclipse(9, clazz);
        assertThat(classInJava9).isNull();

        ClassFile classInJava11 = jdkOverrideAnalyser.getClassFileEclipse(11, clazz);
        assertThat(classInJava11).isNotNull();
    }

}
