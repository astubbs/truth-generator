package io.stubbs.truth.generator;

import com.google.common.truth.StringSubject;
import com.google.common.truth.Subject;
import io.stubbs.truth.generator.subjects.MyStringSubject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker for the system to find extensions to base Truth {@link Subject}s - for example, {@link MyStringSubject}
 * extends Truth's {@link StringSubject}.
 * <p>
 * These extensions get used in place of the base Subjects in the reference chain.
 *
 * @author Antony Stubbs
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface BaseSubjectExtension {
    Class<?> value();
}
