package io.stubbs.truth.generator.testModel;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.ZonedDateTime;

/**
 * @author Antony Stubbs
 */
@Getter
@SuperBuilder(toBuilder = true)
@RequiredArgsConstructor
public class Person {
    protected final String name;
    protected final long someLongAspect;
    protected final ZonedDateTime birthday;

    public int getBirthYear() {
        return birthday.getYear();
    }

}
