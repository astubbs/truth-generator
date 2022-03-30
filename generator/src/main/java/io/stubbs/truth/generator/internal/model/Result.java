package io.stubbs.truth.generator.internal.model;

import lombok.Builder;

/**
 * Final collated results of the generator. Used to present structured results to user.
 * <p>
 * todo work in progress
 *
 * @author Antony Stubbs
 */
@Builder(toBuilder = true)
public class Result {
    Iterable<Class<?>> referencedBuilt;
    Iterable<Class<?>> referencedNotBuild;
}
