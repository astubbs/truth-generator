package io.stubbs.truth.generator.internal;

import javassist.bytecode.AccessFlag;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.ClassFile;
import javassist.bytecode.MethodInfo;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.internal.compiler.util.CtSym;
import org.eclipse.jdt.internal.compiler.util.JRTUtil;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static io.stubbs.truth.generator.internal.Utils.msg;
import static java.util.Optional.ofNullable;

/**
 * Used to test if a provided class contains a given method, when the class byte data is loaded from a configured jar,
 * instead of the runtime JDK.
 *
 * @author Antony Stubbs
 * @see Options#getReleaseTarget()
 */
@Slf4j
@RequiredArgsConstructor
public class JDKOverrideAnalyser {

    private final Options options;

    private static final CtSym ctSym;

    static {
        Path javaHome = Path.of(System.getenv("JAVA_HOME"));

        if (!(javaHome.toFile().exists() && javaHome.toFile().isDirectory())) {
            throw new TruthGeneratorRuntimeException(msg("Cannot look up JAVA_HOME env variable. Found {}, and it either doesn't exist or isn't a directory", javaHome));
        }

        Path ctsymPath = javaHome.resolve("lib").resolve("ct.sym");
        log.error("Using {} as home for ct.sym located at {} which exists? {}", javaHome, ctsymPath, ctsymPath.toFile().exists());

        try {
            ctSym = JRTUtil.getCtSym(javaHome);
        } catch (IOException e) {
            throw new TruthGeneratorRuntimeException("Cannot instantiate CtSym abstraction", e);
        }
    }

    public static Optional<MethodInfo> findMethodWithNoParamsJA(ClassFile classRepresentation, String name) {
        return findMethodJA(classRepresentation, name, 0);
    }

    public boolean doesOverrideClassContainMethod(Class<?> clazz, Method method) {
        Optional<ClassFile> classModel = ofNullable(getClassFileEclipse(options.getReleaseTarget().get(), clazz));

        return classModel
                .filter(model ->
                        doesContainsMethod(model, method))
                .isPresent();
    }

//    private Optional<ClassFile> getCachedClass(Class<?> clazz) {
//        var cachedModel = ofNullable(cache.get(clazz));
//        if (cachedModel.isPresent()) {
//            return cachedModel;
//        } else {
//            Optional<ClassFile> aClass = getClass(clazz);
//            aClass.ifPresent(classFile -> cache.put(clazz, classFile));
//            return aClass;
//        }
//    }

    @SneakyThrows
    @Nullable
    protected ClassFile getClassFileEclipse(int platformNumber, Class<?> clazz) {
        String platformName = Integer.toString(platformNumber);

        String qualifiedSigFilename = clazz.getTypeName().replace('.', '/') + ".sig";

        String releaseCode = CtSym.getReleaseCode(platformName);
        Optional<Path> fullPath = Optional.ofNullable(ctSym.getFullPath(releaseCode, qualifiedSigFilename, clazz.getModule().getName()));

        if (fullPath.isEmpty()) {
            log.info("ct.sym look up failed for class {} in jdk version {} with sig address {}", clazz, platformName, qualifiedSigFilename);
            return null;
        }

        Method getCachedReleasePaths = CtSym.class.getDeclaredMethod("getCachedReleasePaths", String.class);
        getCachedReleasePaths.setAccessible(true);
        Map<String, Path> invoke = (Map) getCachedReleasePaths.invoke(ctSym, releaseCode);
        Stream<String> stringStream = invoke.keySet().stream().filter(x -> x.contains(clazz.getSimpleName()));
        stringStream.forEach(x -> log.info("Found: {}", x));


        byte[] fileBytes = ctSym.getFileBytes(fullPath.get());
        return new ClassFile(new DataInputStream(new ByteArrayInputStream(fileBytes)));
    }

//    private Optional<ClassFile> getClass(Class<?> clazz) {
//        if (!isOverrideConfigured()) {
//            throw new IllegalStateException("No class source override configured");
//        }
//
//        try {
//            String name = clazz.getCanonicalName();
//            //noinspection OptionalGetWithoutIsPresent - checked in isOverrideConfigured
//            var locate = ClassFileLocator.ForJarFile
//                    .of(options.getReleaseTarget().get()) // NOSONAR
//                    .locate(name);
//            byte[] resolve = locate.resolve();
//            ClassFile classRepresentation = new ClassFile(new DataInputStream(new ByteArrayInputStream(resolve)));
//            return of(classRepresentation);
//        } catch (IOException e) {
//            log.error("Can't load class override for {} from {}", clazz, options.getReleaseTarget().get()); // NOSONAR
//            return empty();
//        }
//    }

    private boolean doesContainsMethod(ClassFile model, Method method) {
        Optional<MethodInfo> methodJA = findMethodJA(model, method.getName(), method.getParameterCount());
        return methodJA.isPresent() && AccessFlag.isPublic(methodJA.get().getAccessFlags());
    }

    public static Optional<MethodInfo> findMethodJA(ClassFile classRepresentation, String name, int paramCount) {
        List<MethodInfo> methods = classRepresentation.getMethods();
        return methods.stream().filter(x -> {
            if (x.getName().equals(name)) {

                List<AttributeInfo> attributes = x.getAttributes();

                if (attributes.isEmpty() && paramCount == 0) {
                    return true;
                }

                String descriptor = x.getDescriptor();
                String params = StringUtils.substringBetween(descriptor, "(", ")");
                int hackyParamCount = StringUtils.countMatches(params, ';');
                return hackyParamCount == paramCount;

            }
            return false;
        }).findFirst();
    }

    public boolean isOverrideConfigured() {
        return options.getReleaseTarget().isPresent();
    }

}
