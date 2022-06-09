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

    public static final String BASE_TEST_PACKAGE = "io.stubbs";

    public static GeneratedSubjectTypeStore newGeneratedSubjectTypeStore() {
        return new GeneratedSubjectTypeStore(Set.of(), new BuiltInSubjectTypeStore(TestClassFactories.newReflectionContext()));
    }

    public static ReflectionContext newReflectionContext() {
        // needs Google truth package too?
        return new ReflectionContext(BASE_TEST_PACKAGE);
    }

    public static SourceClassSets newSourceClassSets() {
        // needs Google truth package too?
        String targetPackage = TestClassFactories.class.getPackageName();
        return newSourceClassSets(targetPackage);
    }

    public static SourceClassSets newSourceClassSets(String targetPackage) {
        return new SourceClassSets(targetPackage, newReflectionContext());
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
