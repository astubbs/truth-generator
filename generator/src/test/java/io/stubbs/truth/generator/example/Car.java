package io.stubbs.truth.generator.example;

import lombok.Data;
import lombok.Value;

@Data
public class Car {
    String name;
    Make make;
    int colourId;

    public enum Make {PLASTIC, METAL}
}
