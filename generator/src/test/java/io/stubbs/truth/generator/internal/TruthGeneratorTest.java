package io.stubbs.truth.generator.internal;

import com.google.common.io.Resources;
import com.google.common.truth.Correspondence;
import com.google.common.truth.ObjectArraySubject;
import com.google.common.truth.Truth;
import io.stubbs.truth.generator.SourceClassSets;
import io.stubbs.truth.generator.TestModelUtils;
import io.stubbs.truth.generator.TruthGeneratorAPI;
import io.stubbs.truth.generator.internal.model.Result;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import io.stubbs.truth.generator.internal.modelSubjectChickens.ThreeSystemChildSubject;
import io.stubbs.truth.generator.shaded.org.jboss.forge.roaster.model.sourceChickens.JavaClassSourceSubject;
import io.stubbs.truth.generator.subjects.MyMapSubject;
import io.stubbs.truth.generator.subjects.MyStringSubject;
import io.stubbs.truth.generator.testModel.IdCard;
import io.stubbs.truth.generator.testModel.MyEmployee;
import io.stubbs.truth.generator.testModel.Project;
import io.stubbs.truth.generator.testing.legacy.NonBeanLegacy;
import lombok.SneakyThrows;
import org.jboss.forge.roaster.model.Method;
import org.jboss.forge.roaster.model.Type;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.jboss.forge.roaster.model.source.ParameterSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.threeten.extra.MutableClock;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.chrono.Chronology;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.truth.Correspondence.from;
import static com.google.common.truth.Correspondence.transforming;
import static com.google.common.truth.Truth.assertThat;
import static io.stubbs.truth.generator.internal.modelSubjectChickens.ThreeSystemChildSubject.assertThat;
import static java.util.Optional.of;

/**
 * @author Antony Stubbs
 */
// todo fix method naming
@RunWith(JUnit4.class)
public class TruthGeneratorTest {

    public static final Path testOutputDirectory = Paths.get("").resolve("target").toAbsolutePath();
    Correspondence<MethodSource<?>, String> methodHasName = transforming(MethodSource::getName, "has name of");

    static {
        GeneratedMarker.setClock(MutableClock.epochUTC());
    }

    /**
     * Base test that compares with expected generated code for test model. Brute force test.
     */
    @Test
    public void fullGeneratedCode() throws IOException {
        // todo need to be able to set base package for all generated classes, kind of like shade, so you cah generate test for classes in other restricted modules
        // todo replace with @TempDir
        TruthGenerator truthGenerator = TruthGeneratorAPI.create(testOutputDirectory, Options.builder().useHasInsteadOfGet(true).build());

        GeneratedMarker.setClock(MutableClock.epochUTC());

        //
        truthGenerator.registerStandardSubjectExtension(String.class, MyStringSubject.class);
        truthGenerator.registerStandardSubjectExtension(Map.class, MyMapSubject.class);

        //
        Set<Class<?>> classes = new HashSet<>();
        classes.add(MyEmployee.class);
        classes.add(IdCard.class);
        classes.add(Project.class);


        String packageForEntryPoint = getClass().getPackage().getName();
        SourceClassSets ss = new SourceClassSets(packageForEntryPoint);

        //
        SkeletonGenerator.forceMiddleGenerate = true;
        ss.generateAllFoundInPackagesOf(MyEmployee.class);

        // package exists in other module error - needs package target support
        ss.generateFrom(packageForEntryPoint, UUID.class);
        ss.generateFromShaded(ZoneId.class, ZonedDateTime.class, Chronology.class);


        Result generate = truthGenerator.generate(ss);
        Map<Class<?>, ThreeSystem<?>> generated = generate.getAll();

        assertThat(generated.size()).isAtLeast(classes.size());
        Set<? extends Class<?>> generatedSourceClasses = generated.values().stream().map(x -> x.classUnderTest).collect(Collectors.toSet());
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

    @SneakyThrows
    @Test
    public void generatedManagedEntryPoint() {
        TruthGenerator truthGenerator = TruthGeneratorAPI.create(testOutputDirectory, Options.builder().build());

        String packageForEntryPoint = getClass().getPackage().getName();
        SourceClassSets ss = new SourceClassSets(packageForEntryPoint);

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
        TruthGenerator truthGenerator = TruthGeneratorAPI.create(testOutputDirectory, Options.builder().build());

        String packageForEntryPoint = getClass().getPackage().getName();
        SourceClassSets ss = new SourceClassSets(packageForEntryPoint);

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

    private String loadFileToString(String expectedFileName) throws IOException {
        return Resources.toString(Resources.getResource(expectedFileName), Charset.defaultCharset());
    }

    /**
     * Chicken, or the egg? Create Subjects that we are currently saving and using in tests
     */
    @Test
    public void boostrapProjectSubjects() {
        TruthGenerator tg = TruthGeneratorAPI.createDefaultOptions(testOutputDirectory);
        SourceClassSets ss = new SourceClassSets(getClass().getPackage().getName());
        ss.generateFromShaded(JavaClassSource.class, Method.class);
        ss.generateAllFoundInPackagesOf(getClass());
        tg.generate(ss);
    }

    @Test
    public void package_java_mix() {
        TruthGeneratorAPI tg = TruthGeneratorAPI.createDefaultOptions(testOutputDirectory);

        String targetPackageName = this.getClass().getPackage().getName();
        SourceClassSets ss = new SourceClassSets(targetPackageName);

        ss.generateAllFoundInPackagesOf(IdCard.class);

        // generate java Subjects and put them in our package
        ss.generateFrom(targetPackageName, UUID.class);
        ss.generateFromShaded(ZoneId.class, ZonedDateTime.class, Chronology.class);

        var generated = tg.generate(ss).getAll();
        assertThat(generated.size()).isAtLeast(ss.getTargetPackageAndClasses().size());
    }

    @Test
    public void test_legacy_mode() {
        TruthGeneratorAPI tg = TruthGeneratorAPI.create(testOutputDirectory, Options.builder()
                .useHasInsteadOfGet(true)
                .useGetterForLegacyClasses(true)
                .build());
        SourceClassSets ss = new SourceClassSets(this.getClass().getPackage().getName() + ".legacy");
        ss.generateFromNonBean(NonBeanLegacy.class);
        var generated = tg.generate(ss).getAll();

        assertThat(generated).containsKey(NonBeanLegacy.class);
        ThreeSystem<?> actual = generated.get(NonBeanLegacy.class);
        assertThat(actual)
                .hasParent().hasGenerated().hasMethods()
                .comparingElementsUsing(methodHasName)
                .containsAtLeast("hasName", "hasAge");
    }

    /**
     * Given a single class or classes, generate subjects for all references classes in any nested return values
     */
    @Test
    public void recursive_generation() {
        TruthGenerator tg = TruthGeneratorAPI.createDefaultOptions(testOutputDirectory);
        var generate = tg.generate(MyEmployee.class).getAll();

        //
        assertThat(generate).containsKey(MyEmployee.class);
        assertThat(generate).containsKey(IdCard.class);
        assertThat(generate).containsKey(MyEmployee.State.class);

        // lost in the generics
        assertThat(generate).doesNotContainKey(Project.class);

        //
        assertThat(generate).containsKey(UUID.class);
        assertThat(generate).containsKey(ZonedDateTime.class);
        assertThat(generate).containsKey(DayOfWeek.class);

        // recursive subjects that shouldn't be included
        assertThat(generate).doesNotContainKey(Spliterator.class);
        assertThat(generate).doesNotContainKey(Stream.class);
    }

    /**
     * Automatically shade subjects that are in packages of other modules (that would cause ao compile error)
     * <p>
     * NB: if this test fails, the project probably won't compile - as an invalid source class will have been produced.
     */
    @Test
    public void auto_shade() {
        String basePackage = getClass().getPackage().getName();

        TruthGenerator tg = TruthGeneratorAPI.createDefaultOptions(testOutputDirectory);
        tg.setEntryPoint(of(basePackage));

        Class<UUID> clazz = UUID.class;
        var generate = tg.generate(clazz).getAll();

        //
        assertThat(generate).containsKey(clazz);

        //
        ThreeSystem threeSystem = generate.get(clazz);
        JavaClassSource parent = threeSystem.getParent().getGenerated();
        assertThat(parent.getPackage()).startsWith(basePackage);
    }

    /**
     * Simple check for generating chains for `toSomething` methods
     */
    @Test
    public void toers() {
        TruthGenerator tg = TruthGeneratorAPI.createDefaultOptions(testOutputDirectory);
        var generate = tg.generate(MyEmployee.class).getAll();
        ThreeSystem threeSystem = generate.get(MyEmployee.class);
        assertThat(threeSystem).hasParent().hasGenerated().hasMethods().comparingElementsUsing(methodHasName)
                .contains("hasToPlainPerson");
        assertThat(threeSystem.getParent().getGenerated().getMethod("hasToPlainPerson").getReturnType().getName())
                .isEqualTo("PersonSubject");
    }

    /**
     * Some source which return different types of arrays have been tricky, get some coverage here
     */
    @Test
    public void toArrays() {
        // would like to use generated truth subjects here, but don't want to have to copy in too many things, until the plugin is boot-strapable
        TruthGenerator tg = TruthGeneratorAPI.createDefaultOptions(testOutputDirectory);
        var generate = tg.generate(MyEmployee.class).getAll();
        ThreeSystem threeSystem = generate.get(MyEmployee.class);
        ThreeSystemChildSubject.assertThat(threeSystem).hasParent().hasGenerated().hasMethods().comparingElementsUsing(methodHasName)
                .containsAtLeast("hasToProjectObjectArray", "hasToStateArray");
        Type<JavaClassSource> hasToProjectArray = threeSystem.getParent().getGenerated()
                .getMethod("hasToStateArray").getReturnType();
        assertThat(hasToProjectArray.getSimpleName()).isEqualTo(ObjectArraySubject.class.getSimpleName());
    }

    /**
     * Test if we can get generics in some situations
     */
    @Test
    public void get_generics() {
        Options.OptionsBuilder recursive = Options.builder().recursive(false);// speed
        TruthGenerator tg = TruthGeneratorAPI.create(testOutputDirectory, recursive.build());

        var generate = tg.generate(MyEmployee.class).getAll();
        ThreeSystem threeSystem = generate.get(MyEmployee.class);
        JavaClassSource generated = threeSystem.getParent().getGenerated();


        Correspondence<ParameterSource<JavaClassSource>, Class<?>> classNames = from(
                (actual, expected) -> actual.getType().getName().equals(expected.getName()),
                "has class name matching class name of");

        // map contains strong key, value
        {
            String name = "hasProjectMapWithKey";
            MethodSource<JavaClassSource> method = TestModelUtils.findMethodWithNoParamsRoast(generated, name);
            List<ParameterSource<JavaClassSource>> parameters = method.getParameters();
            assertThat(parameters).comparingElementsUsing(classNames).containsExactly(String.class);
        }

        // list contains strong element
        {
            String name = "hasProjectListWithElement";
            MethodSource<JavaClassSource> method = TestModelUtils.findMethodWithNoParamsRoast(generated, name);
            List<ParameterSource<JavaClassSource>> parameters = method.getParameters();
            assertThat(parameters).comparingElementsUsing(classNames).containsExactly(Project.class);
        }
    }

    @Test
    public void standard_extensions() {
        Options recursive = Options.builder()
                .recursive(false)
                .useHasInsteadOfGet(true)
                .build(); // speed
        TruthGenerator tg = TruthGeneratorAPI.create(testOutputDirectory, recursive);
        TruthGeneratorAPI tgApi = tg;
        tg.setEntryPoint(of(this.getClass().getPackage().getName() + ".extensions"));

        // register handlers
        // todo do this with annotation scanning instead - this is done now?
        tgApi.registerStandardSubjectExtension(String.class, MyStringSubject.class);
        tgApi.registerStandardSubjectExtension(Map.class, MyMapSubject.class);

        //
        var generate = tg.generate(MyEmployee.class).getAll();

        //
        ThreeSystem<?> threeSystem = generate.get(MyEmployee.class);
        JavaClassSource generatedParent = threeSystem.getParent().getGenerated();

        // custom string
        {
            String name = "hasName";
            MethodSource<JavaClassSource> method = TestModelUtils.findMethodWithNoParamsRoast(generatedParent, name);
            Type<JavaClassSource> returnType = method.getReturnType();
            assertThat(returnType.getName()).isEqualTo(MyStringSubject.class.getSimpleName());
            assertThat(returnType.getName()).isEqualTo(MyStringSubject.class.getSimpleName());
        }

        // custom map
        {
            String name = "hasProjectMap";
            MethodSource<JavaClassSource> method = TestModelUtils.findMethodWithNoParamsRoast(generatedParent, name);
            Type<JavaClassSource> returnType = method.getReturnType();
            assertThat(returnType.getName()).isEqualTo(MyMapSubject.class.getSimpleName());
        }
    }

}
