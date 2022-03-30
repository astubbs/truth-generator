package io.stubbs.truth.generator.internal;

import com.google.common.flogger.FluentLogger;
import com.google.common.truth.ObjectArraySubject;
import com.google.common.truth.Subject;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import lombok.experimental.Delegate;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.logging.Level.INFO;
import static org.apache.commons.lang3.ClassUtils.primitiveToWrapper;
import static org.apache.commons.lang3.StringUtils.capitalize;

/**
 * Management class for generated as well as standard Subject lookup based on classes.
 *
 * @author Antony Stubbs
 * @see BuiltInSubjectTypeStore
 */
public class GeneratedSubjectTypeStore {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    private final Map<String, ThreeSystem<?>> generatedSubjects;

    @Delegate
    private final BuiltInSubjectTypeStore builtInSubjectTypeStore;

    /**
     * Useful for testing - a store with no generated skeleton types
     */
    protected GeneratedSubjectTypeStore() {
        this(Set.of(), new BuiltInSubjectTypeStore());
    }

    public GeneratedSubjectTypeStore(Set<? extends ThreeSystem<?>> allTypes, BuiltInSubjectTypeStore builtInSubjectTypeStore) {
        this.generatedSubjects = allTypes.stream().collect(Collectors.toMap(x -> x.classUnderTest.getName(), x -> x));
        this.builtInSubjectTypeStore = builtInSubjectTypeStore;
    }

    protected Optional<SubjectMethodGenerator.ClassOrGenerated> getSubjectForType(final Class<?> type) {
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
                var compiledSubjectForTypeName = builtInSubjectTypeStore.getCompiledSubjectForTypeName(subjectPrefix);
                return SubjectMethodGenerator.ClassOrGenerated.ofClass(compiledSubjectForTypeName);
            } else {
                return SubjectMethodGenerator.ClassOrGenerated.ofClass(ObjectArraySubject.class);
            }
        }

        return getSubjectForNotNativeType(name, type);
    }

    /**
     * @param nameOfType todo redundant? and smelly
     */
    protected Optional<SubjectMethodGenerator.ClassOrGenerated> getSubjectForNotNativeType(String nameOfType, Class<?> type) {
        // explicit extensions take priority
        Class<? extends Subject> extension = this.builtInSubjectTypeStore.getSubjectExtensions(type);
        if (extension != null)
            return SubjectMethodGenerator.ClassOrGenerated.ofClass(extension);

        //
        Optional<SubjectMethodGenerator.ClassOrGenerated> subject = getGeneratedOrCompiledSubjectFromString(nameOfType);

        // Can't find any generated ones or compiled ones - fall back to native subjects that are assignable (e.g. comparable or iterable)
        if (subject.isEmpty()) {
            Optional<Class<? extends Subject>> nativeSubjectForType = builtInSubjectTypeStore.getClosestTruthNativeSubjectForType(type);
            subject = SubjectMethodGenerator.ClassOrGenerated.ofClass(nativeSubjectForType);
            subject.ifPresent(classOrGenerated -> logger.at(INFO).log("Falling back to native interface subject %s for type %s", classOrGenerated.clazz, type));
        }

        return subject;
    }

    private Optional<SubjectMethodGenerator.ClassOrGenerated> getGeneratedOrCompiledSubjectFromString(
            final String name) {
        boolean isObjectArray = name.endsWith("[]");
        if (isObjectArray)
            return of(new SubjectMethodGenerator.ClassOrGenerated(ObjectArraySubject.class, null));

        Optional<ThreeSystem<?>> subjectFromGenerated = getSubjectFromGenerated(name);// takes precedence
        if (subjectFromGenerated.isPresent()) {
            return of(new SubjectMethodGenerator.ClassOrGenerated(null, subjectFromGenerated.get()));
        }

        // any matching compiled subject
        var aClass = builtInSubjectTypeStore.getCompiledSubjectForTypeName(name);
        if (aClass != null)
            return of(new SubjectMethodGenerator.ClassOrGenerated(aClass, null));

        return empty();
    }

    private Optional<ThreeSystem<?>> getSubjectFromGenerated(final String name) {
        return Optional.ofNullable(this.generatedSubjects.get(name));
    }

    public boolean isAnExtendedSubject(Class<?> clazz) {
        return builtInSubjectTypeStore.isAnExtendedSubject(clazz);
    }
}
