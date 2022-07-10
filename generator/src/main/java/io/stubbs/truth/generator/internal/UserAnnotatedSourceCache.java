package io.stubbs.truth.generator.internal;

import io.stubbs.truth.generator.FullContext;
import io.stubbs.truth.generator.SubjectFactoryMethod;
import io.stubbs.truth.generator.UserManagedSubject;
import io.stubbs.truth.generator.internal.model.UserSourceCodeManagedMiddleClass;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.JavaType;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.stubbs.truth.generator.internal.Utils.msg;

//todo collapse
@Slf4j
public class UserAnnotatedSourceCache {

    private final Map<String, UserSourceCodeManagedMiddleClass<?>> cache;

    private final FullContext reflectionContext;

    private final Set<SourceCodeScanner.CPPackage> sourcePackagesToScanForSubjects;

    public UserAnnotatedSourceCache(FullContext reflectionContext, Set<SourceCodeScanner.CPPackage> sourcePackagesToScanForSubjects) {
        this.reflectionContext = reflectionContext;
        this.sourcePackagesToScanForSubjects = sourcePackagesToScanForSubjects;

        this.cache = buildCache();
    }

    private Map<String, UserSourceCodeManagedMiddleClass<?>> buildCache() {
        Stream<UserSourceCodeManagedMiddleClass<?>> wrapStream = this.reflectionContext.getSourceRoots().stream()
                .flatMap(this::buildCacheForEachRoot);
        return StreamEx.of(wrapStream)
                .toMap(UserSourceCodeManagedMiddleClass::getClassUnderTestSimpleName,
                        wrap -> wrap);
    }

    public <T> Optional<UserSourceCodeManagedMiddleClass<T>> getUserSubjectWrapFor(Class<?> clazzUnderTest) {
        String key = clazzUnderTest.getCanonicalName();
        UserSourceCodeManagedMiddleClass<T> value = (UserSourceCodeManagedMiddleClass<T>) cache.get(key);
        return Optional.ofNullable(value);

//        Stream<Path> paths = classToPaths(clazzUnderTest);
//        Optional<Path> first = paths.findFirst();
//        final boolean contains = this.cache.contains(first.get());
//        final List<Path> paths1 = cache.stream().sorted().toList();
//        return first.flatMap(path
//                -> this.cache.contains(path)
//                ? Optional.of(path)
//                : Optional.empty());
    }

    private <T> Stream<Path> classToPaths(Class<T> clazzUnderTest) {
        Stream<Path> rootStream = reflectionContext.getSourceRoots().stream();
        return rootStream.flatMap(rootPath
                -> {
//            streamJavaFilesRecursive(rootPath, clazzUnderTest.getPackageName();
//            toJavaPath(rootPath, clazzUnderTest.getPackageName();
            Path path = getJavaPath(rootPath, clazzUnderTest.getCanonicalName());
            return Stream.of(path);
        });
    }

//    private Stream<? extends Path> toJavaPath(Path rootPath, String packageName) {
//        String packageToPath = relativePath.replace(".", File.separator);
//    }

    private <T> Stream<UserSourceCodeManagedMiddleClass<?>> buildCacheForEachRoot(Path sourceRoot) {
        return sourcePackagesToScanForSubjects.stream()
                .flatMap(packageInPath
                        -> buildCacheForEachPackage(sourceRoot, packageInPath));
    }

    @SneakyThrows
    private <T> Stream<UserSourceCodeManagedMiddleClass<?>> buildCacheForEachPackage(Path rootPath, SourceCodeScanner.CPPackage packageInPath) {
        String relativePath = packageInPath.getPackageName();
        return streamJavaFilesRecursive(rootPath, relativePath);
    }

    @SneakyThrows
    private <T> Stream<UserSourceCodeManagedMiddleClass<?>> streamJavaFilesRecursive(Path rootPath, String relativePath) {
        Path resolvedPath = getPath(rootPath, relativePath);
        return streamValidJavaFilesWithin(resolvedPath)
                .flatMap(path -> maybeParseFile(path));
    }

    private Path getJavaPath(Path rootPath, String relativePath) {
        String packageToPath = relativePath.replace(".", File.separator) + "Subject.java";
        Path resolvedPath = rootPath.resolve(packageToPath);
        return resolvedPath;
    }

    private Path getPath(Path rootPath, String relativePath) {
        String packageToPath = relativePath.replace(".", File.separator);
        Path resolvedPath = rootPath.resolve(packageToPath);
        return resolvedPath;
    }

    @SneakyThrows
    private Stream<Path> streamValidJavaFilesWithin(Path resolvedPath) {
        if (resolvedPath.toFile().exists()) {
            // name
            try (Stream<Path> walk = Files.walk(resolvedPath)) {
                List<Path> javaFiles = walk.filter(
                                this::isAJavaFile)
                        .collect(Collectors.toList());
                return StreamEx.of(javaFiles);
            }
        } else {
            return Stream.of();
        }
    }

    private boolean isAJavaFile(Path pathInQuestion) {
        boolean isAFile = pathInQuestion.toFile().isFile();
        boolean hasJavaExtension = pathInQuestion.toFile().getName().endsWith(".java");
        return isAFile && hasJavaExtension;
    }


    private <T> Stream<UserSourceCodeManagedMiddleClass<T>> maybeParseFile(Path resolve) {
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
            return createWrapIfValid(javaClassSource);
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
    private <T> Stream<UserSourceCodeManagedMiddleClass<T>> createWrapIfValid(JavaClassSource javaClassSource) {
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

                    if (findFactoryMethod.isPresent()) {
                        MethodSource<JavaClassSource> factory = findFactoryMethod.get();
                        UserSourceCodeManagedMiddleClass<T> wrappedSubject = new UserSourceCodeManagedMiddleClass<>(javaClassSource, factory);
                        return Stream.of(wrappedSubject);
                    } else {
                        throw new TruthGeneratorRuntimeException(msg("Subject class {} is missing factory marker {}",
                                javaClassSource.getCanonicalName(),
                                SubjectFactoryMethod.class.getSimpleName()));
                    }
                });
    }


}
