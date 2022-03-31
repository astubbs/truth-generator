package io.stubbs.truth.generator.internal;

import com.google.common.truth.Subject;
import com.google.common.truth.Truth;
import com.google.common.truth.Truth8;
import io.stubbs.truth.generator.BaseSubjectExtension;
import io.stubbs.truth.generator.GeneratorException;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.reflections.Reflections;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.*;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.ClassUtils.primitiveToWrapper;

/**
 * Management class for built-in or explicitly registered Subject lookup based on classes.
 * <p>
 * Built in, as opposed to generate. Maybe rename to static (vs dynamic / generated) instead.
 *
 * @author Antony Stubbs
 * @see GeneratedSubjectTypeStore
 */
public class BuiltInSubjectTypeStore {

    /**
     * In priority order - most specific first. Types that are native to {@link Truth} - i.e. you can call {@link
     * Truth#assertThat}(...) with it. Note that this does not include {@link Truth8} types.
     */
    @Getter(AccessLevel.PRIVATE)
    private static final HashSet<Class<?>> nativeTypes = new LinkedHashSet<>();

    @Getter(AccessLevel.PRIVATE)
    private static final HashSet<Class<?>> nativeTypesTruth8 = new LinkedHashSet<>();

    @Getter(AccessLevel.PRIVATE)
    private static final Map<String, Class<? extends Subject>> classPathSubjectTypes = new HashMap<>();

    /**
     * Higher priority first.
     */
    static Class<?>[] classes = {
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
            Object.class, // catch all - uses plain Subject.class
    };

    /**
     * {@link Path} is excluded, because Turth8's {@link com.google.common.truth.PathSubject} is empty, so our generated
     * PathSubject is superior.
     */
    static Class<?>[] classesFromTruth8 = {
            // Truth8
            // Path.class,
            OptionalDouble.class,
            OptionalInt.class,
            OptionalLong.class,
            Stream.class,
            IntStream.class,
            DoubleStream.class,
            LongStream.class,
    };

    static {
        nativeTypes.addAll(Arrays.stream(classes).collect(Collectors.toList()));

        //
        nativeTypesTruth8.addAll(Arrays.stream(classesFromTruth8).collect(Collectors.toList()));
    }

    static {
        initSubjectTypes();
    }

    /**
     * Base Truth subject extensions to inject into Subject tree
     *
     * @see io.stubbs.truth.generator.BaseSubjectExtension
     */
    @Getter(AccessLevel.PRIVATE)
    private final Map<Class<?>, Class<? extends Subject>> subjectExtensions = new HashMap<>();
    private final ClassUtils classUtils = new ClassUtils();

    public BuiltInSubjectTypeStore() {
        autoRegisterStandardSubjectExtension();
    }

    public static boolean isANativeType(Class<?> aClass) {
        return Stream.concat(
                        getNativeTypes().stream(),
                        getNativeTypesTruth8().stream())
                .anyMatch(aClass::equals);
    }

    protected void autoRegisterStandardSubjectExtension() {
        Set<Class<?>> nativeExtensions = classUtils.findNativeExtensions("io.stubbs");
        for (Class<?> nativeExtension : nativeExtensions) {
            BaseSubjectExtension[] annotationsByType = nativeExtension.getAnnotationsByType(BaseSubjectExtension.class);
            List<BaseSubjectExtension> list = Arrays.asList(annotationsByType);
            Validate.isTrue(list.size() == 1, "Class must be annotated exactly once - found: %s", list);
            BaseSubjectExtension baseSubjectExtension = list.get(0);
            Class<?> targetClass = baseSubjectExtension.value();
            if (Subject.class.isAssignableFrom(nativeExtension)) {
                //noinspection unchecked - checked above as assignable from
                Class<? extends Subject> nativeExtensionSubject = (Class<? extends Subject>) nativeExtension;
                registerStandardSubjectExtension(targetClass, nativeExtensionSubject);
            } else {
                throw new GeneratorException("Class that isn't a Subject incorrectly annotation with " + BaseSubjectExtension.class);
            }
        }
    }

    public void registerStandardSubjectExtension(Class<?> targetType, Class<? extends Subject> subjectExtensionClass) {
        subjectExtensions.put(targetType, subjectExtensionClass);
    }

    private static void initSubjectTypes() {
        Reflections reflections = new Reflections("io.stubbs.truth", "com.google.common.truth");
        Set<Class<? extends Subject>> subjectTypes = reflections.getSubTypesOf(Subject.class);
        Validate.isTrue(!subjectTypes.isEmpty(), "Unexpected: Could not find any compile time Subjects to work with.");
        subjectTypes.forEach(x -> classPathSubjectTypes.put(x.getSimpleName(), x));
    }

    public static boolean hasStaticAccessThroughTruthEntryPoint(final Class<?> returnType) {
        return getNativeTypes().contains(returnType);
    }

    /**
     * Should only do this, if we can't find a more specific subject for the returnType.
     */
    public boolean isTypeCoveredUnderStandardSubjects(final Class<?> returnType) {
        // todo should check if class is assignable from the super subjects, instead of checking names
        // todo use canonical names
        // todo add support truth8 extensions - optional etc

        final Class<?> normalised = (returnType.isArray())
                ? returnType.getComponentType()
                : returnType;

        boolean isCoveredByNonPrimitiveStandardSubjects = Stream.concat(
                        getNativeTypes().stream(),
                        getNativeTypesTruth8().stream()
                )
                .anyMatch(x ->
                        x.isAssignableFrom(normalised)
                );

        boolean isAnArray = returnType.isArray();

        return isCoveredByNonPrimitiveStandardSubjects || isAnArray;
    }

    public Optional<Class<? extends Subject>> getSubjectForNotNativeType(String simpleName, Class<?> clazzUnderTest) {
        return getClosestTruthNativeSubjectForType(clazzUnderTest);
    }

    protected Optional<Class<? extends Subject>> getClosestTruthNativeSubjectForType(final Class<?> type) {
        // native
        Optional<Class<?>> highestPriorityNativeType = findHighestPriorityNativeType(type);
        if (highestPriorityNativeType.isPresent()) {
            if (highestPriorityNativeType.get().equals(Object.class)) {
                return Optional.of(Subject.class);
            } else {
                Class<?> aClass = highestPriorityNativeType.get();
                Class<? extends Subject> compiledSubjectForTypeName = getCompiledSubjectForTypeName(aClass.getSimpleName());
                return ofNullable(compiledSubjectForTypeName);
            }
        }
        return empty();
    }

    public Optional<Class<?>> findHighestPriorityNativeType(Class<?> type) {
        Class<?> normalised = primitiveToWrapper(type);
        return getNativeTypes().stream().filter(x -> x.isAssignableFrom(normalised)).findFirst();
    }

    protected Class<? extends Subject> getCompiledSubjectForTypeName(String name) {
        // remove package if exists
        if (name.contains("."))
            name = StringUtils.substringAfterLast(name, ".");

        String compoundName = name + "Subject";
        return getClassPathSubjectTypes(compoundName);
    }

    public Class<? extends Subject> getClassPathSubjectTypes(String compoundName) {
        return getClassPathSubjectTypes().get(compoundName);
    }

    public boolean isAnExtendedSubject(Class<?> subjectClass) {
        return getSubjectExtensions().containsValue(subjectClass);
    }

    public Class<? extends Subject> getSubjectExtensions(Class<?> type) {
        return getSubjectExtensions().get(type);
    }
}
