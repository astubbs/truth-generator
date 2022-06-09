package io.stubbs.truth.generator.internal;

import com.google.common.io.Resources;
import com.google.common.truth.Truth;
import io.stubbs.truth.generator.FullContext;
import io.stubbs.truth.generator.SourceClassSets;
import io.stubbs.truth.generator.TestClassFactories;
import io.stubbs.truth.generator.TruthGeneratorAPI;
import io.stubbs.truth.generator.internal.model.Result;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import io.stubbs.truth.generator.shaded.org.jboss.forge.roaster.model.sourceChickens.JavaClassSourceSubject;
import io.stubbs.truth.generator.subjects.MyMapSubject;
import io.stubbs.truth.generator.subjects.MyStringSubject;
import io.stubbs.truth.generator.testModel.IdCard;
import io.stubbs.truth.generator.testModel.MyEmployee;
import io.stubbs.truth.generator.testModel.Project;
import io.stubbs.truth.generator.testing.legacy.NonBeanLegacy;
import lombok.SneakyThrows;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.threeten.extra.MutableClock;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.chrono.Chronology;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.google.common.truth.Truth.assertThat;
import static io.stubbs.truth.generator.TestClassFactories.TEST_OUTPUT_DIRECTORY;
import static io.stubbs.truth.generator.internal.modelSubjectChickens.ThreeSystemChildSubject.assertThat;

/**
 * @author Antony Stubbs
 */
// todo fix method naming
@RunWith(JUnit4.class)
public class TruthGeneratorGeneratedSourceTest {

    static {
        GeneratedMarker.setClock(MutableClock.epochUTC());
    }

    /**
     * Base test that compares with expected generated code for test model. Brute force test.
     */
    @Test
    public void fullGeneratedCode() throws IOException {
        GeneratedMarker.setClock(MutableClock.epochUTC());

        //
        Set<Class<?>> classes = new HashSet<>();
        classes.add(MyEmployee.class);
        classes.add(IdCard.class);
        classes.add(Project.class);


        SourceClassSets ss = TestClassFactories.newSourceClassSets();

        //
        SkeletonGenerator.forceMiddleGenerate = true;
        ss.generateAllFoundInPackagesOf(MyEmployee.class);

        // package exists in other module error - needs package target support
        String packageForEntryPoint = getClass().getPackage().getName();
        ss.generateFrom(packageForEntryPoint, UUID.class);
        ss.generateFromShaded(ZoneId.class, ZonedDateTime.class, Chronology.class);

        // todo need to be able to set base package for all generated classes, kind of like shade, so you cah generate test for classes in other restricted modules
        // todo replace with @TempDir
        FullContext fullContext = new FullContext(TEST_OUTPUT_DIRECTORY, ss.getPackageForEntryPoint());
        TruthGenerator truthGenerator = TruthGeneratorAPI.create(
                Options.builder().useHasInsteadOfGet(true).build(),
                fullContext);

        //
        truthGenerator.registerStandardSubjectExtension(String.class, MyStringSubject.class);
        truthGenerator.registerStandardSubjectExtension(Map.class, MyMapSubject.class);

        Result generate = truthGenerator.generate(ss);
        Map<Class<?>, ThreeSystem<?>> generated = generate.getAll();

        assertThat(generated.size()).isAtLeast(classes.size());
        Set<? extends Class<?>> generatedSourceClasses = generated.values().stream().map(x -> x.getClassUnderTest()).collect(Collectors.toSet());
        assertThat(generatedSourceClasses).containsAtLeast(UUID.class, ZonedDateTime.class, MyEmployee.class, MyEmployee.State.class);

        //
        ThreeSystem<?> threeSystemGenerated = generated.get(MyEmployee.class);
        assertThat(threeSystemGenerated).isNotNull();

        // check package of generated target is correct
        assertThat(threeSystemGenerated).hasParent().withSamePackageAs(MyEmployee.class);
        assertThat(threeSystemGenerated).hasMiddle().withSamePackageAs(MyEmployee.class);
        assertThat(threeSystemGenerated).hasChild().withSamePackageAs(MyEmployee.class);

        String expected = loadFileToString("expected/MyEmployeeParentSubject.java.txt");
        assertThat(threeSystemGenerated)
                .hasParent()
                .hasGenerated()
                .hasSourceText()
                .ignoringTrailingWhiteSpace()
                .equalTo(expected); // sanity full chain

        assertThat(threeSystemGenerated).hasParentSource(expected);

        String expected1 = loadFileToString("expected/MyEmployeeSubject.java.txt");
        assertThat(threeSystemGenerated).hasMiddleSource(expected1);

        String expected2 = loadFileToString("expected/MyEmployeeChildSubject.java.txt");
        assertThat(threeSystemGenerated).hasChildSource(expected2);
    }

    private String loadFileToString(String expectedFileName) throws IOException {
        return Resources.toString(Resources.getResource(expectedFileName), Charset.defaultCharset());
    }

    @SneakyThrows
    @Test
    public void generatedManagedEntryPoint() {
        TruthGenerator truthGenerator = TestClassFactories.newTruthGenerator();
        SourceClassSets ss = TestClassFactories.newSourceClassSets(this);

        ss.generateFrom(MyEmployee.class);

        Result generate = truthGenerator.generate(ss);

        OverallEntryPoint overallEntryPoint = generate.getOverallEntryPoint();

        JavaClassSource generated = overallEntryPoint.getOverallEntryPointGenerated();

        String actual = generated.toString();
        assertThat(actual).contains("collections()).that");
        assertThat(actual).contains("maps()).that");
        assertThat(actual).contains("strings()).that");

        String expected = loadFileToString("expected/ManagedTruth.java.txt");
        Truth.assertAbout(JavaClassSourceSubject.javaClassSources())
                .that(generated)
                .hasSourceText()
                .ignoringTrailingWhiteSpace()
                .equalTo(expected);
    }

    @SneakyThrows
    @Test
    public void generatedManagedSubjectBuilder() {
        TruthGenerator truthGenerator = TestClassFactories.newTruthGenerator();
        SourceClassSets ss = TestClassFactories.newSourceClassSets(this);

        ss.generateFrom(MyEmployee.class);

        Result generate = truthGenerator.generate(ss);

        OverallEntryPoint overallEntryPoint = generate.getOverallEntryPoint();

        JavaClassSource generated = overallEntryPoint.getManagedSubjectBuilderGenerated();

        String expected = loadFileToString("expected/ManagedSubjectBuilder.java.txt");
        Truth.assertAbout(JavaClassSourceSubject.javaClassSources())
                .that(generated)
                .hasSourceText()
                .ignoringTrailingWhiteSpace()
                .equalTo(expected);
    }

    @SneakyThrows
    @Test
    public void legacySourceTest() {
        // todo need to be able to set base package for all generated classes, kind of like shade, so you can generate
        //  tests for classes in other restricted modules
        // todo replace with @TempDir
        Options options = Options.builder()
                .useHasInsteadOfGet(true)
                .useGetterForLegacyClasses(true)
                .build();
        TruthGenerator truthGenerator = TestClassFactories.newTruthGenerator(options);

        GeneratedMarker.setClock(MutableClock.epochUTC());

        //
        SourceClassSets ss = TestClassFactories.newSourceClassSets();
        ss.generateFromNonBean(NonBeanLegacy.class);

        //
        SkeletonGenerator.forceMiddleGenerate = true;

        Result generate = truthGenerator.generate(ss);
        Map<Class<?>, ThreeSystem<?>> generated = generate.getAll();

        //
        ThreeSystem<?> threeSystemGenerated = generated.get(NonBeanLegacy.class);

        //
        String expected = loadFileToString("expected/NonBeanLegacyParentSubject.java.txt");
        assertThat(threeSystemGenerated).hasParentSource(expected);
    }

}
