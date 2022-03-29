package io.stubbs.truth.generator.internal;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JDKOverrideAnalyser {

    // todo options
    private File targetJdkLibs = new File("C:\\Users\\anton\\.jdks\\adopt-openjdk-1.8.0_292\\jre\\lib\\rt.jar");

    @SneakyThrows
    public boolean doesOverrideClassContainMethod(Class<?> clazz, Method method) {
        Optional<InputStream> something = inputStreamForClass(clazz);
        return something
                .filter(inputStream ->
                        doesContainsMethod(inputStream, method))
                .isPresent();
    }

    @SneakyThrows
    private boolean doesContainsMethod(InputStream classInputStream, Method method) {
        ClassPool cp = ClassPool.getDefault();
        CtClass ctClass = cp.makeClass(classInputStream);
        String needle = method.getName();
        CtMethod[] methods = ctClass.getMethods();
        Optional<CtMethod> matchingMethod = StreamEx.of(methods)
                .filter(x ->
                        x.getName().equals(needle)
                )
                .findFirst();
        return matchingMethod.isPresent();
    }

    @SneakyThrows
    public Optional<InputStream> inputStreamForClass(Class<?> clazz) {
        String needle = StringUtils.replaceChars(clazz.getCanonicalName(), '.', '/') + ".class";
        // todo try with resource
        JarFile jarFile = new JarFile(targetJdkLibs);
        Optional<JarEntry> matchingJarEntry = StreamEx.of(jarFile.entries())
                .filter(entry ->
                        entry.getName().equals(needle)
                )
                .findFirst();
        if (matchingJarEntry.isPresent())
            return Optional.of(jarFile.getInputStream(matchingJarEntry.get()));
        else
            return Optional.empty();
    }

    @SneakyThrows
    public File dumpToTempFile(InputStream inputStream) {
        File tempFile = File.createTempFile(getClass().getName(), null);
        tempFile.deleteOnExit();
        OutputStream outStream = new FileOutputStream(tempFile);
        IOUtils.copy(inputStream, outStream);
        IOUtils.closeQuietly(outStream);
        return tempFile;
    }
}
