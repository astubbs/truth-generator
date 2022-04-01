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
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    private final Map<Class<?>, ClassFile> cache = new HashMap<>();

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
        Path java_home = Path.of(System.getenv("JAVA_HOME"));
        CtSym ctSym = JRTUtil.getCtSym(java_home);

        Optional<Path> fullPath = Optional.ofNullable(ctSym.getFullPath(releaseCode, qualifiedSigFilename, clazz.getModule().getName()));

        if (fullPath.isEmpty())
            return null;

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
