package io.stubbs.truth.generator.internal;

import com.google.common.truth.Fact;
import com.google.common.truth.Subject;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import lombok.Getter;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.reflections.ReflectionUtils;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.lang.reflect.Modifier.*;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.function.Predicate.not;
import static org.apache.commons.lang3.StringUtils.*;
import static org.reflections.ReflectionUtils.*;

/**
 * Generates assertion methods for adding to skeleton classes with various {@link AssertionMethodStrategy}(s).
 *
 * @author Antony Stubbs
 */
// todo needs refactoring into different strategies, interface https://github.com/astubbs/truth-generator/issues/12
public class SubjectMethodGenerator extends AssertionMethodStrategy {

    private final BooleanStrategy booleanStrategy;
    private final EqualityStrategy equalityStrategy = new EqualityStrategy();
    private final Set<AssertionMethodStrategy> strategies = new HashSet<>();
    private final ChainStrategy chainStrategy;
    private final JDKOverrideAnalyser bs = new JDKOverrideAnalyser(Options.get());
    private final GeneratedSubjectTypeStore generatedSubjectTypeStore;
    // todo as strategies are refactored, this should eventually be removed
    private ThreeSystem<?> context;

    /**
     * @param allTypes todo naming
     */
    public SubjectMethodGenerator(Set<ThreeSystem<?>> allTypes, BuiltInSubjectTypeStore subjectStore) {
        GeneratedSubjectTypeStore generatedSubjectTypeStore = new GeneratedSubjectTypeStore(allTypes, subjectStore);
        this.generatedSubjectTypeStore = generatedSubjectTypeStore;
        chainStrategy = new ChainStrategy(generatedSubjectTypeStore);
        this.booleanStrategy = new BooleanStrategy();
        strategies.add(new OptionalStrategy());
    }

    public void addTests(ThreeSystem<?> system) {
        this.context = system;

        //
        Class<?> classUnderTest = system.getClassUnderTest();
        JavaClassSource generated = system.getParent().getGenerated();

        //
        {
            Collection<Method> getters = getMethods(system)
                    // output a consistent ordering - alphabetical my method name
                    .stream().sorted((o1, o2) -> Comparator.comparing(Method::getName).compare(o1, o2))
                    .collect(Collectors.toList());
            for (Method method : getters) {
                addFieldAccessors(method, generated, classUnderTest);
            }
        }

        //
        {
            Collection<Method> toers = getMethods(classUnderTest, input -> {
                if (input == null) return false;
                String name = input.getName();
                // exclude lombok builder methods
                return startsWith(name, "to") && !endsWith(name, "Builder");
            });
            toers = removeOverridden(toers);
            for (Method method : toers) {
                chainStrategy.addStrategyMaybe(context, method, generated);
            }
        }
    }

    private <T extends Member> Predicate<T> withSuffix(String suffix) {
        return input -> input != null && input.getName().startsWith(suffix);
    }

    private Collection<Method> getMethods(ThreeSystem<?> system) {
        Class<?> classUnderTest = system.getClassUnderTest();
        boolean legacyMode = system.isLegacyMode();

        Set<Method> union = new HashSet<>();
        var getters = getMethods(classUnderTest, withPrefix("get"));
        var issers = getMethods(classUnderTest, withPrefix("is"));

        // also get all other methods, regardless of their prefix
        Predicate<Method> exceptSetters = not(withPrefix("set"));
        Predicate<Method> exceptToers = not(withPrefix("to"));
        var legacy = (legacyMode) ? getMethods(classUnderTest, exceptSetters, exceptToers) : Set.<Method>of();

        union.addAll(getters);
        union.addAll(issers);
        union.addAll(legacy);

        return removeForJdkTarget(system.classUnderTest, removeOverridden(union));
//    return removeOverridden(union);
    }

    private Collection<Method> removeForJdkTarget(Class<?> clazz, Collection<Method> methods) {
        if (!bs.isOverrideConfigured()) {
            return methods;
        }

        if (clazz.getPackageName().startsWith("java.")) {
            return methods.stream()
                    .filter(method ->
                            bs.doesOverrideClassContainMethod(clazz, method)
                    )
                    .collect(Collectors.toSet());
        } else {
            return methods;
        }
    }

    private Collection<Method> getMethods(Class<?> classUnderTest, Predicate<Method>... prefix) {
        // if shaded, can't access package private methods
        boolean isShaded = context.isShaded();
        Predicate<Method> skip = (ignore) -> true;
        Predicate<Method> shadedPredicate = (isShaded) ? withModifier(PUBLIC) : skip;

        List<Predicate<? super Method>> predicatesCollect = Arrays.stream(prefix).collect(Collectors.toList());
        predicatesCollect.add(shadedPredicate);
        predicatesCollect.add(not(withModifier(PRIVATE)));
        predicatesCollect.add(not(withModifier(PROTECTED)));
        predicatesCollect.add(withParametersCount(0));

        Predicate<? super Method>[] predicates = predicatesCollect.toArray(new Predicate[0]);
        return removeForJdkTarget(classUnderTest, ReflectionUtils.getAllMethods(classUnderTest, predicates));
//    return ReflectionUtils.getAllMethods(classUnderTest, predicates);
    }

    private Collection<Method> removeOverridden(final Collection<Method> getters) {
        Map<String, Method> result = new HashMap<>();
        for (Method getter : getters) {
            String sig = getSignature(getter);
            if (result.containsKey(sig)) {
                Method existing = result.get(sig);
                Class<?> existingDeclaringClass = existing.getDeclaringClass();
                Class<?> newDeclaringClass = getter.getDeclaringClass();

                if (existingDeclaringClass.isAssignableFrom(newDeclaringClass)) {
                    // replace
                    result.put(sig, getter);
                } else {
                    // skip
                }
            } else {
                result.put(sig, getter);
            }
        }
        return result.values();
    }

    private String getSignature(final Method getter) {
        return getter.getName() + Arrays.stream(getter.getParameterTypes()).map(Class::getName).collect(Collectors.toList());
    }

    private void addFieldAccessors(Method method, JavaClassSource generated, Class<?> classUnderTest) {
        Class<?> returnType = generatedSubjectTypeStore.getWrappedReturnType(method);

        // todo skip static methods for now - just need to make template a bit more advanced
        if (methodIsStatic(method))
            return;

        // takes priority over primitive or collection types
        chainStrategy.addStrategyMaybe(context, method, generated);

        //
        this.strategies.forEach(x -> x.addStrategyMaybe(context, method, generated));

        if (Boolean.class.isAssignableFrom(returnType)) {
            booleanStrategy.addStrategyMaybe(context, method, generated);
        } else {

            if (Collection.class.isAssignableFrom(returnType)) {
                addHasElementStrategy(method, generated, classUnderTest);
            }

            if (Map.class.isAssignableFrom(returnType)) {
                addMapStrategy(method, generated, classUnderTest);
            }

            equalityStrategy.addStrategyMaybe(context, method, generated);
        }

        generated.addImport(Fact.class)
                .setStatic(true)
                .setName(Fact.class.getCanonicalName() + ".*");
    }

    private void addMapStrategy(Method method, JavaClassSource generated, Class<?> classUnderTest) {
        addMapStrategyGeneric(method, generated, false);
        addMapStrategyGeneric(method, generated, true);
    }

    private MethodSource<JavaClassSource> addMapStrategyGeneric(Method method, JavaClassSource generated, boolean positive) {
        String testPrefix = positive ? "" : "!";

        String body = "" +
                "  if (%sactual.%s().containsKey(expected)) {\n" +
                "    failWithActual(fact(\"expected %s %sto have key\", expected));\n" +
                "  }\n";

        String say = positive ? "" : "NOT ";
        String fieldName = removeStart(method.getName(), "get");
        body = format(body, testPrefix, method.getName(), fieldName, say);

        String methodName = "has" + capitalize(fieldName) + capitalize(say.toLowerCase()).trim() + "WithKey";
        MethodSource<JavaClassSource> newMethod = generated.addMethod();
        newMethod
                .setName(methodName)
                .setReturnTypeVoid()
                .setBody(body)
                .setPublic();

        // parameter
        Class keyType = ClassUtils.getStrippedReturnTypeFirstGenericParam(method);
        String typeName = keyType.getCanonicalName();
        newMethod.addParameter(typeName, "expected");

        //
        newMethod.getJavaDoc().setText(String.format("Check Maps for containing a given {@link %s} key.", ClassUtils.maybeGetSimpleName(keyType)));

        copyThrownExceptions(method, newMethod);

        return newMethod;
    }

    private void addHasElementStrategy(Method method, JavaClassSource generated, Class<?> classUnderTest) {
        addHasElementStrategyGeneric(method, generated, false);
        addHasElementStrategyGeneric(method, generated, true);
    }

    private MethodSource<JavaClassSource> addHasElementStrategyGeneric(Method method, JavaClassSource generated, boolean positive) {
        String body = "" +
                "  if (%sactual.%s().contains(expected)) {\n" +
                "    failWithActual(fact(\"expected %s %sto have element\", expected));\n" +
                "  }\n";
        String testPrefix = positive ? "" : "!";

        String fieldName = removeStart(method.getName(), "get");

        String say = positive ? "" : "NOT ";
        body = format(body, testPrefix, method.getName(), fieldName, say);

        String methodName = "has" + capitalize(fieldName) + capitalize(say.toLowerCase()).trim() + "WithElement";
        MethodSource<JavaClassSource> newMethod = generated.addMethod();
        newMethod
                .setName(methodName)
                .setReturnTypeVoid()
                .setBody(body)
                .setPublic();

        // parameter
        Class elementType = ClassUtils.getStrippedReturnTypeFirstGenericParam(method);
        String canonicalName = elementType.getCanonicalName();
        newMethod.addParameter(canonicalName, "expected");

        newMethod.getJavaDoc().setText(String.format("Checks if a {@link %s} element is, or is not contained in the collection.", ClassUtils.maybeGetSimpleName(elementType)));

        copyThrownExceptions(method, newMethod);

        return newMethod;
    }

    public void addTests(final Set<ThreeSystem<?>> allTypes) {
        for (ThreeSystem system : allTypes) {
            addTests(system);
        }

        // only serialise results, when all have finished - useful for debugging
        for (ThreeSystem c : allTypes) {
            Utils.writeToDisk(c.parent.getGenerated());
        }
    }

    /**
     * Once all strategies have moved to dedicated classes, this class won't need to extend {@link
     * AssertionMethodStrategy}
     */
    @Override
    public boolean addStrategyMaybe(ThreeSystem threeSystem, Method method, JavaClassSource generated) {
        throw new IllegalStateException("no-op - transitional implementation");
    }

    /**
     * An `Either` type that represents a {@link Subject} for a type being either an existing Compiled class, or a new
     * Generated class.
     */
    public static class ClassOrGenerated {

        /**
         * an existing Subject class on the class path
         */
        @Getter
        final Class<?> clazz;

        /**
         * a new generated Subject
         */
        @Getter
        final ThreeSystem<?> generated;

        ClassOrGenerated(final Class<? extends Subject> clazz, final ThreeSystem<?> generated) {
            this.clazz = clazz;
            this.generated = generated;
        }

        static Optional<ClassOrGenerated> ofClass(Class<? extends Subject> compiledSubjectForTypeName) {
            return ofClass(of(compiledSubjectForTypeName));
        }

        static Optional<ClassOrGenerated> ofClass(Optional<Class<? extends Subject>> clazz) {
            //noinspection OptionalIsPresent java 8
            if (clazz.isPresent())
                return of(new ClassOrGenerated(clazz.get(), null));
            else return empty();
        }

        String getSubjectSimpleName() {
            if (clazz == null)
                return this.generated.middle.getSimpleName();
            else
                return clazz.getSimpleName();
        }

        String getSubjectQualifiedName() {
            if (clazz == null)
                return this.generated.middle.getCanonicalName();
            else
                return clazz.getCanonicalName();
        }

        @Override
        public String toString() {
            return "ClassOrGenerated{" +
                    "clazz=" + clazz +
                    ", generated=" + generated +
                    '}';
        }

        public String getFactoryContainerName() {
            return (isGenerated()) ? generated.middle.getCanonicalName() : clazz.getCanonicalName();
        }

        public boolean isGenerated() {
            return generated != null;
        }
    }
}
