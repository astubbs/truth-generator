package io.stubbs.truth.generator.internal;

import com.google.common.flogger.FluentLogger;
import com.google.common.truth.*;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jboss.forge.roaster.model.source.Import;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.reflections.Reflections;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.stubbs.truth.generator.internal.Utils.msg;
import static java.lang.String.format;
import static java.util.Optional.*;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import static org.apache.commons.lang3.ClassUtils.primitiveToWrapper;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.removeStart;

@Slf4j
public class ChainStrategy extends MethodStrategy {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    /**
     * In priority order - most specific first. Types that are native to {@link Truth} - i.e. you can call {@link
     * Truth#assertThat}(...) with it. Note that this does not include {@link Truth8} types.
     */
    @Getter
    // todo used here, but not cohesive to this class
    private static final HashSet<Class<?>> nativeTypes = new LinkedHashSet<>();

    @Getter
    private static final HashSet<Class<?>> nativeTypesTruth8 = new LinkedHashSet<>();

    static {
        // higher priority first
        Class<?>[] classes = {
                Map.class,
                Iterable.class,
                List.class,
                Set.class,
                Throwable.class,
                BigDecimal.class,
                String.class,
                Double.class,
                Long.class,
                Integer.class,
                Short.class,
                Number.class,
                Boolean.class,
                Comparable.class,
                Class.class, // Enum#getDeclaringClass
        };
        nativeTypes.addAll(Arrays.stream(classes).collect(Collectors.toList()));

        //
        nativeTypesTruth8.add(Optional.class);
        nativeTypesTruth8.add(Stream.class);
    }

    private final Map<String, ThreeSystem> generatedSubjects;

    private final Map<Class<?>, Class<? extends Subject>> subjectExtensions;

    private final Map<String, Class<?>> classPathSubjectTypes = new HashMap<>();

    private final Options options = Options.get();

    public ChainStrategy(Set<ThreeSystem> allTypes, Map<Class<?>, Class<? extends Subject>> subjectExtensions) {
        this.generatedSubjects = allTypes.stream().collect(Collectors.toMap(x -> x.classUnderTest.getName(), x -> x));
        this.subjectExtensions = subjectExtensions;

        initSubjectTypes();
    }

    private void initSubjectTypes() {
        Reflections reflections = new Reflections("io.stubbs.truth", "com.google.common.truth");
        Set<Class<? extends Subject>> subjectTypes = reflections.getSubTypesOf(Subject.class);
        Validate.isTrue(!subjectTypes.isEmpty(), "Unexpected: Could not find any compile time Subjects to work with.");
        subjectTypes.forEach(x -> classPathSubjectTypes.put(x.getSimpleName(), x));
    }

    @Override
    protected boolean addStrategyMaybe(ThreeSystem threeSystem, Method method, JavaClassSource generated) {
        addChainStrategy(threeSystem, method, generated, getWrappedReturnType(method));
        return true;
    }

    protected MethodSource<JavaClassSource> addChainStrategy(ThreeSystem threeSystem, Method method, JavaClassSource generated, Class<?> returnType) {
        Optional<SubjectMethodGenerator.ClassOrGenerated> subjectForType;
        boolean optionalUnwrap = false;

        if (returnType.isAssignableFrom(Optional.class) && returnType != Object.class) {
            // unwrap
            Type genericReturnType1 = method.getGenericReturnType();
            if (ParameterizedType.class.isAssignableFrom(genericReturnType1.getClass())) {
                Type[] actualTypeArguments = ((ParameterizedType) genericReturnType1).getActualTypeArguments();
                if (actualTypeArguments.length == 1) {
                    Type optionalReturnType = actualTypeArguments[0];
                    if (optionalReturnType.getClass().isAssignableFrom(Class.class)) {
                        returnType = (Class<?>) optionalReturnType;
                        subjectForType = getSubjectForType(returnType);
                        optionalUnwrap = true;
                    } else {
                        throw new IllegalStateException("Can't cast optionally wrapped type to class");
                    }
                } else {
                    throw new IllegalStateException("Wrong number of type arguments for Optional - should be only one");
                }
            } else {
                log.warn("Optional type without type parameter, can't add optional chain.");
                return null;
            }
        } else {
            subjectForType = getSubjectForType(returnType);
        }

        boolean isCoveredByNonPrimitiveStandardSubjects = isTypeCoveredUnderStandardSubjects(returnType);

        // no subject to chain
        if (subjectForType.isEmpty()) {
            logger.at(WARNING).log("Cant find subject for " + returnType);
            return null;
        }

        SubjectMethodGenerator.ClassOrGenerated subjectClass = subjectForType.get();

        // check if should skip as there are more specific versions that would clash
        if (subjectClass.getSubjectSimpleName().

                equals(BooleanSubject.class.getSimpleName())) {
            return null;
        }

        String nameForChainMethod = createNameForChainMethod(threeSystem, method);
        MethodSource<JavaClassSource> has = generated.addMethod()
                .setName(nameForChainMethod)
                .setPublic();

        StringBuilder body = new StringBuilder("isNotNull();\n");

        if (optionalUnwrap) {
            String name = capitalize(removeStart(method.getName(), "get"));
            body.append("has" + name + "Present();\n");
        }

        String check = format("return check(\"%s()", method.getName());
        body.append(check);

        if (optionalUnwrap) {
            body.append(".get()");
        }

        body.append("\")");

        //
        boolean isAnExtendedSubject = this.subjectExtensions.containsValue(subjectClass.clazz);
        boolean notPrimitive = !returnType.isPrimitive();
        boolean needsAboutCall = notPrimitive && !isCoveredByNonPrimitiveStandardSubjects || isAnExtendedSubject;

        if (needsAboutCall || subjectClass.isGenerated()) {
            String aboutName;
            aboutName = Utils.getFactoryName(returnType); // take a guess
            body.append(format(".about(%s())", aboutName));

            // import
            String factoryContainer = subjectClass.getFactoryContainerName();
            Import anImport = generated.addImport(factoryContainer);
            String name = factoryContainer + "." + aboutName;
            anImport.setName(name) // todo better way to do static method import?
                    .setStatic(true);
        }

        /* Also add cast for return type for older JVMs that don't seem to be able to correctly check for the
         * generic return type from Optional.get, otherwise we seem to get a compilation error on older JVMs.
         */
        String maybeCast = optionalUnwrap ? msg("({})",returnType.getSimpleName()) : "";

        if (methodIsStatic(method)) {
            body.append(format(".that(%s%s.%s()",
                    maybeCast,
                    method.getDeclaringClass().getSimpleName(),
                    method.getName()));
        } else {
            body.append(format(".that(%sactual.%s()",
                    maybeCast,
                    method.getName()));
        }

        if (optionalUnwrap) {
            body.append(".get()");
        }

        body.append(");\n");

        //
        has.setBody(body.toString());

        has.setReturnType(subjectClass.getSubjectSimpleName());

        generated.addImport(subjectClass.getSubjectQualifiedName());

        has.getJavaDoc().

                setText("Returns the Subject for the given field type, so you can chain on other assertions.");

        copyThrownExceptions(method, has);

        return has;
    }

    protected Class<?> getCompiledSubjectForTypeName(String name) {
        // remove package if exists
        if (name.contains("."))
            name = StringUtils.substringAfterLast(name, ".");

        String compoundName = name + "Subject";
        Class<?> aClass = this.classPathSubjectTypes.get(compoundName);
        return aClass;
    }

    private Optional<SubjectMethodGenerator.ClassOrGenerated> getSubjectForType(final Class<?> type) {
        String name;

        // if primitive, wrap and get wrapped Subject
        if (type.isPrimitive()) {
            Class<?> wrapped = primitiveToWrapper(type);
            name = wrapped.getName();
        } else {
            name = type.getName();
        }

        // arrays
        if (type.isArray()) {
            Class<?> componentType = type.getComponentType();
            if (componentType.isPrimitive()) {
                // PrimitiveBooleanArraySubject
                String simpleName = componentType.getSimpleName();
                simpleName = capitalize(simpleName);
                String subjectPrefix = "Primitive" + simpleName + "Array";
                Class<?> compiledSubjectForTypeName = getCompiledSubjectForTypeName(subjectPrefix);
                return SubjectMethodGenerator.ClassOrGenerated.ofClass(compiledSubjectForTypeName);
            } else {
                return SubjectMethodGenerator.ClassOrGenerated.ofClass(ObjectArraySubject.class);
            }
        }

        // explicit extensions take priority
        Class<? extends Subject> extension = this.subjectExtensions.get(type);
        if (extension != null)
            return SubjectMethodGenerator.ClassOrGenerated.ofClass(extension);

        //
        Optional<SubjectMethodGenerator.ClassOrGenerated> subject = getGeneratedOrCompiledSubjectFromString(name);

        // Can't find any generated ones or compiled ones - fall back to native subjects that are assignable (e.g. comparable or iterable)
        if (subject.isEmpty()) {
            Optional<Class<?>> nativeSubjectForType = getClosestTruthNativeSubjectForType(type);
            subject = SubjectMethodGenerator.ClassOrGenerated.ofClass(nativeSubjectForType);
            if (subject.isPresent())
                logger.at(INFO).log("Falling back to native interface subject %s for type %s", subject.get().clazz, type);
        }

        return subject;
    }

    // todo cleanup
    private boolean isTypeCoveredUnderStandardSubjects(final Class<?> returnType) {
        // todo should only do this, if we can't find a more specific subject for the returnType
        // todo should check if class is assignable from the super subjects, instead of checking names
        // todo use qualified names
        // todo add support truth8 extensions - optional etc
        // todo try generating classes for DateTime packages, like Instant and Duration
        // todo this is of course too aggressive

//    boolean isCoveredByNonPrimitiveStandardSubjects = specials.contains(returnType.getSimpleName());
        final Class<?> normalised = (returnType.isArray())
                ? returnType.getComponentType()
                : returnType;

        List<Class<?>> assignable = nativeTypes.stream().filter(x ->
                x.isAssignableFrom(normalised)
        ).collect(Collectors.toList());
        boolean isCoveredByNonPrimitiveStandardSubjects = !assignable.isEmpty();

        // todo is it an array of objects?
        // TODO delete dead code
        boolean array = returnType.isArray();
        Class<?>[] classes = returnType.getClasses();
        String typeName = returnType.getTypeName();
        Class<?> componentType = returnType.getComponentType();

        return isCoveredByNonPrimitiveStandardSubjects || array;
    }

    /**
     * Attempt to swap get for has, but only if it starts with get - otherwise leave it alone.
     */
    private String createNameForChainMethod(ThreeSystem threeSystem, Method method) {
        String name = method.getName();

        if (threeSystem.isLegacyMode()) {
            if (options.useGetterForLegacyClasses) {
                if (options.useHasInsteadOfGet) {
                    return "has" + capitalize(name);
                } else {
                    return "get" + capitalize(name);
                }
            } else {
                return name;
            }
        }

        if (name.startsWith("get")) {
            if (options.isUseHasInsteadOfGet()) {
                name = removeStart(name, "get");
                return "has" + name;
            } else {
                return name;
            }
        } else if (name.startsWith("is")) {
            return "has" + removeStart(name, "is");
        } else if (name.startsWith("to")) {
            return "has" + capitalize(name);
        } else
            return name;
    }


    private Optional<SubjectMethodGenerator.ClassOrGenerated> getGeneratedOrCompiledSubjectFromString(
            final String name) {
        boolean isObjectArray = name.endsWith("[]");
        if (isObjectArray)
            return of(new SubjectMethodGenerator.ClassOrGenerated(ObjectArraySubject.class, null));

        Optional<ThreeSystem> subjectFromGenerated = getSubjectFromGenerated(name);// takes precedence
        if (subjectFromGenerated.isPresent()) {
            return of(new SubjectMethodGenerator.ClassOrGenerated(null, subjectFromGenerated.get()));
        }

        // any matching compiled subject
        Class<?> aClass = getCompiledSubjectForTypeName(name);
        if (aClass != null)
            return of(new SubjectMethodGenerator.ClassOrGenerated(aClass, null));

        return empty();
    }

    private Optional<ThreeSystem> getSubjectFromGenerated(final String name) {
        return Optional.ofNullable(this.generatedSubjects.get(name));
    }


    private Optional<Class<?>> getClosestTruthNativeSubjectForType(final Class<?> type) {
        Class<?> normalised = primitiveToWrapper(type);

        // native
        Optional<Class<?>> highestPriorityNativeType = nativeTypes.stream().filter(x -> x.isAssignableFrom(normalised)).findFirst();
        if (highestPriorityNativeType.isPresent()) {
            Class<?> aClass = highestPriorityNativeType.get();
            Class<?> compiledSubjectForTypeName = getCompiledSubjectForTypeName(aClass.getSimpleName());
            return ofNullable(compiledSubjectForTypeName);
        }
        return empty();
    }

}
