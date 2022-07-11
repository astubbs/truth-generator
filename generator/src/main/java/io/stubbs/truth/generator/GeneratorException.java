package io.stubbs.truth.generator;

/**
 * @author Antony Stubbs
 */
public class GeneratorException extends RuntimeException {
    public GeneratorException(final String s, final ClassNotFoundException e) {
        super(s, e);
    }

    public GeneratorException(String msg) {
        super(msg);
    }
}
