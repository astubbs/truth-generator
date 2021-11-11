package io.stubbs.truth.generator;

import lombok.experimental.StandardException;

@StandardException
public class GeneratorException extends RuntimeException {
  public GeneratorException(final String s, final ClassNotFoundException e) {
    super(s, e);
  }
}
