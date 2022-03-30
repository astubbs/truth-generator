package io.stubbs.truth.generator.example;

import lombok.Value;

/**
 * @author Antony Stubbs
 */
@Value
public class Car {
    String name;
    Make make;
    int colourId;

    public enum Make {PLASTIC, METAL}
}
