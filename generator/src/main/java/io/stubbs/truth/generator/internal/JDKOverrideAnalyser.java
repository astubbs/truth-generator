package io.stubbs.truth.generator.internal;

import javassist.bytecode.AccessFlag;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.ClassFile;
import javassist.bytecode.MethodInfo;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jboss.forge.roaster._shade.org.eclipse.jdt.internal.compiler.util.CtSym;
import org.jboss.forge.roaster._shade.org.eclipse.jdt.internal.compiler.util.JRTUtil;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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
 * @see CtSym
 */
@Slf4j
@RequiredArgsConstructor
public class JDKOverrideAnalyser {

    private final Options options;

    private static final CtSym ctSym;

    private static final String CT_SYM_FILENAME = "ct.sym";

    static {
        String javaHomeEnv = System.getProperty("java.home");
        Validate.notNull(javaHomeEnv, "JAVA_HOME not found in environment, cannot load %s file.", CT_SYM_FILENAME);

        Path javaHome = Path.of(javaHomeEnv);

        if (!(javaHome.toFile().exists() && javaHome.toFile().isDirectory())) {
            throw new TruthGeneratorRuntimeException(msg("Cannot look up JAVA_HOME env variable. Found {}, but it either doesn't exist or isn't a directory", javaHome));
        }

        Path ctsymPath = javaHome.resolve("lib").resolve(CT_SYM_FILENAME);
        log.debug("Using {} as home for ct.sym located at {} which exists? {}", javaHome, ctsymPath, ctsymPath.toFile().exists());

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


    /**
     * JA - using JavaAssist
     *
     * @see ClassFile
     * @see MethodInfo
     */
    public static Optional<MethodInfo> findMethodJA(ClassFile classRepresentation, String methodName, int paramCount) {
        return classRepresentation.getMethods().stream()
                .filter(x -> {
                    if (x.getName().equals(methodName)) {

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

    /**
     * Uses reflection to get access to the full cached path list of {@code ct.sym} to debug reasons why a signature
     * can't be found
     *
     * @see CtSym#fileCache
     * @see CtSym#getCachedReleasePaths
     */
    private void debugCtSymEntries(Class<?> clazz, String releaseCode) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method getCachedReleasePaths = CtSym.class.getDeclaredMethod("getCachedReleasePaths", String.class);
        getCachedReleasePaths.setAccessible(true); //NOSONAR
        //noinspection unchecked
        Map<String, Path> invoke = (Map<String, Path>) getCachedReleasePaths.invoke(ctSym, releaseCode);
        Stream<String> stringStream = invoke.keySet().stream().filter(x -> x.contains(clazz.getSimpleName()));
        stringStream.forEach(x -> log.warn("Found: {}", x));
    }


    private boolean doesContainsMethod(ClassFile model, Method method) {
        Optional<MethodInfo> methodJA = findMethodJA(model, method.getName(), method.getParameterCount());
        return methodJA.isPresent() && AccessFlag.isPublic(methodJA.get().getAccessFlags());
    }

    @SneakyThrows
    @Nullable
    protected ClassFile getClassFileEclipse(int platformNumber, Class<?> clazz) {
        String platformName = Integer.toString(platformNumber);

        String qualifiedSigFilename = clazz.getTypeName().replace('.', '/') + ".sig";

        String releaseCode = CtSym.getReleaseCode(platformName);

        Module module = clazz.getModule();
        Optional<Path> fullPath = Optional.ofNullable(ctSym.getFullPath(releaseCode, qualifiedSigFilename, module.getName()));
        if (fullPath.isEmpty()) {
            // Zulu 11 VM on GitHub shows this issue, however zulu vm 11 on local machine works fine... so..?
            log.error("Lookup for {} {} with Module {} specified failed, will try without module", clazz, qualifiedSigFilename, module);
            fullPath = Optional.ofNullable(ctSym.getFullPath(releaseCode, qualifiedSigFilename, null));
        }

        if (fullPath.isEmpty()) {
            log.info("ct.sym look up failed for class {} in jdk version {}, name {}, module {}, with sig address {}", clazz, platformNumber, platformName, module, qualifiedSigFilename);
            debugCtSymEntries(clazz, releaseCode);
            return null;
        }

        // load bytes into ClassFile stub signature
        byte[] fileBytes = ctSym.getFileBytes(fullPath.get());
        return new ClassFile(new DataInputStream(new ByteArrayInputStream(fileBytes)));
    }

    public boolean isOverrideConfigured() {
        return options.getReleaseTarget().isPresent();
    }

}
