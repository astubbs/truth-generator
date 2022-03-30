package io.stubbs.truth.generator.internal;

import javassist.bytecode.AccessFlag;
import javassist.bytecode.ClassFile;
import javassist.bytecode.MethodInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.dynamic.ClassFileLocator;
import one.util.streamex.StreamEx;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.*;

/**
 * Used to test if a provided class contains a given method, when the class byte data is loaded from a configured jar,
 * instead of the runtime JDK.
 *
 * @author Antony Stubbs
 * @see Options#getRuntimeJavaClassSourceOverride()
 */
@Slf4j
@RequiredArgsConstructor
public class JDKOverrideAnalyser {

    private final Options options;

    private final Map<Class<?>, ClassFile> cache = new HashMap<>();

    public boolean doesOverrideClassContainMethod(Class<?> clazz, Method method) {
        Optional<ClassFile> classModel = getCachedClass(clazz);

        return classModel
                .filter(model ->
                        doesContainsMethod(model, method))
                .isPresent();
    }

    private Optional<ClassFile> getCachedClass(Class<?> clazz) {
        var cachedModel = ofNullable(cache.get(clazz));
        if (cachedModel.isPresent()) {
            return cachedModel;
        } else {
            Optional<ClassFile> aClass = getClass(clazz);
            aClass.ifPresent(classFile -> cache.put(clazz, classFile));
            return aClass;
        }
    }

    private boolean doesContainsMethod(ClassFile model, Method method) {
        String needle = method.getName();
        List<MethodInfo> methods = model.getMethods();
        Optional<MethodInfo> matchingMethod = StreamEx.of(methods)
                .filter(x ->
                        x.getName().equals(needle)
                )
                .findFirst();
        return matchingMethod.isPresent() && AccessFlag.isPublic(matchingMethod.get().getAccessFlags());
    }

    private Optional<ClassFile> getClass(Class<?> clazz) {
        if (!isOverrideConfigured()) {
            throw new IllegalStateException("No class source override configured");
        }

        try {
            String name = clazz.getCanonicalName();
            //noinspection OptionalGetWithoutIsPresent - checked in isOverrideConfigured
            var locate = ClassFileLocator.ForJarFile
                    .of(options.getRuntimeJavaClassSourceOverride().get()) // NOSONAR
                    .locate(name);
            byte[] resolve = locate.resolve();
            ClassFile classRepresentation = new ClassFile(new DataInputStream(new ByteArrayInputStream(resolve)));
            return of(classRepresentation);
        } catch (IOException e) {
            log.error("Can't load class override for {} from {}", clazz, options.getRuntimeJavaClassSourceOverride().get()); // NOSONAR
            return empty();
        }
    }

    public boolean isOverrideConfigured() {
        return options.getRuntimeJavaClassSourceOverride().isPresent();
    }

}
