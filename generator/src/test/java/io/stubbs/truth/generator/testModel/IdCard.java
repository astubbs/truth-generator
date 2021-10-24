package io.stubbs.truth.generator.testModel;

import lombok.Value;

import java.util.UUID;

@Value
public class IdCard {
    UUID id = UUID.randomUUID();
    String name;
    int epoch;
}
