package io.stubbs.truth.generator;

import io.stubbs.truth.generator.internal.*;
import lombok.experimental.UtilityClass;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * Non-trivial object creation for reuse
 */
@UtilityClass
public class TestClassFactories {

    //todo needs to be pulled from maven - multiple
    public static final Set<Path> TEST_SRC_ROOT = Set.of(Paths.get("").resolve("src").resolve("test").resolve("java").toAbsolutePath());

    public static final Path TEST_OUTPUT_DIRECTORY = Paths.get("").resolve("target").resolve("generated-test-sources").toAbsolutePath();

    // todo move?
    public static final String BASE_TEST_PACKAGE = "io.stubbs.truth.generator";

    public static GeneratedSubjectTypeStore newGeneratedSubjectTypeStore() {
        return new GeneratedSubjectTypeStore(Set.of(), newBuiltInSubjectTypeStore());
    }

    public static BuiltInSubjectTypeStore newBuiltInSubjectTypeStore() {
        return new BuiltInSubjectTypeStore(TestClassFactories.newReflectionContext());
    }

    public static SourceClassSets newSourceClassSets() {
        // needs Google truth package too?
        String packageForEntryPoint = TestClassFactories.class.getPackageName();
        return newSourceClassSets(packageForEntryPoint);
    }

    public static SourceClassSets newSourceClassSets(Object packageFromClass) {
        return new SourceClassSets(packageFromClass.getClass().getPackageName(), newReflectionContext());
    }

    public static SourceClassSets newSourceClassSets(String packageForEntryPoint) {
        return new SourceClassSets(packageForEntryPoint, newReflectionContext());
    }

    public static ReflectionContext newReflectionContext() {
        // needs Google truth package too?
        return new ReflectionContext(Set.of(BASE_TEST_PACKAGE, "com.google.truth"));
    }

    public static TruthGenerator newTruthGenerator() {
        return TruthGeneratorAPI.create(Options.builder().build(), newFullContext());
    }

    public static FullContext newFullContext() {
        // needs Google truth package too?
        return new FullContext(TEST_OUTPUT_DIRECTORY, newReflectionContext());
    }

    public static TruthGenerator newTruthGenerator(Options options) {
        return TruthGeneratorAPI.create(options, newFullContext());
    }

    public static SourceCodeScanner newScanner() {
        var packages = Set.of(new SourceCodeScanner.CPPackage("io.stubbs"));
        return new SourceCodeScanner(TestClassFactories.newReflectionContext(), packages, TEST_SRC_ROOT);
    }
}
