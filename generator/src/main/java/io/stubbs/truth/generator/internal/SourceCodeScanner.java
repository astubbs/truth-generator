package io.stubbs.truth.generator.internal;

import com.google.common.truth.Subject;
import io.stubbs.truth.generator.FullContext;
import io.stubbs.truth.generator.SubjectFactoryMethod;
import io.stubbs.truth.generator.UserManagedSubject;
import io.stubbs.truth.generator.internal.model.UserSourceCodeManagedMiddleClass;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.JavaType;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static io.stubbs.truth.generator.internal.Utils.msg;

/**
 * Looks for {@link io.stubbs.truth.generator.UserManagedMiddleSubject} classes which aren't compiled, inside source
 * directories - typically just the test source directory of the module running in.
 *
 * @author Antony Stubbs
 */
@Slf4j
@Value
public class SourceCodeScanner {

    FullContext reflectionContext;

    /**
     * Packages to look for existing {@link Subject}s in.
     */
    Set<CPPackage> sourcePackagesToScanForSubjects;

    public <T> Optional<UserSourceCodeManagedMiddleClass<T>> tryGetUserManagedMiddle(Class<T> clazzUnderTest) {
        // for each source path
        var stream = reflectionContext.getSourceRoots().stream()
                .flatMap(path -> process(clazzUnderTest, path));
        // todo if result > 1, warn
        Optional<UserSourceCodeManagedMiddleClass<T>> first = stream.findFirst();
        return first;
    }

    private <T> Stream<UserSourceCodeManagedMiddleClass<T>> process(Class<T> clazzUnderTest, Path path) {
        return sourcePackagesToScanForSubjects.stream().flatMap(cpPackage -> resolve(clazzUnderTest, path, cpPackage));
    }

    @SneakyThrows // todo remove
    private <T> Stream<UserSourceCodeManagedMiddleClass<T>> resolve(Class<T> clazzUnderTest, Path rootPath, CPPackage cpPackage) {
        String packageToPath = cpPackage.getPackageName().replace(".", File.separator);
        Path resolvedPath = rootPath.resolve(packageToPath);
        if (resolvedPath.toFile().exists()) {
            // name
            try (Stream<Path> walk = Files.walk(resolvedPath)) {
                Stream<Path> javaFiles = walk.filter(
                        pathInQuestion
                                -> pathInQuestion.toFile().isFile()
                                && pathInQuestion.toFile().getName().endsWith(".java"));
                return javaFiles.flatMap(pathInQuestion -> maybeParseFile(clazzUnderTest, pathInQuestion));
            }
        } else {
            return Stream.of();
        }
    }

    private <T> Stream<UserSourceCodeManagedMiddleClass<T>> maybeParseFile(Class<T> clazzUnderTest, Path resolve) {
        JavaType<?> rawParse = null;
        File file = resolve.toFile();
        try {
            rawParse = Roaster.parse(file);
        } catch (IOException e) {
            log.debug("Error parsing file {}", file, e);
        }

        if (rawParse == null || !rawParse.isClass()) {
            return Stream.empty();
        } else if (rawParse instanceof JavaClassSource javaClassSource) {
            return createWrapIfValid(clazzUnderTest, javaClassSource);
        } else {
            return Stream.empty();
        }
    }

    /**
     * Look inside the source code, find the right annotations, and if valid, return the wrapped version
     *
     * @return a maybe empty stream of wrapped versions of the discovered {@link UserSourceCodeManagedMiddleClass}
     * @see UserSourceCodeManagedMiddleClass#getFactoryMethodName()
     */
    private <T> Stream<UserSourceCodeManagedMiddleClass<T>> createWrapIfValid(Class<T> clazzUnderTest, JavaClassSource javaClassSource) {
        Optional<MethodSource<JavaClassSource>> findFactoryMethod = javaClassSource.getMethods().stream()
                .filter(method -> method.hasAnnotation(SubjectFactoryMethod.class))
                .findFirst();

        String targetAnnotation = UserManagedSubject.class.getSimpleName();

        return javaClassSource.getAnnotations().stream()
                .filter(annotation
                        -> annotation.getName().equals(targetAnnotation))
                .flatMap(annotation -> {

                    //
                    String annotationValue = annotation.getStringValue("value");
                    boolean isSubjectForTargetClazz = annotationValue.equals(clazzUnderTest.getName());

                    //
                    if (isSubjectForTargetClazz) {
                        if (findFactoryMethod.isPresent()) {
                            MethodSource<JavaClassSource> factory = findFactoryMethod.get();
                            UserSourceCodeManagedMiddleClass<T> wrappedSubject = new UserSourceCodeManagedMiddleClass<>(javaClassSource, factory);
                            return Stream.of(wrappedSubject);
                        } else {
                            throw new TruthGeneratorRuntimeException(msg("Subject class {} is missing factory marker {}",
                                    javaClassSource.getCanonicalName(),
                                    SubjectFactoryMethod.class.getSimpleName()));
                        }
                    } else {
                        return Stream.empty();
                    }
                });
    }

    @Value
    public static
    class CPPackage {
        String packageName;
    }
}
