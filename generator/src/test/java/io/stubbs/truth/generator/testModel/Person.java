package io.stubbs.truth.generator.testModel;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * @author Antony Stubbs
 */
@Getter
@SuperBuilder(toBuilder = true)
@RequiredArgsConstructor
public class Person<T> {
    protected final String name;
    protected final long someLongAspect;
    protected final ZonedDateTime birthday;

    @Getter
    protected final Optional<T> typeParamTestOptional;
    protected final T typeParamTest;

    public int getBirthYear() {
        return birthday.getYear();
    }

}
