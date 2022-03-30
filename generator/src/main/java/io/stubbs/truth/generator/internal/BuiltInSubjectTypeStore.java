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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    @Getter(AccessLevel.PROTECTED)
    private static final HashSet<Class<?>> nativeTypes = new LinkedHashSet<>();

    @Getter(AccessLevel.PRIVATE)
    private static final HashSet<Class<?>> nativeTypesTruth8 = new LinkedHashSet<>();

    @Getter(AccessLevel.PRIVATE)
    private static final Map<String, Class<? extends Subject>> classPathSubjectTypes = new HashMap<>();

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
    private ClassUtils classUtils = new ClassUtils();

    public BuiltInSubjectTypeStore() {
        autoRegisterStandardSubjectExtension();
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

    // todo cleanup
    public boolean isTypeCoveredUnderStandardSubjects(final Class<?> returnType) {
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

    public Optional<Class<? extends Subject>> getSubjectForNotNativeType(String simpleName, Class<?> clazzUnderTest) {
        return getClosestTruthNativeSubjectForType(clazzUnderTest);
    }

    protected Optional<Class<? extends Subject>> getClosestTruthNativeSubjectForType(final Class<?> type) {
        // native
        Optional<Class<?>> highestPriorityNativeType = findHighestPriorityNativeType(type);
        if (highestPriorityNativeType.isPresent()) {
            Class<?> aClass = highestPriorityNativeType.get();
            Class<? extends Subject> compiledSubjectForTypeName = getCompiledSubjectForTypeName(aClass.getSimpleName());
            return ofNullable(compiledSubjectForTypeName);
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
