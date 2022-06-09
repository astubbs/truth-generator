package io.stubbs.truth.generator;

import io.stubbs.truth.generator.internal.BuiltInSubjectTypeStore;
import io.stubbs.truth.generator.internal.GeneratedSubjectTypeStore;
import io.stubbs.truth.generator.internal.Options;
import io.stubbs.truth.generator.internal.TruthGenerator;
import lombok.experimental.UtilityClass;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * Non-trivial object creation for reuse
 */
@UtilityClass
public class TestClassFactories {

    public static final Path TEST_OUTPUT_DIRECTORY = Paths.get("").resolve("target").toAbsolutePath();

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

}
