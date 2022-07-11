package io.stubbs.truth.generator;

import com.google.common.truth.Subject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks for the machines, the method used to create the {@link Subject}s.
 * <p>
 * Useful so that we don't need to rely completely on String patterns, and so that Subject extension points can have non
 * colliding names - as the method must be static.
 *
 * @author Antony Stubbs
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface SubjectFactoryMethod {
}
