package io.stubbs.truth.generator.testModel;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
@Value
public class IdCard {
    UUID id = UUID.randomUUID();
    String name;
    int epoch;

    SecurityType primarySecurityType;
    List<SecurityType> securityTypes;

    @Value
    public static class SecurityType {

        Type type;

        public Type getType() {
            return this.type;
        }

        public enum Type {
            FOB, MAGNETIC
        }

    }
}
