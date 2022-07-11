package io.stubbs.truth.generator.internal;

/**
 * @author Antony Stubbs
 */
public class TruthGeneratorRuntimeException extends RuntimeException {
    public TruthGeneratorRuntimeException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public TruthGeneratorRuntimeException(String msg) {
        super(msg);
    }
}
