package io.stubbs.truth.generator.testModel;

import lombok.Value;

import java.time.ZonedDateTime;

/**
 * @author Antony Stubbs
 */
@Value
public class Project {
    String desc;
    ZonedDateTime start;
}
