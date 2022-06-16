package io.stubbs.truth.generator.internal;

import io.stubbs.truth.generator.FullContext;
import io.stubbs.truth.generator.SubjectFactoryMethod;
import io.stubbs.truth.generator.UserManagedTruth;
import io.stubbs.truth.generator.internal.model.UserSourceCodeManagedMiddleClass;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.Annotation;
import org.jboss.forge.roaster.model.JavaType;
import org.jboss.forge.roaster.model.impl.JavaClassImpl;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

// for each source path
// for each node in tree under translated package
// parse file
// look for annotation or interface
// look for annotated factory method
@Slf4j
@Value
public class SourceCodeScanner {

    FullContext reflectionContext;

    Set<CPPackage> sourcePackagesToScan;

//    Set<Path> sourceFilePathRoutes;

    public <T> Optional<UserSourceCodeManagedMiddleClass<T>> tryGetUserManagedMiddle(Class<T> clazzUnderTest) {
        // for each source path
        var stream = reflectionContext.getSourceRoots().stream()
                .flatMap(path -> process(clazzUnderTest, path));
        // todo if result > 1, warn
        return stream.findFirst();
    }

    private <T> Stream<UserSourceCodeManagedMiddleClass<T>> process(Class<T> clazzUnderTest, Path path) {
        return sourcePackagesToScan.stream().flatMap(cpPackage -> resolve(clazzUnderTest, path, cpPackage));
    }

    @SneakyThrows // todo remove
    private <T> Stream<UserSourceCodeManagedMiddleClass<T>> resolve(Class<T> clazzUnderTest, Path path, CPPackage cpPackage) {
        final String[] packageComponents = StringUtils.split(cpPackage.getPackageName(), '.');
        var streamResolve = Arrays.stream(packageComponents).map(path::resolve).findFirst();
        String replace = cpPackage.getPackageName().replace(".", File.separator);
        Path resolve = path.resolve(replace);
        if (resolve.toFile().exists()) {
            var paths = Files.newDirectoryStream(resolve);
            var sss = StreamSupport.stream(paths.spliterator(), false).collect(Collectors.toList());
            Stream<Path> walk = Files.walk(resolve);

            Stream<Path> pathStream = walk.filter(path1 -> path1.toFile().isFile() && path1.toFile().getName().endsWith(".java"));
            Stream<UserSourceCodeManagedMiddleClass<T>> rStream = pathStream.flatMap(path1 -> {
                Stream<UserSourceCodeManagedMiddleClass<T>> parse = parseFile(clazzUnderTest, path1);
                return parse;
            });
            return rStream;
        } else return Stream.of();
    }

    private <T> Stream<UserSourceCodeManagedMiddleClass<T>> parseFile(Class<T> clazzUnderTest, Path resolve) {
        JavaType<?> raw = null;
        File file = resolve.toFile();
        try {
            raw = Roaster.parse(file);
        } catch (IOException e) {
            log.debug("Error parsing file {}", file, e);
        }
        boolean aClass = raw.isClass();

        if (!aClass) {
            return Stream.empty();
        }
        JavaClassSource parse = (JavaClassImpl) raw;

        // todo can with delombok find lombok methods?
//        lombok.Lombok.
//        lombok.delombok.Delombok.delombok

        JavaType<?> enclosingType = parse.getEnclosingType();
//        JavaClassImpl impl = (JavaClassImpl) parse;
//        List<MethodSource<JavaClassSource>> methods = impl.getMethods();

        String canonicalName = parse.getCanonicalName();
        List<? extends Annotation<?>> annotations = parse.getAnnotations();

        Optional<MethodSource<JavaClassSource>> first = parse.getMethods().stream().filter(method -> method.hasAnnotation(SubjectFactoryMethod.class)).findFirst();

        JavaType<?> origin = parse.getOrigin();

        org.jboss.forge.roaster._shade.org.eclipse.jdt.core.dom.CompilationUnit internal = (org.jboss.forge.roaster._shade.org.eclipse.jdt.core.dom.CompilationUnit) parse.getInternal();

        boolean threeSystemSubject = resolve.toFile().getName().contains("ThreeSystemSubject");


        JavaClassSource finalParse = parse;
        /**
         * @see UserSourceCodeManagedMiddleClass#getFactoryMethodName()
         */
        Stream<UserSourceCodeManagedMiddleClass<T>> neww = annotations.stream().filter(annotation -> {

            // make DRY

            String name = annotation.getName();
//            Class<?> classValue = annotation.getClassValue();
            String target = UserManagedTruth.class.getSimpleName();
            boolean match = annotation.getName().equals(target);

            return match;
        }).flatMap(annotation -> {
            String value = annotation.getStringValue("value");
//            Class<T> aClass1 = null;

//            Optional<? extends Class<?>> first1 = reflectionContext.getLoaders().stream().flatMap(classLoader -> {
//                try {
//                    return Stream.of(classLoader.loadClass(value));
//                } catch (ClassNotFoundException e) {
////                    e.printStackTrace();
////                    throw new TruthGeneratorRuntimeException("", e);
//                    return Stream.empty();
//                }
//            }).findFirst();
//            if (first1.isPresent()) {
////                aClass1 = (Class<T>) first1.get();
//            } else {
//
//                try {
//                    aClass1 = (Class<T>) Class.forName(value);
//                } catch (ClassNotFoundException e) {
//                    e.printStackTrace();
//                    throw new TruthGeneratorRuntimeException("", e);
//                }
//            }
            if (value.equals(clazzUnderTest.getName())) {
                if (first.isPresent()) {
                    MethodSource<JavaClassSource> factory = first.get();
                    var one = new UserSourceCodeManagedMiddleClass<T>(finalParse, factory);
                    return Stream.of(one);
                } else {
                    throw new TruthGeneratorRuntimeException("Missing factory");
                }

            } else {
                return Stream.empty();
            }
//                    return new UserSuppliedMiddleClass(null, aClass1);
        });


        return neww;
//        return b.map(annotation -> {
//
//            return null;
//        });
    }

    @Value
    public static
    class CPPackage {
        String packageName;
    }
}
