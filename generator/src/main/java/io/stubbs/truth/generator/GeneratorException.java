package io.stubbs.truth.generator;

public class GeneratorException extends RuntimeException {
  public GeneratorException(final String s, final ClassNotFoundException e) {
    super(s, e);
  }
}
