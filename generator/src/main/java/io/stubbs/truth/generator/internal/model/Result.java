package io.stubbs.truth.generator.internal.model;

import io.stubbs.truth.generator.internal.OverallEntryPoint;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * Final collated results of the generator. Used to present structured results to user.
 * <p>
 * TODO this is still a work in progress
 *
 * @author Antony Stubbs
 */
@Value
@Builder(toBuilder = true)
public class Result {
    Iterable<Class<?>> referencedBuilt;
    Iterable<Class<?>> referencedNotBuild;
    Map<Class<?>, ThreeSystem<?>> all;
    OverallEntryPoint overallEntryPoint;
}
