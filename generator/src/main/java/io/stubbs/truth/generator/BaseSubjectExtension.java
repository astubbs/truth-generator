package io.stubbs.truth.generator;

import com.google.common.truth.Subject;
import io.stubbs.truth.generator.internal.MyStringSubject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks for the machines, extensions to base Truth {@link Subject}s - for example, {@link MyStringSubject}.
 *
 * @author Antony Stubbs
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface BaseSubjectExtension {
    Class<?> value();
}
