package io.stubbs.truth.generator.internal;

import com.google.common.truth.Truth8;
import io.stubbs.truth.generator.TestModelUtils;
import javassist.bytecode.ClassFile;
import javassist.bytecode.MethodInfo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.util.CtSym;
import org.eclipse.jdt.internal.compiler.util.JRTUtil;
import org.junit.Assume;
import org.junit.Test;

import javax.annotation.Nullable;
import javax.lang.model.SourceVersion;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

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

    @SneakyThrows
    @Test
    public void staticMethodInSetCopyOfNotIn9andIsIn10() {
        Set<Object> objects = Set.copyOf(Set.of());

        {
            ClassFile classRepresentation = getClassFileEclipse(9, Set.class);
            Optional<MethodInfo> method = TestModelUtils.findMethodJA(classRepresentation, "copyOf", 1);

            Truth8.assertThat(method).isEmpty();
        }

        {
            ClassFile classRepresentation = getClassFileEclipse(10, Set.class);
            Optional<MethodInfo> method = TestModelUtils.findMethodJA(classRepresentation, "copyOf", 1);

            Truth8.assertThat(method).isPresent();
        }
    }

    @SneakyThrows
    private @Nullable
    ClassFile getClassFileEclipse(int platformNumber, Class<?> clazz) {
        String platformName = Integer.toString(platformNumber);

        String qualifiedSigFilename = clazz.getTypeName().replace('.', '/') + ".sig";

        String releaseCode = CtSym.getReleaseCode(platformName);
        Path java_home = Path.of(System.getenv("JAVA_HOME"));
        CtSym ctSym = JRTUtil.getCtSym(java_home);

        Optional<Path> fullPath = Optional.ofNullable(ctSym.getFullPath(releaseCode, qualifiedSigFilename, clazz.getModule().getName()));

        if (fullPath.isEmpty())
            return null;

        byte[] fileBytes = ctSym.getFileBytes(fullPath.get());
        return new ClassFile(new DataInputStream(new ByteArrayInputStream(fileBytes)));
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
                ClassFile classRepresentation = getClassFileEclipse(9, clazz);
                Optional<MethodInfo> method = TestModelUtils.findMethodWithNoParamsJA(classRepresentation, methodName);

                Truth8.assertThat(method).isEmpty();

            }
        }

        {
            ClassFile classRepresentation = getClassFileEclipse(10, clazz);
            Optional<MethodInfo> method = TestModelUtils.findMethodWithNoParamsJA(classRepresentation, methodName);

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

        ClassFile classRepresentation = getClassFileEclipse(9, Duration.class);
        Optional<MethodInfo> toSeconds = TestModelUtils.findMethodWithNoParamsJA(classRepresentation, "toSeconds");

        Truth8.assertThat(toSeconds).isPresent();
    }

    @SneakyThrows
    @Test
    public void durationInJava8() {
        ClassFile classRepresentation = getClassFileEclipse(8, Duration.class);

        Optional<MethodInfo> toSeconds = TestModelUtils.findMethodWithNoParamsJA(classRepresentation, "toSeconds");

        Truth8.assertThat(toSeconds).isEmpty();
    }

    @SneakyThrows
    @Test
    public void java12NewInnerClasses() {
        Class<Enum.EnumDesc> enumDescClass = Enum.EnumDesc.class;

        ClassFile classRepresentation9 = getClassFileEclipse(9, Enum.EnumDesc.class);
        assertThat(classRepresentation9).isNull();

        ClassFile classRepresentation12 = getClassFileEclipse(12, Enum.EnumDesc.class);
        assertThat(classRepresentation12).isNotNull();
    }

    @SneakyThrows
    private ClassFileReader getClassFileEclipseDD(int platformNumber, Class<?> clazz) {
        String platformName = Integer.toString(platformNumber);
        String packageName = clazz.getPackageName();
        String className = clazz.getSimpleName() + ".";

        JavaCompiler systemJavaCompiler = ToolProvider.getSystemJavaCompiler();
        Set<SourceVersion> sourceVersions = systemJavaCompiler.getSourceVersions();


//        JDKPlatformProvider jdkPlatformProvider = new JDKPlatformProvider();
//        Iterable<String> supportedPlatformNames = jdkPlatformProvider.getSupportedPlatformNames();
//
//        PlatformDescription platformDescription = PlatformUtils.lookupPlatformDescription("13");

        String releaseCode = CtSym.getReleaseCode(platformName);
        Map<String, String> getenv = System.getenv();
        Properties properties = System.getProperties();
        Path java_home = Path.of(System.getenv("JAVA_HOME"));
        CtSym ctSym = JRTUtil.getCtSym(java_home);
        FileSystem fs = ctSym.getFs();
        boolean jre12Plus = ctSym.isJRE12Plus();
        List<Path> paths = ctSym.releaseRoots(releaseCode);
        String qualifiedSigFilename = clazz.getCanonicalName().replace('.', '/') + ".sig";
        Path fullPath = ctSym.getFullPath(releaseCode, qualifiedSigFilename, clazz.getModule().getName());
        byte[] fileBytes = ctSym.getFileBytes(fullPath);
        String moduleInJre12plus = ctSym.getModuleInJre12plus(releaseCode, qualifiedSigFilename);


        ClassFileReader classFileReader = new ClassFileReader(fileBytes, clazz.getCanonicalName().toCharArray());
        return classFileReader;
//        IBinaryMethod[] methods = classFileReader.getMethods();
//        Arrays.stream(methods).filter(x->x.toString().contains())
//
//        ClassFile eclipseClassRepresentation = new ClassFile(new DataInputStream(new ByteArrayInputStream(fileBytes)));
//        return eclipseClassRepresentation;
    }


//    private ClassFile getClassFile(int platformNumber, Class<?> clazz) throws PlatformProvider.PlatformNotSupported, IOException {
//        String platformName = Integer.toString(platformNumber);
//        String packageName = clazz.getPackageName();
//        String className = clazz.getSimpleName() + ".";
//
//        JavaCompiler systemJavaCompiler = ToolProvider.getSystemJavaCompiler();
//        Set<SourceVersion> sourceVersions = systemJavaCompiler.getSourceVersions();
//
//
////        JDKPlatformProvider jdkPlatformProvider = new JDKPlatformProvider();
////        Iterable<String> supportedPlatformNames = jdkPlatformProvider.getSupportedPlatformNames();
////
////        PlatformDescription platformDescription = PlatformUtils.lookupPlatformDescription("13");
//
//        String releaseCode = CtSym.getReleaseCode(platformName);
//        Map<String, String> getenv = System.getenv();
//        Properties properties = System.getProperties();
//        Path java_home = Path.of(System.getenv("JAVA_HOME"));
//        CtSym ctSym = JRTUtil.getCtSym(java_home);
//        FileSystem fs = ctSym.getFs();
//        boolean jre12Plus = ctSym.isJRE12Plus();
//        List<Path> paths = ctSym.releaseRoots(releaseCode);
//        String qualifiedSigFilename = clazz.getCanonicalName().replace('.', '/') + ".sig";
//        Path fullPath = ctSym.getFullPath(releaseCode, qualifiedSigFilename, clazz.getModule().getName());
//        byte[] fileBytes = ctSym.getFileBytes(fullPath);
//        String moduleInJre12plus = ctSym.getModuleInJre12plus(releaseCode, qualifiedSigFilename);
//
//        ClassFile eclipseClassRepresentation = new ClassFile(new DataInputStream(new ByteArrayInputStream(fileBytes)));
//
//        ServiceLoader<PlatformProvider> load = ServiceLoader.load(PlatformProvider.class);
//
//        PlatformProvider first = load.findFirst().get();
//        Iterable<String> supportedPlatformNames = first.getSupportedPlatformNames();
//        PlatformDescription platform = first.getPlatform(platformName, "");
//        JavaFileManager fileManager = platform.getFileManager();
//
//
//        List<FileObject> fileObjects = StreamEx.of(StandardLocation.values()).toFlatList(standardLocation -> {
//            if (standardLocation.isModuleOrientedLocation())
//                return List.of();
//            {
//                String moduleName = null;
//                try {
//                    moduleName = fileManager.inferModuleName(standardLocation);
//                    if (moduleName != null) {
//                        JavaFileManager.Location locationForModule = fileManager.getLocationForModule(standardLocation, moduleName);
//                        FileObject fileForInput = fileManager.getFileForInput(standardLocation, packageName, clazz.getSimpleName());
//                        return fileForInput != null ? List.of(fileForInput) : List.of();
//                    } else {
//                        return List.of();
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    return List.of();
//                }
//
//            }
////
////            try {
////                FileObject fileForInput = fileManager.getFileForInput(standardLocation, packageName, clazz.getSimpleName());
////                return fileForInput != null ? List.of(fileForInput) : List.of();
////            } catch (IOException e) {
////                e.printStackTrace();
////                return List.of();
////            }
//        });
//
//
//        Iterable<JavaFileObject> list = fileManager.list(StandardLocation.PLATFORM_CLASS_PATH, packageName, EnumSet.of(JavaFileObject.Kind.OTHER), false);
//        List<JavaFileObject> iterables = StreamEx.of(list.iterator()).toList();
//        Optional<JavaFileObject> first1 = StreamEx.of(list.iterator()).filter(x -> x.getName().contains(className)).findFirst();
//        JavaFileObject durationJFO = first1.get();
//        JavaFileObject.Kind kind = durationJFO.getKind();
//        URI uri = durationJFO.toUri();
//        Modifier accessLevel = durationJFO.getAccessLevel();
//        NestingKind nestingKind = durationJFO.getNestingKind();
//
//        InputStream inputStream = durationJFO.openInputStream();
//        ClassFile classRepresentation = new ClassFile(new DataInputStream(inputStream));
//
//
////        JavaFileManager fm = pp.getPlatform(release, "").getFileManager();
//
//
//        return classRepresentation;
//    }

}
