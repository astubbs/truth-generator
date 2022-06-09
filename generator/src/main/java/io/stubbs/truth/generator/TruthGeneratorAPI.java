package io.stubbs.truth.generator;

import com.google.common.truth.Subject;
import io.stubbs.truth.generator.internal.Options;
import io.stubbs.truth.generator.internal.TruthGenerator;
import io.stubbs.truth.generator.internal.model.Result;

import java.nio.file.Path;
import java.util.Set;

/**
 * Entry point API for Truth Generator
 *
 * @author Antony Stubbs
 * @see TruthGenerator
 */
// TODO clean up
public interface TruthGeneratorAPI {

    static TruthGenerator create(Path testOutputDirectory, SourceClassSets ss) {
        return createDefaultOptions(testOutputDirectory, ss);
    }

    static TruthGenerator createDefaultOptions(Path testOutputDirectory, SourceClassSets ss) {
        return new TruthGenerator(testOutputDirectory, Options.builder().build(), ss);
    }

    static TruthGenerator create(Path testOutputDirectory, Options options, SourceClassSets ss) {
        return new TruthGenerator(testOutputDirectory, options, ss);
    }

    /**
     * Takes a user maintained source file, and adds boiler plate and Subject methods that are missing. If aggressively
     * skips parts if it thinks the user has overridden something.
     * <p>
     * Not implemented yet.
     */
    String maintain(Class source, Class userAndGeneratedMix);

    /**
     * todo
     */
    <T> String combinedSystem(Class<T> source);

    /**
     * todo
     */
    void combinedSystem(String... modelPackages);

    /**
     * @param modelPackages
     */
    void generate(String... modelPackages);

    /**
     * @param classes
     */
    void generateFromPackagesOf(Class<?>... classes);

    /**
     * @param ss
     */
    void combinedSystem(SourceClassSets ss);

    /**
     * Use this entry point to generate for a large and differing set of source classes - which will also generate a
     * single point of entry for all of them.
     *
     * <p>
     * There are many different ways to add, check out the different methods in {@link SourceClassSets}.
     *
     * @return
     * @see SourceClassSets
     */
    Result generate(SourceClassSets ss);

    /**
     * @param classes
     * @return
     */
    Result generate(Set<Class<?>> classes);

    Result generate(Class<?>... classes);

    /**
     * Manually register extensions to base Subject types - i.e. extend StringSubject with your own features. These will
     * get dynamically inserted into the generated Subject tree when used.
     *
     * @param targetType        the class under test - e.g. String
     * @param myMapSubjectClass the Subject class which extends the base Subject
     */
    void registerStandardSubjectExtension(Class<?> targetType, Class<? extends Subject> myMapSubjectClass);

}
