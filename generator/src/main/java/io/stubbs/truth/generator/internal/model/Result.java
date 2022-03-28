package io.stubbs.truth.generator.internal.model;

import lombok.Builder;

@Builder(toBuilder = true)
public class Result {
    Iterable<Class<?>> referencedBuilt;
    Iterable<Class<?>> referencedNotBuild;
}
