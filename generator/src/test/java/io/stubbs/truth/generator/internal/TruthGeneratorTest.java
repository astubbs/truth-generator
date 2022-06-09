package io.stubbs.truth.generator.internal;

import com.google.common.truth.Correspondence;
import com.google.common.truth.ObjectArraySubject;
import io.stubbs.truth.generator.SourceClassSets;
import io.stubbs.truth.generator.TestClassFactories;
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
import static java.util.Optional.of;

public class TruthGeneratorTest {

    Correspondence<MethodSource<?>, String> methodHasName = transforming(MethodSource::getName, "has name of");

    /**
     * Chicken, or the egg? Create Subjects that we are currently saving and using in tests
     */
    @Test
    public void boostrapProjectSubjects() {
        TruthGenerator tg = TestClassFactories.newTruthGenerator();
        SourceClassSets ss = TestClassFactories.newSourceClassSets();
        ss.generateFromShaded(JavaClassSource.class, Method.class);
        ss.generateAllFoundInPackagesOf(getClass());
        tg.generate(ss);
    }

    @Test
    public void packageJavaMix() {
        TruthGenerator tg = TestClassFactories.newTruthGenerator();
        SourceClassSets ss = TestClassFactories.newSourceClassSets();

        ss.generateAllFoundInPackagesOf(IdCard.class);

        // generate java Subjects and put them in our package
        ss.generateFrom(ss.getPackageForEntryPoint(), UUID.class);
        ss.generateFromShaded(ZoneId.class, ZonedDateTime.class, Chronology.class);

        var generated = tg.generate(ss).getAll();
        assertThat(generated.size()).isAtLeast(ss.getTargetPackageAndClasses().size());
    }

    @Test
    public void testLegacyMode() {
        Options options = Options.builder()
                .useHasInsteadOfGet(true)
                .useGetterForLegacyClasses(true)
                .build();
        TruthGenerator tg = TestClassFactories.newTruthGenerator(options);

        SourceClassSets ss = TestClassFactories.newSourceClassSets(".legacy");
        ss.generateFromNonBean(NonBeanLegacy.class);

        //
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
    // todo use bootstrapped ResultSubject
    @Test
    public void recursiveGeneration() {
        TruthGenerator tg = TestClassFactories.newTruthGenerator();
        var allGeneratedSystems = tg.generate(MyEmployee.class).getAll();

        //
        assertThat(allGeneratedSystems).containsKey(MyEmployee.class);
        assertThat(allGeneratedSystems).containsKey(IdCard.class);
        assertThat(allGeneratedSystems).containsKey(MyEmployee.State.class);

        // lost in the generics
        assertThat(allGeneratedSystems).doesNotContainKey(Project.class);

        //
        assertThat(allGeneratedSystems).containsKey(UUID.class);
        assertThat(allGeneratedSystems).containsKey(ZonedDateTime.class);
        assertThat(allGeneratedSystems).containsKey(DayOfWeek.class);

        // recursive subjects that shouldn't be included
        assertThat(allGeneratedSystems).doesNotContainKey(Spliterator.class);
        assertThat(allGeneratedSystems).doesNotContainKey(Stream.class);
    }

    /**
     * Automatically shade subjects that are in packages of other modules (that would cause ao compile error)
     * <p>
     * NB: if this test fails, the project probably won't compile - as an invalid source class will have been produced.
     */
    @Test
    public void autoShade() {
        TruthGenerator tg = TestClassFactories.newTruthGenerator();
        SourceClassSets ss = TestClassFactories.newSourceClassSets();

        Class<UUID> clazz = UUID.class;
        tg.setEntryPoint(of(ss.getPackageForEntryPoint()));
        var generate = tg.generate(clazz).getAll();

        //
        assertThat(generate).containsKey(clazz);

        //
        ThreeSystem threeSystem = generate.get(clazz);
        JavaClassSource parent = threeSystem.getParent().getGenerated();
        assertThat(parent.getPackage()).startsWith(ss.getPackageForEntryPoint());
    }

    /**
     * Simple check for generating chains for `toSomething` methods
     */
    @Test
    public void toers() {
        TruthGenerator tg = TestClassFactories.newTruthGenerator();
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
        TruthGenerator tg = TestClassFactories.newTruthGenerator();
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
        TruthGenerator tg = TestClassFactories.newTruthGenerator(recursive.build());

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
        TruthGenerator tg = TestClassFactories.newTruthGenerator(recursive);
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
