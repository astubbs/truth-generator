package io.stubbs.truth.generator.internal;

import com.google.common.truth.Correspondence;
import com.google.common.truth.ObjectArraySubject;
import com.google.common.truth.Truth;
import io.stubbs.truth.ManagedTruth;
import io.stubbs.truth.generator.SourceClassSets;
import io.stubbs.truth.generator.TestModelUtils;
import io.stubbs.truth.generator.TruthGeneratorAPI;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import io.stubbs.truth.generator.internal.modelSubjectChickens.ThreeSystemChildSubject;
import io.stubbs.truth.generator.subjects.MyMapSubject;
import io.stubbs.truth.generator.subjects.MyStringSubject;
import io.stubbs.truth.generator.testModel.IdCard;
import io.stubbs.truth.generator.testModel.MyEmployee;
import io.stubbs.truth.generator.testModel.Project;
import io.stubbs.truth.generator.testing.legacy.NonBeanLegacy;
import org.jboss.forge.roaster.model.Method;
import org.jboss.forge.roaster.model.Type;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.jboss.forge.roaster.model.source.ParameterSource;
import org.junit.Test;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.chrono.Chronology;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.UUID;
import java.util.stream.Stream;

import static com.google.common.truth.Correspondence.from;
import static com.google.common.truth.Correspondence.transforming;
import static com.google.common.truth.Truth.assertThat;
import static io.stubbs.truth.generator.internal.TruthGeneratorGeneratedSourceTest.TEST_OUTPUT_DIRECTORY;
import static java.util.Optional.of;

public class TruthGeneratorTest {

    /**
     * Used as a target to output all generated data during test suite. In order to not clash with bootstrapped
     * generated output from the plugin running as part of the BUILD.
     */
    public static final Path TEST_OUTPUT_DIRECTORY = Paths.get("").resolve("target").resolve("tmp-test-output").toAbsolutePath();

    Correspondence<MethodSource<?>, String> methodHasName = transforming(MethodSource::getName, "has name of");

    /**
     * Chicken, or the egg? Create Subjects that we are currently saving and using in tests
     */
    @Test
    public void boostrapProjectSubjects() {
        TruthGenerator tg = TruthGeneratorAPI.createDefaultOptions(TEST_OUTPUT_DIRECTORY);
        SourceClassSets ss = new SourceClassSets(getClass().getPackage().getName());
        ss.generateFromShaded(JavaClassSource.class, Method.class);
        ss.generateAllFoundInPackagesOf(getClass());
        tg.generate(ss);
    }

    @Test
    public void packageJavaMix() {
        TruthGeneratorAPI tg = TruthGeneratorAPI.createDefaultOptions(TEST_OUTPUT_DIRECTORY);

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
    public void testLegacyMode() {
        TruthGeneratorAPI tg = TruthGeneratorAPI.create(TEST_OUTPUT_DIRECTORY, Options.builder()
                .useHasInsteadOfGet(true)
                .useGetterForLegacyClasses(true)
                .build());
        SourceClassSets ss = new SourceClassSets(this.getClass().getPackage().getName() + ".legacy");
        ss.generateFromNonBean(NonBeanLegacy.class);
        var generated = tg.generate(ss).getAll();

        assertThat(generated).containsKey(NonBeanLegacy.class);
        ThreeSystem<?> actual = generated.get(NonBeanLegacy.class);
        ThreeSystemChildSubject.assertThat(actual)
                .hasParent().hasGenerated().hasMethods()
                .comparingElementsUsing(methodHasName)
                .containsAtLeast("hasName", "hasAge");
    }

    /**
     * Given a single class or classes, generate subjects for all references classes in any nested return values
     */
    @Test
    public void recursiveGeneration() {
        TruthGenerator tg = TruthGeneratorAPI.createDefaultOptions(TEST_OUTPUT_DIRECTORY);
        var allGeneratedSystems = tg.generate(MyEmployee.class);

        //
        var assertGeneratedSet = ManagedTruth.assertThat(allGeneratedSystems).getAll();
        assertGeneratedSet.containsKey(MyEmployee.class);
        assertGeneratedSet.containsKey(IdCard.class);
        assertGeneratedSet.containsKey(MyEmployee.State.class);

        // lost in the generics
        assertGeneratedSet.doesNotContainKey(Project.class);

        //
        assertGeneratedSet.containsKey(UUID.class);
        assertGeneratedSet.containsKey(ZonedDateTime.class);
        assertGeneratedSet.containsKey(DayOfWeek.class);

        // recursive subjects that shouldn't be included
        assertGeneratedSet.doesNotContainKey(Spliterator.class);
        assertGeneratedSet.doesNotContainKey(Stream.class);
    }

    /**
     * Automatically shade subjects that are in packages of other modules (that would cause ao compile error)
     * <p>
     * NB: if this test fails, the project probably won't compile - as an invalid source class will have been produced.
     */
    @Test
    public void autoShade() {
        String basePackage = getClass().getPackage().getName();

        TruthGenerator tg = TruthGeneratorAPI.createDefaultOptions(TEST_OUTPUT_DIRECTORY);
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
        TruthGenerator tg = TruthGeneratorAPI.createDefaultOptions(TEST_OUTPUT_DIRECTORY);
        var generate = tg.generate(MyEmployee.class).getAll();
        ThreeSystem threeSystem = generate.get(MyEmployee.class);
        ThreeSystemChildSubject.assertThat(threeSystem).hasParent().hasGenerated().hasMethods().comparingElementsUsing(methodHasName)
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
        TruthGenerator tg = TruthGeneratorAPI.createDefaultOptions(TEST_OUTPUT_DIRECTORY);
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
    public void getGenerics() {
        Options.OptionsBuilder recursive = Options.builder().recursive(false);// speed
        TruthGenerator tg = TruthGeneratorAPI.create(TEST_OUTPUT_DIRECTORY, recursive.build());

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
    public void standardExtensions() {
        Options recursive = Options.builder()
                .recursive(false)
                .useHasInsteadOfGet(true)
                .build(); // speed
        TruthGenerator tg = TruthGeneratorAPI.create(TEST_OUTPUT_DIRECTORY, recursive);
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
